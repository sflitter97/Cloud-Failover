package com.flitterkomskis.cloudfailover.cloudproviders

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface InstanceHandle {
    fun acceptDeleteInstance(provider: ServiceProvider): Boolean
    fun acceptStartInstance(provider: ServiceProvider): Boolean
    fun acceptStopInstance(provider: ServiceProvider): Boolean
    fun acceptWaitForState(provider: ServiceProvider, state: InstanceState, timeout: Int): Boolean
    fun acceptGetInstance(provider: ServiceProvider): InstanceInfo
}
