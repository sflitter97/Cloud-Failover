package com.flitterkomskis.cloudfailover.cluster

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.UUID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/**
 * Transforms a [ClusterMembership] into an [EntityModel] of the cluster membership, which contains the cluster
 * membership and relevant [Link]s in the REST API.
 */
@Component
class ClusterMembershipAssembler : RepresentationModelAssembler<ClusterMembership, EntityModel<ClusterMembership>> {
    private val logger: Logger = LoggerFactory.getLogger(ClusterModelAssembler::class.java)
    private val mapper = jacksonObjectMapper()

    /**
     * Takes the provided [ClusterMembership] and returns an [EntityModel] of the membership.
     * @param entity The membership to convert.
     * @return The [EntityModel] of membership.
     */
    override fun toModel(entity: ClusterMembership): EntityModel<ClusterMembership> {
        logger.info(ServletUriComponentsBuilder.fromCurrentRequest().build().toString())
        return EntityModel(entity)
    }
    fun toModel(id: UUID, entity: ClusterMembership): EntityModel<ClusterMembership> {
        logger.info(ServletUriComponentsBuilder.fromCurrentRequest().build().toString())
        val handleStr = mapper.writeValueAsString(entity.handle)
        return EntityModel(entity,
            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(ClusterController::class.java).getInstance(id, handleStr)).withSelfRel()
        )
    }
}
