package com.flitterkomskis.cloudfailover

import com.flitterkomskis.cloudfailover.cloudproviders.ServiceProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.netflix.zuul.EnableZuulProxy
import org.springframework.context.annotation.Bean
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@EnableZuulProxy
@SpringBootApplication
class CloudFailoverApplication {
    @Autowired
    private lateinit var serviceProvider: ServiceProvider

    @Bean
    fun corsFilter(): CorsFilter? {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()
        config.allowCredentials = true
        // Don't do this in production, use a proper list  of allowed origins
        config.allowedOrigins = listOf("*")
        config.allowedHeaders = listOf("Origin", "Content-Type", "Accept")
        config.allowedMethods = listOf("GET", "POST", "PUT", "OPTIONS", "DELETE", "PATCH")
        source.registerCorsConfiguration("/**", config)
        return CorsFilter(source)
    }
}

    fun main(args: Array<String>) {
        runApplication<CloudFailoverApplication>(*args)
    }