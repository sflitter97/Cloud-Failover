package com.flitterkomskis.cloudfailover.cloudproviders

/**
 * Represents a handle to an instance on a provider. A handle contains all relevant information for an instance that
 * can uniquely identify it within that provider. For example, in AWS the region and instance ID are both needed to
 * uniquely identify an instance.
 */
data class InstanceHandle(val instanceId: String, val region: String, val provider: Provider)
