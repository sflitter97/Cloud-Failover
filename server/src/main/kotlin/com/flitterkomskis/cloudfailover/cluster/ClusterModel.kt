package com.flitterkomskis.cloudfailover.cluster

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceHandle
import java.util.UUID
import org.springframework.data.annotation.TypeAlias
import org.springframework.hateoas.server.core.Relation

@TypeAlias("cluster")
@Relation(collectionRelation = "clusters", itemRelation = "cluster")
data class ClusterModel(
    var name: String = "",
    var id: UUID = UUID.randomUUID(),
    var instances: List<InstanceHandle> = listOf(),
    var accessInstance: InstanceHandle? = null,
    var backupInstance: InstanceHandle? = null,
    var targetPort: Int = 0,
    var targetPath: String = "",
    var enableInstanceStateManagement: String = "false",
    var enableHotBackup: String = "false",
    var enableAutomaticPriorityAdjustment: String = "false",
    var state: ClusterState = ClusterState.NO_INSTANCES
) {
        constructor(cluster: Cluster) : this() {
                name = cluster.name
                id = cluster.id
                instances = cluster.instances.map { it -> it.handle }
                accessInstance = cluster.accessInstance
                backupInstance = cluster.backupInstance
                targetPort = cluster.targetPort
                targetPath = cluster.targetPath
                enableInstanceStateManagement = cluster.enableInstanceStateManagement.toString()
                enableHotBackup = cluster.enableHotBackup.toString()
                enableAutomaticPriorityAdjustment = cluster.enableAutomaticPriorityAdjustment.toString()
                state = cluster.state
        }
}
