package com.flitterkomskis.cloudfailover.cluster

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * Provides persistence for [Cluster]s. Automatically saves clusters to a collection named "Clusters" in the Mongo
 * Database.
 */
@Repository
interface ClusterRepository : MongoRepository<Cluster, UUID>, CustomClusterRepository

interface CustomClusterRepository {
    fun setState(id: UUID, state: ClusterState): Cluster
}

open class ClusterRepositoryImpl : CustomClusterRepository {
    private var logger: Logger = LoggerFactory.getLogger(ClusterRepositoryImpl::class.java)
    @Autowired private lateinit var operations: MongoOperations

    override fun setState(id: UUID, state: ClusterState): Cluster {
        val query = Query()
        query.addCriteria(Criteria.where("_id").`is`(id))
        val update = Update()
        update.set("state", state)
        return operations.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), Cluster::class.java)
            ?: throw ClusterRepositoryException("Cluster with id $id not found.")
    }
}
