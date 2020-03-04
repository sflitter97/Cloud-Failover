package com.flitterkomskis.cloudfailover.cloudproviders

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.Base64

// TODO: Attach VM instance to cluster
// TODO: Get all VMs in cluster
// TODO: Figure out whether the above actions should be in cluster controller or service controller

@RestController
@CrossOrigin
class ServiceController {
    private val logger: Logger = LoggerFactory.getLogger(ServiceController::class.java)
    @Autowired private lateinit var serviceProvider: ServiceProvider
    @Autowired private lateinit var assembler: InstanceModelAssembler

    @GetMapping("/instances")
    fun getInstances(): CollectionModel<EntityModel<InstanceInfo>> {
        val instances = serviceProvider.listInstances()
        return CollectionModel(instances.map { instanceInfo ->
            assembler.toModel(instanceInfo)
        },
            linkTo(methodOn(ServiceController::class.java).getInstances()).withSelfRel())
    }

    @GetMapping("/instances/{handle}")
    fun getInstance(@PathVariable handle: InstanceHandle): EntityModel<InstanceInfo> {
        logger.info(handle.toString());
        return assembler.toModel(serviceProvider.getInstance(handle))
    }

    @PostMapping("/instances")
    fun createInstance(@RequestBody request: CreateInstanceRequest) {
        serviceProvider.createInstance(request.provider, request.name, request.type, request.imageId, request.region)
    }

    @PutMapping("/instances/{handle}")
    fun editInstance(@PathVariable handle: InstanceHandle) {}

    @DeleteMapping("/instances/{handle}")
    fun deleteInstance(@PathVariable handle: InstanceHandle) {}

    @PutMapping("/instances/{handle}/start")
    fun startInstance(@PathVariable handle: InstanceHandle) {}

    @PutMapping("/instances/stop/{handle}/stop")
    fun stopInstance(@PathVariable handle: InstanceHandle) {}
}
