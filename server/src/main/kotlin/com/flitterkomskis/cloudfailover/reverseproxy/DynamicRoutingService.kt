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
import org.springframework.stereotype.Service

@Service
class DynamicRoutingService @Autowired constructor(
    private val zuulProperties: ZuulProperties,
    private val zuulHandlerMapping: ZuulHandlerMapping,
    private val clusterService: ClusterService
) {
    private val logger: Logger = LoggerFactory.getLogger(DynamicRoutingService::class.java)
    private val HTTP_PROTOCOL = "http://"
    private val ACCESS_PREFIX = "/api/access"
    @Autowired private lateinit var serviceProvider: ServiceProvider

    @PostConstruct
    fun initialize() {
        try {
            clusterService.listClusters().forEach { cluster ->
                addDynamicRouteInZuul(cluster)
            }
            zuulHandlerMapping.setDirty(true)
        } catch (e: Exception) {
            logger.error("Error loading previously made routes.", e)
        }
    }

    fun updateDynamicRoute(cluster: Cluster): Boolean {
        logger.debug("Applying cluster $cluster to zuul proxy.", cluster)
        addDynamicRouteInZuul(cluster)
        zuulHandlerMapping.setDirty(true)
        return true
    }

    private fun addDynamicRouteInZuul(cluster: Cluster) {
        val url = createTargetURL(cluster)
        zuulProperties.getRoutes().put(
            cluster.id.toString(),
            ZuulRoute(
                cluster.id.toString(), "$ACCESS_PREFIX/${cluster.id}/**",
                null, url, true, false, HashSet()
            )
        )
    }

    private fun createTargetURL(cluster: Cluster): String {
        val accessInstance = cluster.accessInstance ?: throw DynamicRoutingServiceException("Cluster has no access instance defined.")
        val instanceInfo = serviceProvider.getInstance(accessInstance)
        if (instanceInfo.state != InstanceState.RUNNING) {
            throw IllegalStateException("Cannot change access instance for cluster ${cluster.id}. Instance with handle $accessInstance is not running.")
        }
        val host = serviceProvider.getInstance(accessInstance).host
        val targetUrl = "${HTTP_PROTOCOL}$host:${cluster.targetPort}${cluster.targetPath}"
        logger.info("Target URL for cluster $cluster is $targetUrl")
        return targetUrl
    }
}
