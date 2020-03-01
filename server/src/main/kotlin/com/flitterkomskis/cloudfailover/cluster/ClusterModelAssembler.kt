package com.flitterkomskis.cloudfailover.cluster

import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import org.springframework.stereotype.Component

@Component
class ClusterModelAssembler: RepresentationModelAssembler<Cluster, EntityModel<Cluster>> {
    override fun toModel(entity: Cluster): EntityModel<Cluster> {
        return EntityModel(entity,
            linkTo(methodOn(ClusterController::class.java).getCluster(entity.id)).withSelfRel(),
            linkTo(ClusterController::class.java).slash(entity.id).slash("instances").withRel("instances")
        )
    }
}