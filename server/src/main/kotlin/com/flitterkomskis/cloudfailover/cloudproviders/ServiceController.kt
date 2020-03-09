package com.flitterkomskis.cloudfailover.cloudproviders

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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// TODO: Attach VM instance to cluster
// TODO: Get all VMs in cluster
// TODO: Figure out whether the above actions should be in cluster controller or service controller

@RestController
@CrossOrigin
@RequestMapping("/api/instances")
class ServiceController {
    private val logger: Logger = LoggerFactory.getLogger(ServiceController::class.java)
    @Autowired private lateinit var serviceProvider: ServiceProvider
    @Autowired private lateinit var assembler: InstanceModelAssembler
    private val mapper = jacksonObjectMapper()

    @GetMapping("")
    fun getInstances(): CollectionModel<EntityModel<InstanceInfo>> {
        val instances = serviceProvider.listInstances()
        return CollectionModel(instances.map { instanceInfo ->
            assembler.toModel(instanceInfo)
            },
            linkTo(methodOn(ServiceController::class.java).getInstances()).withSelfRel())
    }

    @GetMapping("/{handle}")
    fun getInstance(@PathVariable handle: String): EntityModel<InstanceInfo> {
        val instanceHandle = mapper.readValue(handle, InstanceHandle::class.java)
        logger.info("Request to get instance $instanceHandle")
        return assembler.toModel(serviceProvider.getInstance(instanceHandle))
    }

    @PostMapping("")
    fun createInstance(@RequestBody request: CreateInstanceRequest) {
        logger.info("Request to create instance with payload $request")
        serviceProvider.createInstance(request.provider, request.name, request.type, request.imageId, request.region)
    }

    @DeleteMapping("/{handle}")
    fun deleteInstance(@PathVariable handle: String): EntityModel<InstanceInfo> {
        val instanceHandle = mapper.readValue(handle, InstanceHandle::class.java)
        logger.info("Request to delete instance $instanceHandle")
        serviceProvider.deleteInstance(instanceHandle)
        return assembler.toModel(serviceProvider.getInstance(instanceHandle))
    }

    @PutMapping("/{handle}/start")
    fun startInstance(@PathVariable handle: String): EntityModel<InstanceInfo> {
        val instanceHandle = mapper.readValue(handle, InstanceHandle::class.java)
        logger.info("Request to start instance $instanceHandle")
        serviceProvider.startInstance(instanceHandle)
        return assembler.toModel(serviceProvider.getInstance(instanceHandle))
    }

    @PutMapping("/{handle}/stop")
    fun stopInstance(@PathVariable handle: String): EntityModel<InstanceInfo> {
        val instanceHandle = mapper.readValue(handle, InstanceHandle::class.java)
        logger.info("Request to stop instance $instanceHandle")
        serviceProvider.stopInstance(instanceHandle)
        return assembler.toModel(serviceProvider.getInstance(instanceHandle))
    }
}
