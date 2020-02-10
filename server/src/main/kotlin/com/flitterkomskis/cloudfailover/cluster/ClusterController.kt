package com.flitterkomskis.cloudfailover.cluster

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceHandle
import java.util.UUID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ClusterController {
    private val logger: Logger = LoggerFactory.getLogger(ClusterController::class.java)
    @Autowired private lateinit var clusterService: ClusterService

    @GetMapping("/clusters")
    fun getClusters(): List<Cluster> {
        logger.info(clusterService.listClusters().toString())
        return clusterService.listClusters()
    }

    @GetMapping("/clusters/{id}")
    fun getCluster(@PathVariable id: UUID): Cluster {
        return clusterService.getCluster(id)
    }

    @PostMapping("/clusters")
    fun createCluster(@RequestParam name: String): Cluster {
        return clusterService.createCluster(name)
    }

    @PatchMapping("/clusters/{id}")
    fun editCluster(@PathVariable id: UUID, @RequestBody payload: Map<String, Any>): Cluster {
        return clusterService.updateCluster(id, payload)
    }

    @DeleteMapping("/clusters/{id}")
    fun deleteCluster(@PathVariable id: UUID): Boolean {
        return clusterService.deleteCluster(id)
    }

    @PostMapping("/clusters/instances/{id}")
    fun addInstance(@PathVariable id: UUID, @RequestBody handle: InstanceHandle): Cluster {
        return clusterService.addInstance(id, handle)
    }

    @DeleteMapping("/clusters/instances/{id}")
    fun deleteInstance(@PathVariable id: UUID, @RequestBody handle: InstanceHandle): Cluster {
        return clusterService.removeInstance(id, handle)
    }
}
