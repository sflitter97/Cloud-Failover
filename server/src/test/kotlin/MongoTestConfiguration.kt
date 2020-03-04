package com.flitterkomskis.cloudfailover

import com.flitterkomskis.cloudfailover.cluster.ClusterRepository
import com.mongodb.MongoClientSettings
import com.mongodb.ServerAddress
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import de.bwaldvogel.mongo.MongoServer
import de.bwaldvogel.mongo.backend.memory.MemoryBackend
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.MongoDbFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoClientDbFactory
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
@EnableMongoRepositories(basePackageClasses = [ClusterRepository::class])
@Profile("test")
class MongoTestConfiguration {
    @Bean
    fun mongoTemplate(mongoClient: MongoClient): MongoTemplate {
        return MongoTemplate(mongoDbFactory(mongoClient))
    }

    @Bean
    fun mongoDbFactory(mongoClient: MongoClient): MongoDbFactory {
        return SimpleMongoClientDbFactory(mongoClient, "test")
    }

    @Bean(destroyMethod = "shutdown")
    fun mongoServer(): MongoServer {
        val mongoServer = MongoServer(MemoryBackend())
        mongoServer.bind()
        return mongoServer
    }

    @Bean(destroyMethod = "close")
    fun mongoClient(mongoServer: MongoServer): MongoClient {
        return MongoClients.create(MongoClientSettings.builder()
            .applyToClusterSettings { builder ->
                builder.hosts(mutableListOf(ServerAddress(mongoServer.localAddress)))
            }
            .build())
    }
}
