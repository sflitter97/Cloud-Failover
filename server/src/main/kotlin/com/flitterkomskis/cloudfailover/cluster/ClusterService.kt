package com.flitterkomskis.cloudfailover.cluster

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceDeletedException
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceHandle
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceState
import com.flitterkomskis.cloudfailover.cloudproviders.ServiceProvider
import com.flitterkomskis.cloudfailover.reverseproxy.DynamicRoutingService
import java.lang.IllegalArgumentException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

/**
 * Provides a generic interface for the service layer below the [ClusterController].
 */
interface ClusterService {
    /**
     * Finds and returns all [Cluster]s.
     * @return A [List] of the [Cluster]s.
     */
    fun listClusters(): List<Cluster>
    /**
     * Finds and returns a specific [Cluster] given its id.
     * @param id ID that uniquely identifies the [Cluster].
     * @return The [Cluster] with the given id.
     */
    fun getCluster(id: UUID): Cluster
    /**
     * Creates a new [Cluster].
     * @param payload The state with which to initialize the [Cluster].
     * @return The newly created [Cluster].
     */
    fun createCluster(payload: Map<String, Any>): Cluster
    /**
     * Updated the existing [Cluster] with the given id using the information in payload.
     * @param id ID that uniquely identifies the cluster.
     * @param payload The information with which to update the [Cluster].
     * @return The updated [Cluster].
     */
    fun updateCluster(id: UUID, payload: Map<String, Any>): Cluster
    /**
     * Adds an instance to the [Cluster] with the given id.
     * @param id ID that uniquely identifies the [Cluster].
     * @param handle The instance to add to the [Cluster].
     * @return The [Cluster] after the instance has been added.
     */
    fun addInstance(id: UUID, handle: InstanceHandle): Cluster
    /**
     * Removes an instance from the [Cluster] with the given id.
     * @param id ID that uniquely identifies the [Cluster].
     * @param handle The instance to add to the [Cluster].
     * @return The [Cluster] after the instance has been removed.
     */
    fun removeInstance(id: UUID, handle: InstanceHandle): Cluster
    /**
     * Changes the access instance of a [Cluster] with the given id.
     * @param id ID that uniquely identifies the [Cluster].
     * @param handle The instance to become the new access instance.
     * @return The [Cluster] after the access instance has been set.
     */
    fun setAccessInstance(id: UUID, handle: InstanceHandle): Cluster
    /**
     * Deletes a [Cluster] from the [ClusterRepository].
     * @param id ID that uniquely identifies the [Cluster].
     * @return True if the cluster was deleted successfully of false otherwise.
     */
    fun deleteCluster(id: UUID): Boolean

    fun getUsedInstances(): Set<InstanceHandle>

    fun editInstance(id: UUID, handle: InstanceHandle, payload: Map<String, Any>): ClusterMembership

    fun addResponseTime(id: UUID, time: Long)

    fun flagRequest(id: UUID)
}

/**
 * Implementation of the [ClusterService] which saves [Cluster]s in the [ClusterRepository].
 */
