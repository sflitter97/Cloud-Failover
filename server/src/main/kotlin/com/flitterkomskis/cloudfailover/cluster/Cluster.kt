package com.flitterkomskis.cloudfailover.cluster

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceHandle
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
    @Id var id = UUID.randomUUID()

    /**
     * List of instances to which this cluster can direct requests.
     */
    var instances: MutableList<InstanceHandle> = mutableListOf()

    /**
     * The instance to which the cluster is currently directing requests.
     */
    var accessInstance: InstanceHandle? = null
        set(handle) {
        if (!instances.contains(handle)) {
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
     * Adds an instance to the cluster.
     * @param handle The instance to add to the cluster.
     */
    fun addInstance(handle: InstanceHandle) {
        if (!instances.contains(handle)) {
            instances.add(handle)
        }
    }

    /**
     * Removes an instances from the cluster. Does nothing if the instance is not in the cluster.
     * @param handle The instance to remove from the cluster.
     */
    fun removeInstance(handle: InstanceHandle) {
        if (instances.contains(handle)) {
            instances.remove(handle)
        }
    }
}
