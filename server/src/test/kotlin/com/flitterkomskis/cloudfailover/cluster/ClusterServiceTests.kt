package com.flitterkomskis.cloudfailover.cluster

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceHandle
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceInfo
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceState
import com.flitterkomskis.cloudfailover.cloudproviders.Provider
import com.flitterkomskis.cloudfailover.cloudproviders.ServiceProvider
import com.flitterkomskis.cloudfailover.cloudproviders.ServiceProviderException
import com.flitterkomskis.cloudfailover.reverseproxy.DynamicRoutingService
import com.mongodb.internal.connection.tlschannel.util.Util.assertTrue
import java.util.UUID
import jdk.jfr.Category
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@ExtendWith(MockitoExtension::class)
@SpringBootTest
@Category("Unit")
@ActiveProfiles("test")
class ClusterServiceTests {
    private val logger: Logger = LoggerFactory.getLogger(ClusterServiceTests::class.java)
    @InjectMocks
    @Autowired private lateinit var clusterService: ClusterServiceImpl

    @Mock
    private lateinit var routingService: DynamicRoutingService
    @Mock
    private lateinit var serviceProvider: ServiceProvider

    // from https://medium.com/@elye.project/befriending-kotlin-and-mockito-1c2e7b0ef791
    private fun <T> any(): T {
        Mockito.any<T>()
        return uninitialized()
    }
    private fun <T> uninitialized(): T = null as T

    @Test
    fun contextLoads() {
        // Empty function to make sure context loads
    }

    @Test
    fun listClusters() {
        val clusterName = "Test cluster 1"
        assertThat(clusterService.listClusters().isEmpty())
        clusterService.createCluster(mapOf("name" to clusterName))
        assertThat(clusterService.listClusters().size).isEqualTo(1)
        assertThat(clusterService.listClusters()[0].name).isEqualTo(clusterName)
    }

    @Test
    fun getCluster() {
        val clusterName = "Test cluster 1"
        val id = UUID.randomUUID()
        assertThrows<NoSuchElementException> {
            clusterService.getCluster(id)
        }
        val cluster = clusterService.createCluster(mapOf("name" to clusterName))
        assertThat(clusterService.getCluster(cluster.id).name).isEqualTo(clusterName)
    }

    @Test
    fun updateCluster() {
        var clusterName = "Test cluster 1"
        val port = "80"
        val path = "/"
        var cluster = clusterService.createCluster(mapOf("name" to clusterName))
        clusterName = "Test cluster 2"

        `when`(routingService.updateDynamicRoute(any())).thenReturn(true)

        cluster = clusterService.updateCluster(cluster.id, mapOf<String, Any>("name" to clusterName, "targetPort" to port, "targetPath" to path))
        assertThat(clusterService.getCluster(cluster.id).name).isEqualTo(clusterName)
        assertThat(clusterService.getCluster(cluster.id).targetPort.toString()).isEqualTo(port)
        assertThat(clusterService.getCluster(cluster.id).targetPath).isEqualTo(path)
    }

    @Test
    fun deleteCluster() {
        val clusterName = "Test cluster 1"
        val cluster = clusterService.createCluster(mapOf("name" to clusterName))
        assertDoesNotThrow { clusterService.getCluster(cluster.id) }
        assertTrue(clusterService.deleteCluster(cluster.id))
        assertFalse(clusterService.deleteCluster(cluster.id))
    }

    @Test
    fun addResponseTime_WhenManyRequests_TakesOneEveryTwoSeconds() {
        val cluster = clusterService.createCluster(mapOf("name" to "clusterName"))
        clusterService.addResponseTime(cluster.id, 10)
        clusterService.addResponseTime(cluster.id, 10)
        clusterService.addResponseTime(cluster.id, 10)
        clusterService.addResponseTime(cluster.id, 10)
        clusterService.addResponseTime(cluster.id, 10)

        var info = clusterService.getResponseTimeInfo(cluster.id)
        logger.info(info.toString())
        assertThat(info.requestCount).isEqualTo(1)

        Thread.sleep(2000L)
        clusterService.addResponseTime(cluster.id, 10)
        info = clusterService.getResponseTimeInfo(cluster.id)
        logger.info(info.toString())
        assertThat(info.requestCount).isEqualTo(2)
    }

