package com.flitterkomskis.cloudfailover.cluster

import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

/**
 * Provides persistence for [Cluster]s. Automatically saves clusters to a collection in the Mongo Database. Provides
 * basic operations such as getting and saving whole cluster objects.
 */
@Repository
interface ClusterRepository : MongoRepository<Cluster, UUID>, CustomClusterRepository

/**
 * Interface for custom operations we want the [ClusterRepository] to support.
 */
interface CustomClusterRepository {
    /**
     * Atomically sets the cluster to the given state.
     * @param id ID that uniquely identifies the cluster.
     * @param state The new [ClusterState] to change the cluster to.
     * @return The cluster after the state has been changed.
     */
    fun setState(id: UUID, state: ClusterState): Cluster
}

/**
 * Implementation of the [CustomClusterRepository] interface. Spring Boot will automatically find this class and use
 * it when creating the [ClusterRepository].
 */
open class ClusterRepositoryImpl : CustomClusterRepository {
    @Autowired private lateinit var operations: MongoOperations

    /**
     * Atomically changes the state of the cluster using the findAndModify operation of the Mongo Database.
     * @param id ID that uniquely identifies the cluster.
     * @param state The new [ClusterState] to change the cluster to.
     * @return The cluster after the state has been changed.
     */
    override fun setState(id: UUID, state: ClusterState): Cluster {
        val query = Query()
        query.addCriteria(Criteria.where("_id").`is`(id))
        val update = Update()
        update.set("state", state)
        return operations.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), Cluster::class.java)
            ?: throw ClusterRepositoryException("Cluster with id $id not found.")
    }
}
