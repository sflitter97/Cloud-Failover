package com.flitterkomskis.cloudfailover.cloudproviders

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.flitterkomskis.cloudfailover.cloudproviders.awsserviceprovider.AwsInstanceHandle
import com.flitterkomskis.cloudfailover.cloudproviders.azureserviceprovider.AzureInstanceHandle
import com.flitterkomskis.cloudfailover.cloudproviders.gcpserviceprovider.GcpInstanceHandle

/**
 * Interface for all handle implementations. This class will be implemented for each provider specific handle. A handle
 * contains all relevant information for an instance that can uniquely identify it within that provider. For example,
 * in AWS the region and instance ID are both needed to uniquely identify an instance.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "handleType")
@JsonSubTypes(
    JsonSubTypes.Type(name = "AwsInstanceHandle", value = AwsInstanceHandle::class),
    JsonSubTypes.Type(name = "GcpInstanceHandle", value = GcpInstanceHandle::class),
    JsonSubTypes.Type(name = "AzureInstanceHandle", value = AzureInstanceHandle::class)
)
interface InstanceHandle {
    /**
     * Overridden in implementing classes to ensure the proper service provider is called without needed cases or
     * type checking.
     */
    fun acceptDeleteInstance(provider: ServiceProvider): Boolean
    /**
     * Overridden in implementing classes to ensure the proper service provider is called without needed cases or
     * type checking.
     */
    fun acceptStartInstance(provider: ServiceProvider): Boolean
    /**
     * Overridden in implementing classes to ensure the proper service provider is called without needed cases or
     * type checking.
     */
    fun acceptStopInstance(provider: ServiceProvider): Boolean
    /**
     * Overridden in implementing classes to ensure the proper service provider is called without needed cases or
     * type checking.
     */
    fun acceptWaitForState(provider: ServiceProvider, state: InstanceState, timeout: Int): Boolean
    /**
     * Overridden in implementing classes to ensure the proper service provider is called without needed cases or
     * type checking.
     */
    fun acceptGetInstance(provider: ServiceProvider): InstanceInfo
}
