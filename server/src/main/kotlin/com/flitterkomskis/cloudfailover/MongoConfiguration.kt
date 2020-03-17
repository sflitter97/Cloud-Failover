package com.flitterkomskis.cloudfailover

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration

/**
 * Sets up the connection to the Mongo database in a production / development environment (not testing).
 * If the MULTICLOUD_FAILOVER_MONGO_CONN_STR envrionmental variable is present, it will be used as the connection string
 * to the server. Otherwise, it will attempt to connect to a MongoDB running on the local machine on the default port
 * (27017).
 */
@Configuration
@Profile("production")
class MongoConfiguration : AbstractMongoClientConfiguration() {
    private val CONN_STR_ENV_VAR = "MULTICLOUD_FAILOVER_MONGO_CONN_STR"

    override fun getDatabaseName(): String {
        return "Multicloud-Failover"
    }

    override fun mongoClient(): MongoClient {
        // If environment parameters not present, use local database
        if (!System.getenv().containsKey(CONN_STR_ENV_VAR)) {
            return MongoClients.create()
        }

        val settings = MongoClientSettings.builder()
            .applyToSslSettings { builder -> builder.enabled(true) }
            .applyConnectionString(ConnectionString(System.getenv(CONN_STR_ENV_VAR)))
            .build()
        return MongoClients.create(settings)
    }
}
