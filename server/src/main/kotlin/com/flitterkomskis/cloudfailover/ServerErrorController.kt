package com.flitterkomskis.cloudfailover

import javax.servlet.http.HttpServletRequest
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController
import org.springframework.boot.web.servlet.error.ErrorAttributes
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

// based on https://thepracticaldeveloper.com/2019/09/09/custom-error-handling-rest-controllers-spring-boot/
@Controller
class ServerErrorController(e: ErrorAttributes) : AbstractErrorController(e) {
    override fun getErrorPath(): String {
        return "/error"
    }

    @RequestMapping("/error")
    fun handleError(request: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        val body: Map<String, Any> = this.getErrorAttributes(request, false)
        val status: HttpStatus = this.getStatus(request)
        return ResponseEntity<Map<String, Any>>(body, status)
    }
}
