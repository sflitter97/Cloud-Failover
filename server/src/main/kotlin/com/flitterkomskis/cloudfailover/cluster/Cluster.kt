package com.flitterkomskis.cloudfailover.cluster

import com.fasterxml.jackson.annotation.JsonProperty
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceHandle
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.hateoas.RepresentationModel

@TypeAlias("cluster")
open class Cluster(var name: String): RepresentationModel<Cluster>() {

    @Id var id = UUID.randomUUID()

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var instances: MutableList<InstanceHandle> = mutableListOf()

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
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
