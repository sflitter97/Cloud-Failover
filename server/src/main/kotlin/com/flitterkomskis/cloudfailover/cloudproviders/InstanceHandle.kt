package com.flitterkomskis.cloudfailover.cloudproviders

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.flitterkomskis.cloudfailover.cloudproviders.awsserviceprovider.AwsInstanceHandle
import com.flitterkomskis.cloudfailover.cloudproviders.azureserviceprovider.AzureInstanceHandle
import com.flitterkomskis.cloudfailover.cloudproviders.gcpserviceprovider.GcpInstanceHandle

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "handleType")
interface InstanceHandle {
    fun acceptDeleteInstance(provider: ServiceProvider): Boolean
    fun acceptStartInstance(provider: ServiceProvider): Boolean
    fun acceptStopInstance(provider: ServiceProvider): Boolean
    fun acceptWaitForState(provider: ServiceProvider, state: InstanceState, timeout: Int): Boolean
    fun acceptGetInstance(provider: ServiceProvider): InstanceInfo
}
