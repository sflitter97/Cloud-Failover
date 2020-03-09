package com.flitterkomskis.cloudfailover.cluster

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
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
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
    fun addInstance() {
        // TODO: mock service provider
    }

    @Test
    fun removeInstance() {
        // TODO: mock service provider
    }
}
