package com.flitterkomskis.cloudfailover.cluster

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceHandle
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceInfo
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceModelAssembler
import com.flitterkomskis.cloudfailover.cloudproviders.ServiceProvider
import com.flitterkomskis.cloudfailover.reverseproxy.DynamicRoutingService
import java.util.UUID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.hateoas.EntityModel
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * Provides [Cluster] access management API methods, such as accessing and changing the access instance of a [Cluster].
 */
@RestController
@CrossOrigin
@RequestMapping("/api/traffic")
class TrafficController {
    private val logger: Logger = LoggerFactory.getLogger(TrafficController::class.java)
    @Autowired private lateinit var clusterService: ClusterService
    @Autowired private lateinit var routingService: DynamicRoutingService
    @Autowired private lateinit var serviceProvider: ServiceProvider
    @Autowired private lateinit var instanceModelAssembler: InstanceModelAssembler
    @Autowired private lateinit var clusterModelAssembler: ClusterModelAssembler

    /**
     * Returns the [InstanceInfo] for the access instance of the [Cluster] given by id.
     * @param id ID that uniquely identifies the cluster.
     * @return The [InstanceInfo] for the instance serving as the access instance of the [Cluster].
     */
    @GetMapping("/{id}")
    fun getCurrentCloudProvider(@PathVariable id: UUID): EntityModel<InstanceInfo> {
        val cluster = clusterService.getCluster(id)
        val accessInstance = cluster.accessInstance ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No access instance specified")
        return instanceModelAssembler.toModel(serviceProvider.getInstance(accessInstance))
    }

    @PutMapping("/{id}")
    fun switchCurrentCloudProvider(@PathVariable id: UUID, @RequestBody handle: InstanceHandle): EntityModel<Cluster> {
        logger.info("Change access instance for cluster $id to $handle")
        val cluster = clusterService.getCluster(id)
        cluster.accessInstance = handle
        routingService.updateDynamicRoute(cluster)
        return clusterModelAssembler.toModel(clusterService.setAccessInstance(id, handle))
    }
}
