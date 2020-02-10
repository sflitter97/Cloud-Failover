package com.flitterkomskis.cloudfailover.cloudproviders.awsserviceprovider

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceState
import com.flitterkomskis.cloudfailover.cloudproviders.Provider
import com.flitterkomskis.cloudfailover.cloudproviders.ServiceProvider
import jdk.jfr.Category
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
// @TestPropertySource(properties = arrayOf("spring.cloud.task.closecontext_enabled=false"))
@Category("Integration")
class AwsServiceProviderTests {
    private val logger: Logger = LoggerFactory.getLogger(AwsServiceProviderTests::class.java)
    private val AWS_ACCESS_KEY = System.getenv("AWS_ACCESS_KEY")
    private val AWS_SECRET_KEY = System.getenv("AWS_SECRET_KEY")

    @Test
    fun contextLoads() {
        // Empty function to make sure context loads
    }

    @Test
    fun createListDeleteInstance() {
        val serviceProvider = ServiceProvider()
        logger.info(AWS_ACCESS_KEY)
        logger.info(AWS_SECRET_KEY)
        serviceProvider.initAws(AWS_ACCESS_KEY, AWS_SECRET_KEY)

        val provider = Provider.AWS
        val name = "Cloud failover test instance"
        val type = "T2_MICRO"
        val imageId = "ami-02ccb28830b645a41"
        val region = "us-east-2"

        val handle = serviceProvider.createInstance(provider, name, type, imageId, region)

        val instances = serviceProvider.listInstances()
        val instance = instances.find { instanceInfo -> instanceInfo.provider == Provider.AWS && instanceInfo.name == name }
        assertThat(instance).isNotNull

        assertThat(serviceProvider.deleteInstance(handle)).isTrue()
    }

    @Test
    fun stopStartInstance() {
        val serviceProvider = ServiceProvider()
        serviceProvider.initAws(AWS_ACCESS_KEY, AWS_SECRET_KEY)

        val provider = Provider.AWS
        val name = "Cloud failover test instance"
        val type = "T2_MICRO"
        val imageId = "ami-02ccb28830b645a41"
        val region = "us-east-2"

        val handle = serviceProvider.createInstance(provider, name, type, imageId, region)

        assertThat(serviceProvider.waitForState(handle, InstanceState.RUNNING))
        assertThat(serviceProvider.stopInstance(handle)).isTrue()
        assertThat(serviceProvider.waitForState(handle, InstanceState.STOPPED))
        assertThat(serviceProvider.startInstance(handle)).isTrue()
        assertThat(serviceProvider.waitForState(handle, InstanceState.RUNNING))
        assertThat(serviceProvider.deleteInstance(handle)).isTrue()
    }
}
