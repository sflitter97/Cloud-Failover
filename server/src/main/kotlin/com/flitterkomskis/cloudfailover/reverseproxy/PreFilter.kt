package com.flitterkomskis.cloudfailover.reverseproxy

import com.netflix.zuul.ZuulFilter
import com.netflix.zuul.context.RequestContext
import javax.servlet.http.HttpServletRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Filter for the Zuul proxy before requests are forwarded. Logs requests as they are made for
 * debugging purposes and attaches the start time of the request to it for measuring response times. Based on
 * https://stackoverflow.com/questions/42857658/how-to-log-time-taken-by-rest-web-service-in-spring-boot/42859535
 */
@Component
class PreFilter : ZuulFilter() {
    private val logger: Logger = LoggerFactory.getLogger(PreFilter::class.java)
    override fun filterType(): String {
        return PRE_TYPE
    }

    override fun filterOrder(): Int {
        return PRE_DECORATION_FILTER_ORDER - 1
    }

    override fun shouldFilter(): Boolean {
        val ctx: RequestContext = RequestContext.getCurrentContext()
        val requestURL: String = ctx.request.requestURL.toString()
        return requestURL.contains("/api/access/")
    }

    override fun run() {
        val ctx: RequestContext = RequestContext.getCurrentContext()
        val request: HttpServletRequest = ctx.request
        val startTime: Long = Instant.now().toEpochMilli()
        logger.info("PreFilter: ${request.method} to ${request.requestURL}")
        logger.info("Request URL::" + request.requestURL.toString() +
            ":: Start Time=" + Instant.now())
        ctx["startTime"] = startTime
    }
}
