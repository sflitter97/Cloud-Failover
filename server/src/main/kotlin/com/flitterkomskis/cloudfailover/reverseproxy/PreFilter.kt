package com.flitterkomskis.cloudfailover.reverseproxy

import com.netflix.zuul.ZuulFilter
import com.netflix.zuul.context.RequestContext
import javax.servlet.http.HttpServletRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.util.UrlPathHelper

@Component
class PreFilter : ZuulFilter() {
    private val urlPathHelper = UrlPathHelper()
    override fun filterType(): String {
        return "pre"
    }

    override fun filterOrder(): Int {
        return 1
    }

    override fun shouldFilter(): Boolean {
        val ctx: RequestContext = RequestContext.getCurrentContext()
        val requestURL: String = ctx.getRequest().getRequestURL().toString()
        return !(requestURL.contains("proxyurl") || requestURL.contains("/admin/"))
    }

    override fun run(): Any? {
        val ctx: RequestContext = RequestContext.getCurrentContext()
        val request: HttpServletRequest = ctx.getRequest()
        log.info(
            "PreFilter: " + String.format(
                "%s request to %s",
                request.method,
                request.requestURL.toString()
            )
        )
        log.info(request.toString())
        // if(!request.requestURL.toString().endsWith('/'))
        return null
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PreFilter::class.java)
    }
}
