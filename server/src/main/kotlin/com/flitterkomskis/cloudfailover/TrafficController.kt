package com.flitterkomskis.cloudfailover

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TrafficController {

    @GetMapping("/traffic")
    fun getCurrentCloudProvider() {}

    @PostMapping("/traffic")
    fun switchCurrentCloudProvider() {}
}