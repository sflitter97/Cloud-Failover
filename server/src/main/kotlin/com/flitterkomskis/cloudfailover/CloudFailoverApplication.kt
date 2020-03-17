package com.flitterkomskis.cloudfailover

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.netflix.zuul.EnableZuulProxy
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

/**
 * Miscellaneous setup for the application. Enables CORS and forwards non-api requests to / so the web ui can handle
 * them.
 */
@EnableZuulProxy
@SpringBootApplication
@Controller
class CloudFailoverApplication {
    /**
     * Enable CORS for ease of development, should be removed before deploying to production. Allows requests to the
     * api from all hosts and all request types.
     */
    @Bean
    fun corsFilter(): CorsFilter? {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()
        config.allowCredentials = true
        config.allowedOrigins = listOf("*")
        config.allowedHeaders = listOf("Origin", "Content-Type", "Accept")
        config.allowedMethods = listOf("GET", "POST", "PUT", "OPTIONS", "DELETE", "PATCH")
        source.registerCorsConfiguration("/**", config)
        return CorsFilter(source)
    }

    /**
     * Forwards all non-api requests to the root of the server (which will server index.html from the frontend). This
     * is needed so the React router can handle all frontend requests and route them accordingly.
     */
    @RequestMapping("/{path:[^\\.]*}", "/**/{path:^(?!api).*}/{path:[^\\.]*}")
    fun forward(): String {
        return "forward:/"
    }
}

/**
 * Entry point for the application.
 */
fun main(args: Array<String>) {
    runApplication<CloudFailoverApplication>(*args)
}
