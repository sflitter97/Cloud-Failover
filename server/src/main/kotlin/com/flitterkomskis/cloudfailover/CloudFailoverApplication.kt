package com.flitterkomskis.cloudfailover

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CloudFailoverApplication

fun main(args: Array<String>) {
	runApplication<CloudFailoverApplication>(*args)
}
