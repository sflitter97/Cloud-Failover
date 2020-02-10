package com.flitterkomskis.cloudfailover.cluster

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceHandle
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias

@TypeAlias("cluster")
class Cluster(var name: String) {

    @Id var id = UUID.randomUUID()

    var instances: MutableList<InstanceHandle> = mutableListOf()

    var accessInstance: InstanceHandle? = null
        set(handle) {
        if (!instances.contains(handle)) {
            throw Exception("Handle not found in instances.")
        }
        field = handle
    }

    var targetPort: Int = 0

    var targetPath: String = ""

    fun addInstance(handle: InstanceHandle) {
        if (!instances.contains(handle)) {
            instances.add(handle)
        }
    }

    fun removeInstance(handle: InstanceHandle) {
        if (instances.contains(handle)) {
            instances.remove(handle)
        }
    }
}
