package com.flitterkomskis.cloudfailover

import com.flitterkomskis.cloudfailover.cloudproviders.ServiceProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.netflix.zuul.EnableZuulProxy

@EnableZuulProxy
@SpringBootApplication
class CloudFailoverApplication
    @Autowired private lateinit var serviceProvider: ServiceProvider

fun main(args: Array<String>) {
    runApplication<CloudFailoverApplication>(*args)
}
