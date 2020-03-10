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

interface ClusterService {
    fun listClusters(): List<Cluster>
    fun getCluster(id: UUID): Cluster
    fun createCluster(payload: Map<String, Any>): Cluster
    fun updateCluster(id: UUID, payload: Map<String, Any>): Cluster
    fun addInstance(id: UUID, handle: InstanceHandle): Cluster
    fun removeInstance(id: UUID, handle: InstanceHandle): Cluster
    fun setAccessInstance(id: UUID, handle: InstanceHandle): Cluster
    fun deleteCluster(id: UUID): Boolean
}

@Service
class ClusterServiceImpl : ClusterService {
    private val logger: Logger = LoggerFactory.getLogger(ClusterServiceImpl::class.java)
    @Autowired private lateinit var clusterRepository: ClusterRepository
    @Autowired private lateinit var routingService: DynamicRoutingService

    override fun listClusters(): List<Cluster> {
        return clusterRepository.findAll()
    }

    override fun getCluster(id: UUID): Cluster {
        return clusterRepository.findById(id).orElseThrow()
    }

    fun updateClusterFromPayload(cluster: Cluster, payload: Map<String, Any>): Cluster {
        payload.forEach { (key: String, value: Any) ->
            if (key == "name" && value is String) {
                logger.info("Setting $key to $value")
                cluster.name = value
            } else if (key == "instances" && value is List<*>) {
                val mapper = jacksonObjectMapper()
                // Cast List<*> to List<Map>
                value.forEach { handle ->
                    logger.info(handle.toString())
                    logger.info(handle!!::class.toString())
                }
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

    override fun createCluster(payload: Map<String, Any>): Cluster {
        var cluster = Cluster()
        cluster = updateClusterFromPayload(cluster, payload)
        clusterRepository.save(cluster)
        return cluster
    }

    override fun updateCluster(id: UUID, payload: Map<String, Any>): Cluster {
        var cluster = getCluster(id)
        cluster = updateClusterFromPayload(cluster, payload)
        clusterRepository.save(cluster)
        if (payload.containsKey("targetPort") || payload.containsKey("targetPath")) {
            routingService.updateDynamicRoute(cluster)
        }
        return cluster
    }

    override fun addInstance(id: UUID, handle: InstanceHandle): Cluster {
        val cluster = getCluster(id)
        cluster.addInstance(handle)
        logger.info("Added $handle to cluster $id")
        clusterRepository.save(cluster)
        return cluster
    }

    override fun removeInstance(id: UUID, handle: InstanceHandle): Cluster {
        val cluster = getCluster(id)
        cluster.removeInstance(handle)
        logger.info("Removed $handle from cluster $id")
        clusterRepository.save(cluster)
        return cluster
    }

    override fun setAccessInstance(id: UUID, handle: InstanceHandle): Cluster {
        val cluster = getCluster(id)
        cluster.accessInstance = handle
        logger.info("Set $handle as access instance for cluster $id")
        clusterRepository.save(cluster)
        return cluster
    }

    override fun deleteCluster(id: UUID): Boolean {
        try {
            val cluster = clusterRepository.findById(id).orElseThrow()
            clusterRepository.delete(cluster)
            return true
        } catch (e: NoSuchElementException) {
            return false
        }
    }
}
