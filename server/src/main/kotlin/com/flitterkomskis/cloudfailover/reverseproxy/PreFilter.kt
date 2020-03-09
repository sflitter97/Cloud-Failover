package com.flitterkomskis.cloudfailover.reverseproxy

import com.netflix.zuul.ZuulFilter
import com.netflix.zuul.context.RequestContext
import javax.servlet.http.HttpServletRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PreFilter : ZuulFilter() {
    private val logger: Logger = LoggerFactory.getLogger(PreFilter::class.java)
    override fun filterType(): String {
        return "pre"
    }

    override fun filterOrder(): Int {
        return 1
    }

    override fun shouldFilter(): Boolean {
        val ctx: RequestContext = RequestContext.getCurrentContext()
        val requestURL: String = ctx.request.requestURL.toString()
        return requestURL.contains("/api/access/")
    }

    override fun run(): Any? {
        val ctx: RequestContext = RequestContext.getCurrentContext()
        val request: HttpServletRequest = ctx.request
        logger.info("PreFilter: ${request.method} to ${request.requestURL}")
        // if(!request.requestURL.toString().endsWith('/'))
        return null
    }
}
