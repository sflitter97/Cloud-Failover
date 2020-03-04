package com.flitterkomskis.cloudfailover.cluster

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Component
class ClusterModelAssembler : RepresentationModelAssembler<Cluster, EntityModel<Cluster>> {
    private val logger: Logger = LoggerFactory.getLogger(ClusterModelAssembler::class.java)
    override fun toModel(entity: Cluster): EntityModel<Cluster> {
        logger.info(ServletUriComponentsBuilder.fromCurrentRequest().build().toString())
        val host = ServletUriComponentsBuilder.fromCurrentRequestUri().replacePath(null)
        return EntityModel(entity,
            linkTo(methodOn(ClusterController::class.java).getCluster(entity.id)).withSelfRel(),
            linkTo(ClusterController::class.java).slash(entity.id).slash("instances").withRel("instances"),
            Link(host.path("/access/${entity.id}").build().toUriString(), "access")
        )
    }
}
