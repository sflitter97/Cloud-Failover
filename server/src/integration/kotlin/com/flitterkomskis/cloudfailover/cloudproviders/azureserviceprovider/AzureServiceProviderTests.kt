package com.flitterkomskis.cloudfailover.cloudproviders.azureserviceprovider

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceState
import com.flitterkomskis.cloudfailover.cloudproviders.Provider
import com.flitterkomskis.cloudfailover.cloudproviders.ServiceProvider
import jdk.jfr.Category
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(properties = arrayOf("spring.cloud.task.closecontext_enabled=false"))
@Category("Integration")
class AzureServiceProviderTests {
    @Test
    fun contextLoads() {
        // Empty function to make sure context loads
    }

    @Test
    fun createListDeleteInstance() {
        val serviceProvider = ServiceProvider()
        serviceProvider.initAzure()

        val provider = Provider.AZURE
        val name = "test-instance1"
        val type = "Standard_B1ls"
        val imageId = "NULL"
        val region = "Central US"

        val handle = serviceProvider.createInstance(provider, name, type, imageId, region)
        val instances = serviceProvider.listInstances()
        val instance = instances.find { instanceInfo -> instanceInfo.provider == Provider.AZURE && instanceInfo.name == name }

        Assertions.assertThat(instance).isNotNull
        Assertions.assertThat(serviceProvider.deleteInstance(handle)).isTrue()
    }

    @Test
    fun stopStartInstance() {
        val serviceProvider = ServiceProvider()
        serviceProvider.initAzure()

        val provider = Provider.AZURE
        val name = "test-instance2"
        val type = "Standard_B1ls"
        val imageId = "NULL"
        val region = "Central US"

        val handle = serviceProvider.createInstance(provider, name, type, imageId, region)

        Assertions.assertThat(serviceProvider.waitForState(handle, InstanceState.RUNNING))
        Assertions.assertThat(serviceProvider.stopInstance(handle)).isTrue()
        Assertions.assertThat(serviceProvider.waitForState(handle, InstanceState.TERMINATED))
        Assertions.assertThat(serviceProvider.startInstance(handle)).isTrue()
        Assertions.assertThat(serviceProvider.waitForState(handle, InstanceState.RUNNING))
        Assertions.assertThat(serviceProvider.deleteInstance(handle)).isTrue()
    }
}
