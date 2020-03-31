package com.flitterkomskis.cloudfailover.cloudproviders

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Interface for all handle implementations. This class will be implemented for each provider specific handle. A handle
 * contains all relevant information for an instance that can uniquely identify it within that provider. For example,
 * in AWS the region and instance ID are both needed to uniquely identify an instance.
 */
data class InstanceHandle(val instanceId: String, val region: String, val provider: Provider) {}
