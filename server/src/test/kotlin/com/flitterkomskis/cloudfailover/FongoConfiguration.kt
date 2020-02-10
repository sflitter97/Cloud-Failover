package com.flitterkomskis.cloudfailover

import com.github.fakemongo.Fongo
import com.mongodb.client.MongoClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration

@ComponentScan
@Profile("fongo")
class FongoConfiguration : AbstractMongoClientConfiguration() {
    @Bean
    override fun mongoClient(): MongoClient {
        return Fongo("Fongo").mongo as MongoClient
    }

    override fun getDatabaseName(): String {
        return "Cloud-Failover-MockDB"
    }
}
