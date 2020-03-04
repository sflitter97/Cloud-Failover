package com.flitterkomskis.cloudfailover.cloudproviders

import com.flitterkomskis.cloudfailover.cluster.Cluster
import com.flitterkomskis.cloudfailover.cluster.ClusterController
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Component
class InstanceModelAssembler : RepresentationModelAssembler<InstanceInfo, EntityModel<InstanceInfo>> {
    private val logger: Logger = LoggerFactory.getLogger(InstanceModelAssembler::class.java)
    override fun toModel(entity: InstanceInfo): EntityModel<InstanceInfo> {
        logger.info(ServletUriComponentsBuilder.fromCurrentRequest().build().toString())
        val host = ServletUriComponentsBuilder.fromCurrentRequestUri().replacePath(null)
        return EntityModel(entity//,
            //WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(ServiceController::class.java).getInstance(entity.handle)).withSelfRel()
        )
    }
}