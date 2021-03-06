package com.flitterkomskis.cloudfailover

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(properties = arrayOf("spring.cloud.task.closecontext_enabled=false"))
@ActiveProfiles("test")
class CloudFailoverApplicationTests {
    @Test
    fun contextLoads() {
    }
}
