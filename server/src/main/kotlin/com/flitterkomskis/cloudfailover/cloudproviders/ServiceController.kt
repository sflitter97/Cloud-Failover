package com.flitterkomskis.cloudfailover.cloudproviders

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController

// TODO: Attach VM instance to cluster
// TODO: Get all VMs in cluster
// TODO: Figure out whether the above actions should be in cluster controller or service controller

@RestController
class ServiceController {
    val serviceProvider = ServiceProvider()

    @GetMapping("/instances")
    fun getInstances(): List<InstanceInfo> {
        return serviceProvider.listInstances()
    }

    @GetMapping("/instances/{instanceId}")
    fun getInstance(@PathVariable instanceId: String) {}

    @PostMapping("/instances")
    fun createInstance() {}

    @PutMapping("/instances/{instanceId}")
    fun editInstance(@PathVariable instanceId: String) {}

    @DeleteMapping("/instances/{instanceId}")
    fun deleteInstance(@PathVariable instanceId: String) {}

    @PutMapping("/instances/start/{instanceId}")
    fun startInstance(@PathVariable instanceId: Int) {}

    @PutMapping("/instances/stop/{instanceId}")
    fun stopInstance(@PathVariable instanceId: String) {}
}