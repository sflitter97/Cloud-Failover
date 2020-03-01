package com.flitterkomskis.cloudfailover.cluster

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceHandle
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceInfo
import com.flitterkomskis.cloudfailover.cloudproviders.ServiceProvider
import com.flitterkomskis.cloudfailover.reverseproxy.DynamicRoutingService
import java.util.UUID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
class TrafficController {
    private val logger: Logger = LoggerFactory.getLogger(TrafficController::class.java)
    @Autowired private lateinit var clusterService: ClusterService
    @Autowired private lateinit var routingService: DynamicRoutingService
    @Autowired private lateinit var serviceProvider: ServiceProvider

    @GetMapping("/traffic/{id}")
    fun getCurrentCloudProvider(@PathVariable id: UUID): InstanceInfo? {
        val cluster = clusterService.getCluster(id)
        val accessInstance = cluster.accessInstance ?: return null
        return serviceProvider.getInstance(accessInstance)
    }

    @PutMapping("/traffic/{id}")
    fun switchCurrentCloudProvider(@PathVariable id: UUID, @RequestBody handle: InstanceHandle): Cluster {
        val cluster = clusterService.setAccessInstance(id, handle)
        routingService.updateDynamicRoute(cluster)
        return cluster
    }
}