@Service
class ClusterServiceImpl : ClusterService {
    private val logger: Logger = LoggerFactory.getLogger(ClusterServiceImpl::class.java)
    @Autowired private lateinit var clusterRepository: ClusterRepository
    @Autowired private lateinit var routingService: DynamicRoutingService
    @Autowired private lateinit var serviceProvider: ServiceProvider
    private val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)
    private val ALPHA: Double = 1.0 / 8.0
    private val BETA: Double = 1.0 / 4.0
    private val addResponseTimeLocks: ConcurrentHashMap<UUID, Mutex> = ConcurrentHashMap()
    private val clusterResponseTimeInfos: ConcurrentHashMap<UUID, ClusterResponseTimeInfo> = ConcurrentHashMap()
    var minRequestCount = 10
    var addResponseTimeInterval = 2000L
    var transitionInterval = 300000L

    @EventListener
    fun initialize(event: ContextRefreshedEvent) {
        val clusters = listClusters()
        clusters.forEach {
            addResponseTimeLocks[it.id] = Mutex()
            clusterResponseTimeInfos[it.id] = ClusterResponseTimeInfo()
        }
    }

    /**
     * Finds and returns all [Cluster]s in the Mongo Database.
     * @return A [List] of the [Cluster]s in the [ClusterRepository].
     */
    override fun listClusters(): List<Cluster> {
        return clusterRepository.findAll().map {
            removeDeletedInstances(it)
        }
    }

    /**
     * Finds and returns a specific [Cluster] in the Mongo Database given its id.
     * @param id ID that uniquely identifies the [Cluster].
     * @return The [Cluster] with the given id.
     */
    override fun getCluster(id: UUID): Cluster {
        return removeDeletedInstances(clusterRepository.findById(id).orElseThrow())
    }

    /**
     * Helper function that modifies the state of a [Cluster] based on the information in payload. Returns the [Cluster]
     * with its state updated.
     * @param cluster The [Cluster] to modify.
     * @param payload The state information to replace in the cluster.
     * @return The updated [Cluster]
     */
    private fun updateClusterFromPayload(cluster: Cluster, payload: Map<String, Any>): Cluster {
        payload.forEach { (key: String, value: Any) ->
            if (key == "name" && value is String) {
                logger.info("Setting $key to $value")
                cluster.name = value
            } else if (key == "instances" && value is List<*>) {
                val mapper = jacksonObjectMapper()
                // Cast List<*> to List<Map>
                val strList = value.filterIsInstance<Map<String, Any>>().takeIf { it.size == value.size }
                    ?: throw IllegalArgumentException("All elements must be maps.")

                logger.info("Setting $key to $value")
                val instanceList = strList.map { handle -> mapper.convertValue(handle, InstanceHandle::class.java) }
                instanceList.forEach {
                    if (!cluster.hasInstance(it)) {
                        cluster.addInstance(it)
                    }
                }
                cluster.instances.forEach {
                    if (instanceList.none { it2 -> it.handle == it2 }) {
                        cluster.removeInstance(it.handle)
                    }
                }
            } else if (key == "targetPort" && value is String) {
                logger.info("Setting $key to $value")
                cluster.targetPort = value.toInt()
            } else if (key == "targetPath" && value is String) {
                logger.info("Setting $key to $value")
                cluster.targetPath = value
            } else if (key == "accessInstance" && value is Map<*, *>) {
                val mapper = jacksonObjectMapper()
                val handle = mapper.convertValue(value, InstanceHandle::class.java)
                logger.info("Setting $key to $value")
                cluster.accessInstance = handle
            } else if (key == "backupInstance" && value is Map<*, *>) {
                val mapper = jacksonObjectMapper()
                val handle = mapper.convertValue(value, InstanceHandle::class.java)
                logger.info("Setting $key to $value")
                cluster.backupInstance = handle
            } else if (key == "enableInstanceStateManagement" && value is String) {
                logger.info("Setting $key to $value")
                cluster.enableInstanceStateManagement = value.toBoolean()
            } else if (key == "enableHotBackup" && value is String) {
                logger.info("Setting $key to $value")
                cluster.enableHotBackup = value.toBoolean()
            } else if (key == "enableAutomaticPriorityAdjustment" && value is String) {
                logger.info("Setting $key to $value")
                cluster.enableAutomaticPriorityAdjustment = value.toBoolean()
            }
        }
        return cluster
    }

    /**
     * Creates a new [Cluster] and saves it to the [ClusterRepository].
     * @param payload The state with which to initialize the [Cluster].
     * @return The newly created [Cluster].
     */
    override fun createCluster(payload: Map<String, Any>): Cluster {
        var cluster = Cluster()
        cluster = updateClusterFromPayload(cluster, payload)
        clusterRepository.save(cluster)
        addResponseTimeLocks[cluster.id] = Mutex()
        clusterResponseTimeInfos[cluster.id] = ClusterResponseTimeInfo()
        val getTopTwo = !payload.containsKey("accessInstance") && !payload.containsKey("backupInstance")
        manageInstanceStates(cluster, getTopTwo)
        return cluster
    }

    /**
     * Updates the existing [Cluster] with the given id using the information in payload and saves it in the
     * [ClusterRepository].
     * @param id ID that uniquely identifies the cluster.
     * @param payload The information with which to update the [Cluster].
     * @return The updated [Cluster].
     */
    override fun updateCluster(id: UUID, payload: Map<String, Any>): Cluster {
        return runBlocking {
            addResponseTimeLocks[id]?.withLock {
                var cluster = getCluster(id)
                cluster = updateClusterFromPayload(cluster, payload)
                clusterRepository.save(cluster)
                if (payload.containsKey("targetPort") || payload.containsKey("targetPath")) {
                    routingService.updateDynamicRoute(cluster)
                }
                val getTopTwo = !payload.containsKey("accessInstance") && !payload.containsKey("backupInstance")
                manageInstanceStates(cluster, getTopTwo)
                cluster
            } ?: throw ClusterServiceException("ID $id not found in addResponseTimeLocks")
        }
    }

    /**
     * Adds an instance to the [Cluster] with the given id.
     * @param id ID that uniquely identifies the [Cluster].
     * @param handle The instance to add to the [Cluster].
     * @return The [Cluster] after the instance has been added.
     */
    override fun addInstance(id: UUID, handle: InstanceHandle): Cluster {
        return runBlocking {
            addResponseTimeLocks[id]?.withLock {
                val cluster = getCluster(id)
                if (getUsedInstances().contains(handle)) {
                    logger.warn("Cannot add $handle to cluster $id, it is already used by another cluster.")
                    return@runBlocking cluster
                }
                cluster.addInstance(handle)
                logger.info("Added $handle to cluster $id")
                clusterRepository.save(cluster)
                manageInstanceStates(cluster)
                cluster
            } ?: throw ClusterServiceException("ID $id not found in addResponseTimeLocks")
        }
    }

    /**
     * Removes an instance from the [Cluster] with the given id.
     * @param id ID that uniquely identifies the [Cluster].
     * @param handle The instance to add to the [Cluster].
     * @return The [Cluster] after the instance has been removed.
     */
    override fun removeInstance(id: UUID, handle: InstanceHandle): Cluster {
        return runBlocking {
            addResponseTimeLocks[id]?.withLock {
                val cluster = getCluster(id)
                cluster.removeInstance(handle)
                logger.info("Removed $handle from cluster $id")
                clusterRepository.save(cluster)
                manageInstanceStates(cluster)
                cluster
            } ?: throw ClusterServiceException("ID $id not found in addResponseTimeLocks")
        }
    }

    /**
     * Changes the access instance of a [Cluster] with the given id.
     * @param id ID that uniquely identifies the [Cluster].
     * @param handle The instance to become the new access instance.
     * @return The [Cluster] after the access instance has been set.
     */
    override fun setAccessInstance(id: UUID, handle: InstanceHandle): Cluster {
        return runBlocking {
            addResponseTimeLocks[id]?.withLock {
                val cluster = getCluster(id)
                cluster.accessInstance = handle
                logger.info("Set $handle as access instance for cluster $id")
                clusterRepository.save(cluster)
                cluster
            } ?: throw ClusterServiceException("ID $id not found in addResponseTimeLocks")
        }
    }

    /**
     * Deletes a [Cluster] from the [ClusterRepository] and removes its route from the [DynamicRoutingService].
     * @param id ID that uniquely identifies the [Cluster].
     * @return True if the cluster was deleted successfully of false otherwise.
     */
    override fun deleteCluster(id: UUID): Boolean {
        return runBlocking {
            addResponseTimeLocks[id]?.withLock {
                try {
                    val cluster = clusterRepository.findById(id).orElseThrow()
                    clusterRepository.delete(cluster)
                    routingService.removeDynamicRoute(cluster)
                    addResponseTimeLocks.remove(id)
                    clusterResponseTimeInfos.remove(id)
                    true
                } catch (e: NoSuchElementException) {
                    false
                }
            } ?: false
        }
    }

    override fun getUsedInstances(): Set<InstanceHandle> {
        val clusters = listClusters()
        return clusters.fold(hashSetOf<InstanceHandle>(), { instances, cluster ->
            instances.addAll(cluster.instances.map { it.handle })
            instances
        })
    }

    private fun manageInstanceStates(cluster: Cluster, getTopTwo: Boolean = true) {
            if (!cluster.enableInstanceStateManagement) return
            if (cluster.instances.size == 0) return
            if (clusterResponseTimeInfos[cluster.id]?.lastTransitionTime?.plusMillis(transitionInterval)
                    ?.isAfter(Instant.now()) == true
            ) return

            logger.info("Managing instance states")
            clusterResponseTimeInfos[cluster.id]?.flags?.clear()

            if (getTopTwo) {
                val (newPrimary, newBackup) = getTopTwoInstances(cluster)

                logger.info("Old access instance ${cluster.accessInstance}. Old backup instance ${cluster.backupInstance}.")
                logger.info("New access instance $newPrimary. New backup instance $newBackup.")

                if (!(newPrimary == cluster.accessInstance && newBackup == cluster.backupInstance)) {
                    cluster.accessInstance = newPrimary
                    cluster.backupInstance = newBackup
                    clusterRepository.save(cluster)
                    transitionClusterAccess(cluster)
                }
            } else {
                logger.info("Access instance ${cluster.accessInstance}. Backup instance ${cluster.backupInstance}.")
                transitionClusterAccess(cluster)
            }
    }

    private fun getTopTwoInstances(cluster: Cluster): Pair<InstanceHandle?, InstanceHandle?> {
        logger.info("Getting top two instances. AccessInstance: ${cluster.accessInstance}. Backup Instance: ${cluster.backupInstance}. Instances: ${cluster.instances}")
        var newPrimary = cluster.accessInstance
        var newBackup = cluster.backupInstance
        cluster.instances.forEach { entry ->
            val primaryPriority = cluster.instances.find { it.handle == newPrimary }?.priority ?: Integer.MAX_VALUE
            val backupPriority = cluster.instances.find { it.handle == newBackup }?.priority ?: Integer.MAX_VALUE
            if (entry.priority < primaryPriority) {
                newBackup = newPrimary
                newPrimary = entry.handle
            } else if (entry.handle != newPrimary && entry.priority < backupPriority) {
                newBackup = entry.handle
            }
        }
        return Pair(newPrimary, newBackup)
    }

    private fun transitionClusterAccess(cluster: Cluster) {
        logger.info("Transitioning access for cluster ${cluster.id}")
        val toStart = if (cluster.enableHotBackup)
            listOfNotNull(cluster.accessInstance, cluster.backupInstance)
            else
                listOfNotNull(cluster.accessInstance)
        val infos = serviceProvider.getInstances(cluster.instances.map { it.handle })
        val toStop = infos.filter { !(it.handle == cluster.accessInstance || (cluster.enableHotBackup && it.handle == cluster.backupInstance)) }
            .filter { !InstanceState.StoppingInstanceStates.contains(it.state) }
        logger.info("Instances to start: $toStart")
        logger.info("Instances to stop: $toStop")

        clusterRepository.setState(cluster.id, ClusterState.TRANSITIONING)
        logger.info("Waiting for instances to start")
        toStart.forEach { serviceProvider.startInstance(it) }

        coroutineScope.launch {
            toStart.forEach { serviceProvider.waitForState(it, InstanceState.RUNNING) }
            routingService.updateDynamicRoute(cluster)

            logger.info("Waiting for instances to stop")
            toStop.forEach { serviceProvider.stopInstance(it.handle) }
            toStop.forEach { serviceProvider.waitForState(it.handle, InstanceState.STOPPED) }
            logger.info("Transition completed")
            clusterRepository.setState(cluster.id, ClusterState.OPERATIONAL)
            clusterResponseTimeInfos[cluster.id]?.lastTransitionTime = Instant.now()
        }
    }

    override fun editInstance(id: UUID, handle: InstanceHandle, payload: Map<String, Any>): ClusterMembership {
        return runBlocking {
            addResponseTimeLocks[id]?.withLock {
                val cluster = getCluster(id)
                val membership = cluster.instances.find { it.handle == handle } ?: throw Exception("Membership not found in cluster")
                payload.forEach { (key: String, value: Any) ->
                    if (key == "priority" && value is String) {
                        logger.info("Setting $key to $value")
                        membership.priority = value.toInt()
                    }
                }
                clusterRepository.save(cluster)
                membership
            } ?: throw ClusterServiceException("ID $id not found in addResponseTimeLocks")
        }
    }

    private fun removeDeletedInstances(cluster: Cluster): Cluster {
        val toDelete = cluster.instances.filter {
            try {
                serviceProvider.getInstance(it.handle)
                false
            } catch (e: InstanceDeletedException) {
                true
            }
        }
        toDelete.forEach { cluster.instances.remove(it) }
        if (toDelete.any { it.handle == cluster.accessInstance }) {
            cluster.accessInstance = null
            cluster.backupInstance = null // necessary to avoid edge cases where we have a backup instance but no primary instance
        }
        if (toDelete.any { it.handle == cluster.backupInstance }) {
            cluster.backupInstance = null
        }

        if (toDelete.any { it.handle == cluster.accessInstance } || toDelete.any { it.handle == cluster.backupInstance }) {
            val (newPrimary, newBackup) = getTopTwoInstances(cluster)
            logger.info("New access instance $newPrimary. New backup instance $newBackup.")
            cluster.accessInstance = newPrimary
            cluster.backupInstance = newBackup

            clusterRepository.save(cluster)
            routingService.updateDynamicRoute(cluster)
        }
        return cluster
    }

    override fun addResponseTime(id: UUID, time: Long) {
        val now = Instant.now()
        if (clusterResponseTimeInfos[id]?.lastUpdateTime?.plusMillis(addResponseTimeInterval)?.isBefore(now) == true) {
            try {
                if (addResponseTimeLocks[id]?.tryLock() == true) {
                    val cluster = clusterResponseTimeInfos[id]

                    if (cluster == null || cluster.lastUpdateTime.plusMillis(addResponseTimeInterval).isAfter(now)) {
                        return
                    }

                    if (cluster.requestCount > minRequestCount && time > cluster.rtt + 4 * cluster.rttVar) {
                        logger.info("Flagging request for cluster $id. Request Count: ${cluster.requestCount}, RTT: ${cluster.rtt}, RTTVAR: ${cluster.rttVar}, TIME: $time")
                        flagRequest(id)
                    }

                    if (cluster.requestCount != 0L) {
                        cluster.rttVar = (cluster.rttVar * (1 - BETA) + abs(time - cluster.rtt) * BETA).toLong()
                        cluster.rtt = (cluster.rtt * (1 - ALPHA) + time * ALPHA).toLong()
                    } else {
                        cluster.rttVar = time / 2
                        cluster.rtt = time
                    }
                    ++cluster.requestCount
                    cluster.lastUpdateTime = now
                    clusterResponseTimeInfos[id] = cluster

                    logger.info("New response time added to cluster $id. Request Count: ${cluster.requestCount}, RTT: ${cluster.rtt}, RTTVAR: ${cluster.rttVar}, TIME: $time")
                }
            } finally {
                addResponseTimeLocks[id]?.unlock()
            }
        }
    }

    override fun flagRequest(id: UUID) {
        logger.info("Flagging request for cluster $id.")
        val now = Instant.now()
        clusterResponseTimeInfos[id]?.flags?.add(now)
        while (clusterResponseTimeInfos[id]?.flags?.first?.isBefore(now.minusSeconds(60)) == true) {
            clusterResponseTimeInfos[id]?.flags?.removeFirst()
        }

        if ((clusterResponseTimeInfos[id]?.flags?.size ?: 0) > 4) {
            coroutineScope.launch {
                addResponseTimeLocks[id]?.withLock {
                    val cluster = getCluster(id)
                    val membership = cluster.instances.find { it.handle == cluster.accessInstance }
                    if (membership != null && cluster.enableAutomaticPriorityAdjustment) ++membership.priority
                    clusterRepository.save(cluster)
                    manageInstanceStates(cluster)
                }
            }
        }
    }

    fun getResponseTimeInfo(id: UUID): ClusterResponseTimeInfo {
        val info = clusterResponseTimeInfos[id] ?: throw ClusterServiceException("ID not found in clusterResponseTimeInfos")
        return ClusterResponseTimeInfo(info)
    }
}
