package com.flitterkomskis.cloudfailover.cloudproviders.gcpserviceprovider

import com.fasterxml.jackson.annotation.JsonTypeName
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceHandle
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceInfo
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceState
import com.flitterkomskis.cloudfailover.cloudproviders.ServiceProvider
import org.springframework.data.annotation.TypeAlias

/**
 * Implementation of [InstanceHandle] for instances within GCP.
 * @property instanceId GCP given instance id.
 * @property region The region in which the instance is located.
 */
@TypeAlias("GcpInstanceHandle")
@JsonTypeName("GcpInstanceHandle")
class GcpInstanceHandle(val instanceId: String, val region: String) : InstanceHandle {
    override fun acceptDeleteInstance(provider: ServiceProvider): Boolean {
        return provider.deleteInstance(this)
    }

    override fun acceptStartInstance(provider: ServiceProvider): Boolean {
        return provider.startInstance(this)
    }

    override fun acceptStopInstance(provider: ServiceProvider): Boolean {
        return provider.stopInstance(this)
    }

    override fun acceptWaitForState(provider: ServiceProvider, state: InstanceState, timeout: Int): Boolean {
        return provider.waitForState(this, state, timeout)
    }

    override fun acceptGetInstance(provider: ServiceProvider): InstanceInfo {
        return provider.getInstance(this)
    }
}
