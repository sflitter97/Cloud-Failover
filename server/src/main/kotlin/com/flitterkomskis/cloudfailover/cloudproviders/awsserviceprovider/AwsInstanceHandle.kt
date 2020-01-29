package com.flitterkomskis.cloudfailover.cloudproviders.awsserviceprovider

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceHandle
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceState
import com.flitterkomskis.cloudfailover.cloudproviders.ServiceProvider

class AwsInstanceHandle(val instanceId: String, val region: String) : InstanceHandle {
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
}
