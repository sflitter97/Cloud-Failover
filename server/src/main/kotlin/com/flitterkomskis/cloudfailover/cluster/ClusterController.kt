package com.flitterkomskis.cloudfailover.cluster

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import org.springframework.web.bind.annotation.RestController

/**
 * Provides [Cluster] management API methods. Converts [Cluster]s to an [EntityModel], which contains the [Cluster]
 * as well as relevant [Link]s.
 */
@RestController
@CrossOrigin
@RequestMapping("/api/clusters")
class ClusterController {
    private val logger: Logger = LoggerFactory.getLogger(ClusterController::class.java)
    @Autowired private lateinit var clusterService: ClusterService
    @Autowired private lateinit var assembler: ClusterModelAssembler
    @Autowired private lateinit var membershipAssembler: ClusterMembershipAssembler
    private val mapper = jacksonObjectMapper()

    /**
     * Returns a list of all clusters from the [ClusterService].
     * @return A [CollectionModel] of [EntityModel] of [Cluster]s in the repository.
     */
    @GetMapping("")
    fun getClusters(): CollectionModel<EntityModel<ClusterModel>> {
        logger.info(clusterService.listClusters().toString())
        val clusters = clusterService.listClusters()
        return CollectionModel(clusters.map { cluster ->
                assembler.toModel(cluster)
            },
            linkTo(methodOn(ClusterController::class.java).getClusters()).withSelfRel()
        )
    }

    /**
     * Returns the [Cluster] from the [ClusterService] given its id.
     * @param id ID that uniquely identifies the [Cluster].
     * @return A [Cluster] with the given id.
     */
    @GetMapping("/{id}")
    fun getCluster(@PathVariable id: UUID): EntityModel<ClusterModel> {
        return assembler.toModel(clusterService.getCluster(id))
    }

    /**
     * Creates the [Cluster] with details specified in the payload.
     * @param payload Map of strings to any objects which specifies the details of the cluster to create.
     * May contain "name", "instances", "targetPort", or "targetPath". "name", "targetPort",
     * and "targetPath" should map to strings, while "instances" should map to a [List] of [Map] of strings to strings,
     * with each [Map] in the [List] containing the details for that instance.
     * @return The created [Cluster].
     */
    @PostMapping("")
    fun createCluster(@RequestBody payload: Map<String, Any>): EntityModel<ClusterModel> {
        return assembler.toModel(clusterService.createCluster(payload))
    }

    /**
     * Modifies the [Cluster] with the given id and with new state specified in the payload.
     * @param id ID that uniquely identifies the [Cluster].
     * @param payload Map of strings to any objects which specifies the details of the cluster to modify.
     * May contain "name", "instances", "targetPort", or "targetPath". "name", "targetPort",
     * and "targetPath" should map to strings, while "instances" should map to a [List] of [Map] of strings to strings,
     * with each [Map] in the [List] containing the details for that instance.
     * @return The updated [Cluster].
     */
    @PatchMapping("/{id}")
    fun editCluster(@PathVariable id: UUID, @RequestBody payload: Map<String, Any>): EntityModel<ClusterModel> {
        logger.info("Patch request for cluster $id with payload $payload")
        return assembler.toModel(clusterService.updateCluster(id, payload))
    }

    /**
     * Deletes the [Cluster] with the given id.
     * @param id ID that uniquely identifies the [Cluster].
     * @return True if the [Cluster] was deleted successfully and false otherwise.
     */
    @DeleteMapping("/{id}")
    fun deleteCluster(@PathVariable id: UUID): Boolean {
        return clusterService.deleteCluster(id)
    }

    /**
     * Adds an instance to the [Cluster] with the given id.
     * @param id ID that uniquely identifies the [Cluster].
     * @param handle The instance to add to the [Cluster].
     * @return The [Cluster] after the instance has been added.
     */
    @PostMapping("/{id}/instances")
    fun addInstance(@PathVariable id: UUID, @RequestBody handle: InstanceHandle): EntityModel<ClusterModel> {
        return assembler.toModel(clusterService.addInstance(id, handle))
    }

    /**
     * Removes an instance from the [Cluster] with the given id.
     * @param id ID that uniquely identifies the [Cluster].
     * @param handle The instance to remove from the [Cluster].
     * @return The [Cluster] after the instance has been removed.
     */
    @DeleteMapping("/{id}/instances")
    fun deleteInstance(@PathVariable id: UUID, @RequestBody handle: InstanceHandle): EntityModel<ClusterModel> {
        return assembler.toModel(clusterService.removeInstance(id, handle))
    }

    @GetMapping("/used_instances")
    fun getUsedInstances(): CollectionModel<InstanceHandle> {
        val instances = clusterService.getUsedInstances()
        return CollectionModel(instances)
    }

    @GetMapping("/{id}/instances")
    fun getInstances(@PathVariable id: UUID): CollectionModel<EntityModel<ClusterMembership>> {
        val cluster = clusterService.getCluster(id)
        return CollectionModel(cluster.instances.map {
            membershipAssembler.toModel(id, it)
        },
            linkTo(methodOn(ClusterController::class.java).getInstances(id)).withSelfRel()
        )
    }

    @GetMapping("/{id}/instances/{handle}")
    fun getInstance(@PathVariable id: UUID, @PathVariable handle: String): EntityModel<ClusterMembership> {
        val instanceHandle = mapper.readValue(handle, InstanceHandle::class.java)
        val cluster = clusterService.getCluster(id)
        return membershipAssembler.toModel(id, cluster.instances.first { it.handle == instanceHandle })
    }

    @PatchMapping("/{id}/instances/{handle}")
    fun editInstance(@PathVariable id: UUID, @PathVariable handle: String, @RequestBody payload: Map<String, Any>): EntityModel<ClusterMembership> {
        val instanceHandle = mapper.readValue(handle, InstanceHandle::class.java)
        clusterService.editInstance(id, instanceHandle, payload)
        val cluster = clusterService.getCluster(id)
        return membershipAssembler.toModel(id, cluster.instances.first { it.handle == instanceHandle })
    }
}
