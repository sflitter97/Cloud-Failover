package com.flitterkomskis.cloudfailover.cluster

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceHandle
import java.util.UUID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
@RequestMapping("/clusters")
class ClusterController {
    private val logger: Logger = LoggerFactory.getLogger(ClusterController::class.java)
    @Autowired private lateinit var clusterService: ClusterService
    @Autowired private lateinit var assembler: ClusterModelAssembler

    @GetMapping("")
    fun getClusters(): CollectionModel<EntityModel<Cluster>> {
        logger.info(clusterService.listClusters().toString())
        val clusters = clusterService.listClusters()
        return CollectionModel(clusters.map { cluster ->
                assembler.toModel(cluster)
            },
            linkTo(methodOn(ClusterController::class.java).getClusters()).withSelfRel(),
            Link("/profile/clusters", "profile")
        )
    }

    @GetMapping("/{id}")
    fun getCluster(@PathVariable id: UUID): EntityModel<Cluster> {
        return assembler.toModel(clusterService.getCluster(id))
    }

    @PostMapping("")
    fun createCluster(@RequestParam name: String): EntityModel<Cluster> {
        return assembler.toModel(clusterService.createCluster(name))
    }

    @PatchMapping("/{id}")
    fun editCluster(@PathVariable id: UUID, @RequestBody payload: Map<String, Any>): EntityModel<Cluster> {
        return assembler.toModel(clusterService.updateCluster(id, payload))
    }

    @DeleteMapping("/{id}")
    fun deleteCluster(@PathVariable id: UUID): Boolean {
        return clusterService.deleteCluster(id)
    }

    @PostMapping("/{id}/instances")
    fun addInstance(@PathVariable id: UUID, @RequestBody handle: InstanceHandle): EntityModel<Cluster> {
        return assembler.toModel(clusterService.addInstance(id, handle))
    }

    @DeleteMapping("/{id}/instances")
    fun deleteInstance(@PathVariable id: UUID, @RequestBody handle: InstanceHandle): EntityModel<Cluster> {
        return assembler.toModel(clusterService.removeInstance(id, handle))
    }
}
