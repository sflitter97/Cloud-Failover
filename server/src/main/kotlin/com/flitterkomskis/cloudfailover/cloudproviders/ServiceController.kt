package com.flitterkomskis.cloudfailover.cloudproviders

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Provides Instance management API methods.
 */
@RestController
@CrossOrigin
@RequestMapping("/api/instances")
class ServiceController {
    private val logger: Logger = LoggerFactory.getLogger(ServiceController::class.java)
    @Autowired private lateinit var serviceProvider: ServiceProvider
    @Autowired private lateinit var assembler: InstanceModelAssembler
    private val mapper = jacksonObjectMapper()

    /**
     * Returns a list of all instances from the [ServiceProvider].
     * @return A [CollectionModel] of [EntityModel] of [InstanceInfo]s from the cloud providers.
     */
    @GetMapping("")
    fun getInstances(): CollectionModel<EntityModel<InstanceInfo>> {
        val instances = serviceProvider.listInstances()
        return CollectionModel(instances.map { instanceInfo ->
            assembler.toModel(instanceInfo)
            },
            linkTo(methodOn(ServiceController::class.java).getInstances()).withSelfRel())
    }

    /**
     * Returns the [InstanceInfo] from the [ServiceProvider] given its handle.
     * @param handle Stringified [InstanceHandle] that uniquely identifies the instance.
     * @return The [InstanceInfo] for the given handle.
     */
    @GetMapping("/{handle}")
    fun getInstance(@PathVariable handle: String): EntityModel<InstanceInfo> {
        val instanceHandle = mapper.readValue(handle, InstanceHandle::class.java)
        logger.info("Request to get instance $instanceHandle")
        return assembler.toModel(serviceProvider.getInstance(instanceHandle))
    }

    /**
     * Creates the instance with details specified in the [CreateInstanceRequest].
     * @param request [CreateInstanceRequest] with the details of the instance to create.
     */
    @PostMapping("")
    fun createInstance(@RequestBody request: CreateInstanceRequest): ResponseEntity<String> {
        try {
            logger.info("Request to create instance with payload $request")
            serviceProvider.createInstance(request.provider, request.name, request.type, request.imageId, request.region)
            return ResponseEntity<String>("", HttpHeaders(), HttpStatus.CREATED)
        } catch(e: Exception) {
            e.printStackTrace()
            return ResponseEntity("", HttpHeaders(), HttpStatus.BAD_REQUEST)
        }
    }

    /**
     * Deletes the instance with the given handle.
     * @param handle Stringified [InstanceHandle] that uniquely identifies the instance.
     * @return True if the instance was deleted successfully and false otherwise.
     */
    @DeleteMapping("/{handle}")
    fun deleteInstance(@PathVariable handle: String): ResponseEntity<String> {
        val instanceHandle = mapper.readValue(handle, InstanceHandle::class.java)
        logger.info("Request to delete instance $instanceHandle")
        serviceProvider.deleteInstance(instanceHandle)
        return ResponseEntity.ok("")
    }

    /**
     * Starts the instance with the given handle.
     * @param handle Stringified [InstanceHandle] that uniquely identifies the instance.
     * @return The [InstanceInfo] for the given handle.
     */
    @PutMapping("/{handle}/start")
    fun startInstance(@PathVariable handle: String): EntityModel<InstanceInfo> {
        val instanceHandle = mapper.readValue(handle, InstanceHandle::class.java)
        logger.info("Request to start instance $instanceHandle")
        serviceProvider.startInstance(instanceHandle)
        return assembler.toModel(serviceProvider.getInstance(instanceHandle))
    }

    /**
     * Stops the instance with the given handle.
     * @param handle Stringified [InstanceHandle] that uniquely identifies the instance.
     * @return The [InstanceInfo] for the given handle.
     */
    @PutMapping("/{handle}/stop")
    fun stopInstance(@PathVariable handle: String): EntityModel<InstanceInfo> {
        val instanceHandle = mapper.readValue(handle, InstanceHandle::class.java)
        logger.info("Request to stop instance $instanceHandle")
        serviceProvider.stopInstance(instanceHandle)
        return assembler.toModel(serviceProvider.getInstance(instanceHandle))
    }
}
