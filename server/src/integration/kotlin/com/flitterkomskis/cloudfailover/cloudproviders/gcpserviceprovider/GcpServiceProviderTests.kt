package com.flitterkomskis.cloudfailover.cloudproviders.gcpserviceprovider

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
class GcpServiceProviderTests {
    @Test
    fun contextLoads() {
        //Empty function to make sure context loads
    }

    @Test
    fun createListDeleteInstance() {
        val serviceProvider = ServiceProvider()
        serviceProvider.initGcp()

        val provider = Provider.GCP
        val name = "test-instance1"
        val type = "zones/us-west1-a/machineTypes/n1-standard-1"
        val imageId = "projects/debian-cloud/global/images/family/debian-9"
        val region = "us-west1-a"

        val handle = serviceProvider.createInstance(provider, name, type, imageId, region)
        val instances = serviceProvider.listInstances()
        val instance = instances.find { instanceInfo -> instanceInfo.provider == Provider.GCP && instanceInfo.name == name }

        Assertions.assertThat(instance).isNotNull
        Assertions.assertThat(serviceProvider.deleteInstance(handle)).isTrue()
    }

    @Test
    fun stopStartInstance() {
        val serviceProvider = ServiceProvider()
        serviceProvider.initGcp()

        val provider = Provider.GCP
        val name = "test-instance2"
        val type = "zones/us-west1-a/machineTypes/n1-standard-1"
        val imageId = "projects/debian-cloud/global/images/family/debian-9"
        val region = "us-west1-a"

        val handle = serviceProvider.createInstance(provider, name, type, imageId, region)

        Assertions.assertThat(serviceProvider.waitForState(handle, InstanceState.RUNNING))
        Assertions.assertThat(serviceProvider.stopInstance(handle)).isTrue()
        Assertions.assertThat(serviceProvider.waitForState(handle, InstanceState.TERMINATED))
        Assertions.assertThat(serviceProvider.startInstance(handle)).isTrue()
        Assertions.assertThat(serviceProvider.waitForState(handle, InstanceState.RUNNING))
        Assertions.assertThat(serviceProvider.deleteInstance(handle)).isTrue()
    }
}