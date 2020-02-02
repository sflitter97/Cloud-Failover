package com.flitterkomskis.cloudfailover

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ClusterController {

    @GetMapping("/clusters")
    fun getClusters() {}

    @GetMapping("/clusters/{id}")
    fun getCluster(@PathVariable id: String) {}

    @PostMapping("/clusters")
    fun createCluster() {}

    @PutMapping("/clusters/{id}")
    fun editCluster(@PathVariable id: String) {}

    @DeleteMapping("/cluster/{id}")
    fun deleteCluster(@PathVariable id: String) {}
}