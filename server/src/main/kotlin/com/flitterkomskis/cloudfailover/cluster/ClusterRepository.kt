package com.flitterkomskis.cloudfailover.cluster

import java.util.UUID
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ClusterRepository : MongoRepository<Cluster, UUID>
