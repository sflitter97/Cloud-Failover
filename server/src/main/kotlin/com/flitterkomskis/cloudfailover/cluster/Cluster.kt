package com.flitterkomskis.cloudfailover.cluster

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceHandle
import com.flitterkomskis.cloudfailover.cloudproviders.ServiceProvider
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias

/**
 * Represents a logical grouping of instances.
 * @property name User given name for the cluster.
 */
@TypeAlias("cluster")
class Cluster(var name: String = "") {

    /**
     * Randomly generated ID to uniquely identify a cluster.
     */
    @Id var id: UUID = UUID.randomUUID()

    /**
     * List of cluster memberships to which this cluster can direct requests.
     */
    var instances: MutableList<ClusterMembership> = mutableListOf()

    /**
     * The instance to which the cluster is currently directing requests.
     */
    var accessInstance: InstanceHandle? = null
        set(handle) {
        if (handle != null && instances.none { it.handle == handle }) {
            throw Exception("Handle not found in instances.")
        }
        field = handle
    }

    /**
     * The instance to which the cluster is currently directing requests.
     */
    var backupInstance: InstanceHandle? = null
        set(handle) {
            if (handle != null && instances.none { it.handle == handle }) {
                throw Exception("Handle not found in instances.")
            }
            field = handle
        }

    /**
     * The port to which requests will be forwarded on the end host
     */
    var targetPort: Int = 0

    /**
     * Path that will be prefixed to requests before forwarding.
     * e.g. if targetPath = "/a", then a request to <cluster url>/b will be forwarded to <end host>/a/b
     */
    var targetPath: String = ""

    /**
     * Toggle for whether to manage the instance states for this cluster. When True, will automatically request
     * the [ServiceProvider] start the primary access instance (and backup if enableHotBackup is true) and stop all
     * other clusters.
     */
    var enableInstanceStateManagement: Boolean = false

    /**
     * Toggle for whether to start the backup access instance. Allows for faster cluster transitions at the expense
     * of being billed for more cloud resources. This toggle is only valid when enableInstanceStateManagement is true.
     */
    var enableHotBackup: Boolean = false

    /**
     * Toggle for whether instances will automatically have their priorities updated when cluster transitions occur.
     * When false, the instance priorities will only change when the user updates them manually.
     */
    var enableAutomaticPriorityAdjustment: Boolean = false

    /**
     * The state of the cluster.
     */
    var state: ClusterState = ClusterState.OPERATIONAL

    /**
     * Adds an instance to the cluster.
     * @param handle The instance to add to the cluster.
     */
    fun addInstance(handle: InstanceHandle) {
        if (instances.none { it.handle == handle }) {
            instances.add(ClusterMembership(handle, 1))
        }
    }

    /**
     * Removes an instances from the cluster. Does nothing if the instance is not in the cluster.
     * @param handle The instance to remove from the cluster.
     */
    fun removeInstance(handle: InstanceHandle) {
            instances.removeIf { it.handle == handle }
    }

    /**
     * Returns true if the cluster has a membership with the given handle and false otherwise
     * @param handle The instance to check for membership
     * @return True if the instance belongs to the cluster and false otherwise
     */
    fun hasInstance(handle: InstanceHandle): Boolean {
        return instances.any { it.handle == handle }
    }
}
