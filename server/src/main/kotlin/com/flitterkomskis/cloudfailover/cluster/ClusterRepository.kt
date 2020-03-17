package com.flitterkomskis.cloudfailover.cluster

import java.util.UUID
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

/**
 * Provides persistence for [Cluster]s. Automatically saves clusters to a collection named "Clusters" in the Mongo
 * Database.
 */
@Repository
interface ClusterRepository : MongoRepository<Cluster, UUID>
