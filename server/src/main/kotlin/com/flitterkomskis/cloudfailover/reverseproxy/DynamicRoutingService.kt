package com.flitterkomskis.cloudfailover.reverseproxy

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceState
import com.flitterkomskis.cloudfailover.cloudproviders.ServiceProvider
import com.flitterkomskis.cloudfailover.cluster.Cluster
import com.flitterkomskis.cloudfailover.cluster.ClusterService
import java.util.HashSet
import javax.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute
import org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

/**
 * Provides the ability to modify the routes in the Zuul Proxy, which forwards requests to a cluster to the appropriate
 * instance.
 */
@Service
class DynamicRoutingService {
    private val logger: Logger = LoggerFactory.getLogger(DynamicRoutingService::class.java)
    private val HTTP_PROTOCOL = "http://"
    private val ACCESS_PREFIX = "/api/access"
    @Autowired private lateinit var zuulProperties: ZuulProperties
    @Autowired private lateinit var zuulHandlerMapping: ZuulHandlerMapping
    @Autowired private lateinit var clusterService: ClusterService
    @Autowired private lateinit var serviceProvider: ServiceProvider

    /**
     * Initializes the Zuul Proxy when the application starts. Re-adds existing routes to the proxy.
     */
    @EventListener
    fun initialize(event: ContextRefreshedEvent) {
        try {
            clusterService.listClusters().forEach { cluster ->
                addDynamicRouteInZuul(cluster)
            }
            zuulHandlerMapping.setDirty(true)
        } catch (e: Exception) {
            logger.error("Error loading previously made routes.", e)
        }
    }

    /**
     * Updates the route for the given [Cluster].
     * @param cluster The [Cluster] whose route is to be updated.
     * @return Returns true if the route is updated successfully and false otherwise.
     */
    fun updateDynamicRoute(cluster: Cluster): Boolean {
        logger.debug("Applying cluster $cluster to zuul proxy.", cluster)
        addDynamicRouteInZuul(cluster)
        zuulHandlerMapping.setDirty(true)
        return true
    }

    private fun addDynamicRouteInZuul(cluster: Cluster) {
        val url = createTargetURL(cluster)
        zuulProperties.routes[cluster.id.toString()] = ZuulRoute(
            cluster.id.toString(),
            "$ACCESS_PREFIX/${cluster.id}/**",
            null,
            url,
            true,
            false,
            HashSet()
        )
    }

    /**
     * Helper function that creates the target URL for a given [Cluster]. Get the url of the access instance of the
     * [Cluster] and adds the port and path from the [Cluster].
     * @param cluster The [Cluster] to create a target url for.
     * @return The target url for cluster.
     */
    private fun createTargetURL(cluster: Cluster): String {
        val accessInstance = cluster.accessInstance ?: throw DynamicRoutingServiceException("Cluster has no access instance defined.")
        val instanceInfo = serviceProvider.getInstance(accessInstance)
        if (instanceInfo.state != InstanceState.RUNNING) {
            throw IllegalStateException("Cannot set route for cluster ${cluster.id}. Instance with handle $accessInstance is not running.")
        }
        val host = serviceProvider.getInstance(accessInstance).host
        val targetUrl = "${HTTP_PROTOCOL}$host:${cluster.targetPort}${cluster.targetPath}"
        logger.info("Target URL for cluster $cluster is $targetUrl")
        return targetUrl
    }

    /**
     * Removes a route for a [Cluster].
     * @param cluster The [Cluster] whose route to remove.
     * @return True if the route was removed and false otherwise.
     */
    fun removeDynamicRoute(cluster: Cluster): Boolean {
        zuulProperties.routes.remove(cluster.id.toString())
        return true
    }
}
