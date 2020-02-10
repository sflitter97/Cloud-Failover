package com.flitterkomskis.cloudfailover.cluster

import com.github.fakemongo.junit.FongoRule
import com.mongodb.internal.connection.tlschannel.util.Util.assertTrue
import java.util.UUID
import jdk.jfr.Category
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@Category("Unit")
class ClusterServiceTests {
    private val logger: Logger = LoggerFactory.getLogger(ClusterServiceTests::class.java)
    @get:Rule val fongoRule = FongoRule()
    @Autowired private lateinit var clusterService: ClusterService

    @Test
    fun contextLoads() {
        // Empty function to make sure context loads
    }

    @Test
    fun listClusters() {
        val clusterName = "Test cluster 1"
        assertThat(clusterService.listClusters().isEmpty())
        clusterService.createCluster(clusterName)
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
        val cluster = clusterService.createCluster(clusterName)
        assertThat(clusterService.getCluster(cluster.id).name).isEqualTo(clusterName)
    }

    @Test
    fun updateCluster() {
        var clusterName = "Test cluster 1"
        val port = 80
        val path = "/"
        var cluster = clusterService.createCluster(clusterName)
        clusterName = "Test cluster 2"
        cluster = clusterService.updateCluster(cluster.id, mapOf<String, Any>("name" to clusterName, "targetPort" to port, "targetPath" to path))
        assertThat(clusterService.getCluster(cluster.id).name).isEqualTo(clusterName)
        assertThat(clusterService.getCluster(cluster.id).targetPort).isEqualTo(port)
        assertThat(clusterService.getCluster(cluster.id).targetPath).isEqualTo(path)
    }

    @Test
    fun deleteCluster() {
        val clusterName = "Test cluster 1"
        val cluster = clusterService.createCluster(clusterName)
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