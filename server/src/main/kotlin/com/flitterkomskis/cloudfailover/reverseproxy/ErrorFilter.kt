package com.flitterkomskis.cloudfailover.reverseproxy

import com.flitterkomskis.cloudfailover.cluster.ClusterService
import com.netflix.zuul.ZuulFilter
import com.netflix.zuul.context.RequestContext
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants
import org.springframework.stereotype.Component

@Component
class ErrorFilter : ZuulFilter() {
    private val logger: Logger = LoggerFactory.getLogger(ErrorFilter::class.java)
    protected val SEND_ERROR_FILTER_RAN = "sendErrorFilter.ran"
    @Autowired
    private lateinit var clusterService: ClusterService
    override fun filterType(): String {
        return FilterConstants.ERROR_TYPE
    }

    override fun filterOrder(): Int {
        return -1
    }

    override fun shouldFilter(): Boolean {
        val ctx = RequestContext.getCurrentContext()
        return ctx.throwable != null
    }

    override fun run() {
        val ctx = RequestContext.getCurrentContext()
        // ctx.set(SEND_ERROR_FILTER_RAN)

        val request: HttpServletRequest = ctx.request
        val startToken = "/api/access/"
        val endToken = "/"
        val startIdx = request.requestURL.indexOf(startToken) + startToken.length
        val id: String = request.requestURL.substring(startIdx, request.requestURL.indexOf(endToken, startIdx))
        clusterService.flagRequest(UUID.fromString(id))
    }
}
