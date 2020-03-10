package com.flitterkomskis.cloudfailover.cloudproviders

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import org.springframework.stereotype.Component

@Component
class InstanceModelAssembler : RepresentationModelAssembler<InstanceInfo, EntityModel<InstanceInfo>> {
    private val logger: Logger = LoggerFactory.getLogger(InstanceModelAssembler::class.java)
    private val mapper = jacksonObjectMapper()

    override fun toModel(entity: InstanceInfo): EntityModel<InstanceInfo> {
        val handleStr = mapper.writeValueAsString(entity.handle)
        return EntityModel(entity,
            linkTo(methodOn(ServiceController::class.java).getInstance(handleStr)).withSelfRel(),
            linkTo(methodOn(ServiceController::class.java).deleteInstance(handleStr)).withRel("delete"),
            linkTo(methodOn(ServiceController::class.java).startInstance(handleStr)).withRel("start"),
            linkTo(methodOn(ServiceController::class.java).stopInstance(handleStr)).withRel("stop")
        )
    }
}
