package com.flitterkomskis.cloudfailover.cluster

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceHandle
import java.util.UUID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

interface ClusterService {
    fun listClusters(): List<Cluster>
    fun getCluster(id: UUID): Cluster
    fun createCluster(name: String): Cluster
    fun updateCluster(id: UUID, payload: Map<String, Any>): Cluster
    fun addInstance(id: UUID, handle: InstanceHandle): Cluster
    fun removeInstance(id: UUID, handle: InstanceHandle): Cluster
    fun setAccessInstance(id: UUID, handle: InstanceHandle): Cluster
    fun deleteCluster(id: UUID): Boolean
}

@Service
class ClusterServiceImpl : ClusterService {
    private val logger: Logger = LoggerFactory.getLogger(ClusterServiceImpl::class.java)
    @Autowired lateinit var clusterRepository: ClusterRepository

    override fun listClusters(): List<Cluster> {
        return clusterRepository.findAll()
    }

    override fun getCluster(id: UUID): Cluster {
        return clusterRepository.findById(id).orElseThrow()
    }

    override fun createCluster(name: String): Cluster {
        val cluster = Cluster(name)
        clusterRepository.save(cluster)
        return cluster
    }

    override fun updateCluster(id: UUID, payload: Map<String, Any>): Cluster {
        val cluster = getCluster(id)
        payload.forEach { key: String, value: Any ->
            if (key.equals("name") && value is String) {
                cluster.name = value
            } else if (key.equals("targetPort") && value is Int) {
                cluster.targetPort = value
            } else if (key.equals("targetPath") && value is String) {
                cluster.targetPath = value
            }
        }
        clusterRepository.save(cluster)
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
