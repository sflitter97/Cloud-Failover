package com.flitterkomskis.cloudfailover.cloudproviders

/**
 * Enum for the different states in instance can be in.
 */
enum class InstanceState {
    STOPPED,
    RUNNING,
    PENDING,
    STOPPING,
    DELETED,
    DELETING,
    UNKNOWN,
    PROVISIONING,
    STAGING,
    REPAIRING,
    TERMINATED,
    DEALLOCATED,
    DEALLOCATING,
    STARTING
}
