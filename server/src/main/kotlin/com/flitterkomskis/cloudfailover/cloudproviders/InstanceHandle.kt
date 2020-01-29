package com.flitterkomskis.cloudfailover.cloudproviders

interface InstanceHandle {
    fun acceptDeleteInstance(provider: ServiceProvider): Boolean
    fun acceptStartInstance(provider: ServiceProvider): Boolean
    fun acceptStopInstance(provider: ServiceProvider): Boolean
    fun acceptWaitForState(provider: ServiceProvider, state: InstanceState, timeout: Int): Boolean
}
