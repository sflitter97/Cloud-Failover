package com.flitterkomskis.cloudfailover.cluster

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceHandle
import com.flitterkomskis.cloudfailover.reverseproxy.DynamicRoutingService
import java.lang.IllegalArgumentException
import java.util.UUID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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
}

/**
 * Implementation of the [ClusterService] which saves [Cluster]s in the [ClusterRepository].
 */
@Service
class ClusterServiceImpl : ClusterService {
    private val logger: Logger = LoggerFactory.getLogger(ClusterServiceImpl::class.java)
    @Autowired private lateinit var clusterRepository: ClusterRepository
    @Autowired private lateinit var routingService: DynamicRoutingService

    /**
     * Finds and returns all [Cluster]s in the Mongo Database.
     * @return A [List] of the [Cluster]s in the [ClusterRepository].
     */
    override fun listClusters(): List<Cluster> {
        return clusterRepository.findAll()
    }

    /**
     * Finds and returns a specific [Cluster] in the Mongo Database given its id.
     * @param id ID that uniquely identifies the [Cluster].
     * @return The [Cluster] with the given id.
     */
    override fun getCluster(id: UUID): Cluster {
        return clusterRepository.findById(id).orElseThrow()
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
                cluster.instances = instanceList.toMutableList()
            } else if (key == "targetPort" && value is String) {
                logger.info("Setting $key to $value")
                cluster.targetPort = value.toInt()
            } else if (key == "targetPath" && value is String) {
                logger.info("Setting $key to $value")
                cluster.targetPath = value
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
        var cluster = getCluster(id)
        cluster = updateClusterFromPayload(cluster, payload)
        clusterRepository.save(cluster)
        if (payload.containsKey("targetPort") || payload.containsKey("targetPath")) {
            routingService.updateDynamicRoute(cluster)
        }
        return cluster
    }

    /**
     * Adds an instance to the [Cluster] with the given id.
     * @param id ID that uniquely identifies the [Cluster].
     * @param handle The instance to add to the [Cluster].
     * @return The [Cluster] after the instance has been added.
     */
    override fun addInstance(id: UUID, handle: InstanceHandle): Cluster {
        val cluster = getCluster(id)
        cluster.addInstance(handle)
        logger.info("Added $handle to cluster $id")
        clusterRepository.save(cluster)
        return cluster
    }

    /**
     * Removes an instance from the [Cluster] with the given id.
     * @param id ID that uniquely identifies the [Cluster].
     * @param handle The instance to add to the [Cluster].
     * @return The [Cluster] after the instance has been removed.
     */
    override fun removeInstance(id: UUID, handle: InstanceHandle): Cluster {
        val cluster = getCluster(id)
        cluster.removeInstance(handle)
        logger.info("Removed $handle from cluster $id")
        clusterRepository.save(cluster)
        return cluster
    }

    /**
     * Changes the access instance of a [Cluster] with the given id.
     * @param id ID that uniquely identifies the [Cluster].
     * @param handle The instance to become the new access instance.
     * @return The [Cluster] after the access instance has been set.
     */
    override fun setAccessInstance(id: UUID, handle: InstanceHandle): Cluster {
        val cluster = getCluster(id)
        cluster.accessInstance = handle
        logger.info("Set $handle as access instance for cluster $id")
        clusterRepository.save(cluster)
        return cluster
    }

    /**
     * Deletes a [Cluster] from the [ClusterRepository] and removes its route from the [DynamicRoutingService].
     * @param id ID that uniquely identifies the [Cluster].
     * @return True if the cluster was deleted successfully of false otherwise.
     */
    override fun deleteCluster(id: UUID): Boolean {
        return try {
            val cluster = clusterRepository.findById(id).orElseThrow()
            clusterRepository.delete(cluster)
            routingService.removeDynamicRoute(cluster)
            true
        } catch (e: NoSuchElementException) {
            false
        }
    }
}
