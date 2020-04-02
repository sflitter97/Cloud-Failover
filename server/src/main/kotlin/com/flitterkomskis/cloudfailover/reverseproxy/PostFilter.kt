package com.flitterkomskis.cloudfailover.reverseproxy

import com.flitterkomskis.cloudfailover.cluster.ClusterService
import com.netflix.zuul.ZuulFilter
import com.netflix.zuul.context.RequestContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.POST_TYPE
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SEND_RESPONSE_FILTER_ORDER
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import javax.servlet.http.HttpServletRequest

@Component
class PostFilter : ZuulFilter() {
    private val logger: Logger = LoggerFactory.getLogger(PostFilter::class.java)
    @Autowired private lateinit var clusterService: ClusterService
    override fun filterType(): String {
        return POST_TYPE
    }

    override fun filterOrder(): Int {
        return SEND_RESPONSE_FILTER_ORDER - 1
    }

    override fun shouldFilter(): Boolean {
        return true
    }

    override fun run() {
        val stopTime = Instant.now().toEpochMilli()
        val ctx: RequestContext = RequestContext.getCurrentContext()
        val request: HttpServletRequest = ctx.request
        val startTime = ctx["startTime"] as Long
        logger.info(
            "Request URL::" + request.requestURL.toString() +
                ":: Time Taken=" + (stopTime - startTime)
        )
        val startToken = "/api/access/"
        val endToken = "/"
        val startIdx = request.requestURL.indexOf(startToken) + startToken.length
        val id: String = request.requestURL.substring(startIdx, request.requestURL.indexOf(endToken, startIdx))
        clusterService.addResponseTime(UUID.fromString(id), stopTime - startTime)
    }
}