    @Test
    fun addResponseTime_WhenManyFlags_TransitionCluster() {
        val cluster = clusterService.createCluster(mapOf("name" to "clusterName"))
        clusterService.updateCluster(cluster.id, mapOf(
            "enableInstanceStateManagement" to "true",
            "enableHotBackup" to "true",
            "enableAutomaticPriorityAdjustment" to "true")
        )
        val handle1 = InstanceHandle("handle1", "us-east", Provider.AWS)
        val handle2 = InstanceHandle("handle2", "us-east", Provider.GCP)
        val handle3 = InstanceHandle("handle3", "us-east", Provider.AZURE)

        val instanceInfos = listOf<InstanceInfo>(
            InstanceInfo(Provider.AWS, "name", "type1", InstanceState.STOPPED, handle1, "aws-host"),
            InstanceInfo(Provider.GCP, "name", "type1", InstanceState.STOPPED, handle2, "gcp-host"),
            InstanceInfo(Provider.AZURE, "name", "type1", InstanceState.STOPPED, handle3, "azure-host")
        )
        `when`(serviceProvider.getInstance(any())).then {
                invk -> instanceInfos.find { it.handle == invk.getArgument(0) } }
        `when`(serviceProvider.getInstances(any())).thenAnswer { invk ->
            instanceInfos.filter { info -> invk.getArgument<List<InstanceHandle>>(0).any { info.handle == it } }
        }
        `when`(serviceProvider.startInstance(any())).thenAnswer { invk ->
            instanceInfos.find { it.handle == invk.getArgument<InstanceHandle>(0) }?.state = InstanceState.RUNNING
            true
        }
        `when`(serviceProvider.stopInstance(any())).thenAnswer { invk ->
            instanceInfos.find { it.handle == invk.getArgument<InstanceHandle>(0) }?.state = InstanceState.STOPPED
            true
        }
        `when`(serviceProvider.waitForState(any(), any(), anyInt())).thenAnswer { invk ->
            val info = instanceInfos.find { it.handle == invk.getArgument<InstanceHandle>(0) }
            if (info == null || info.state != invk.getArgument<InstanceState>(1))
                throw ServiceProviderException("Mismatching state")
            true
        }

        clusterService.addInstance(cluster.id, handle1)
        Thread.sleep(100L)
        clusterService.addInstance(cluster.id, handle2)
        Thread.sleep(100L)
        clusterService.addInstance(cluster.id, handle3)
        Thread.sleep(100L)

        verify(serviceProvider).startInstance(handle1)

        // modify for faster testing
        clusterService.minRequestCount = 0
        clusterService.addResponseTimeInterval = 0L
        clusterService.transitionInterval = 0L

        clusterService.addResponseTime(cluster.id, 10)
        Thread.sleep(100L)
        clusterService.addResponseTime(cluster.id, 50)
        Thread.sleep(100L)
        clusterService.addResponseTime(cluster.id, 250)
        Thread.sleep(100L)
        clusterService.addResponseTime(cluster.id, 1250)
        Thread.sleep(100L)
        clusterService.addResponseTime(cluster.id, 6250)
        Thread.sleep(100L)
        clusterService.addResponseTime(cluster.id, 31250)

        // empty update to ensure all previous actions are completed
        Thread.sleep(100L)
        logger.info(instanceInfos.toString())

        verify(serviceProvider).stopInstance(handle1)
        verify(serviceProvider).startInstance(handle2)
        verify(serviceProvider).startInstance(handle3)
    }
}
