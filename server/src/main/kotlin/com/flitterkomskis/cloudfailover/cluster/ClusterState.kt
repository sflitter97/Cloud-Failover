package com.flitterkomskis.cloudfailover.cluster

/**
 * Enum for the different states a cluster can be in.
 */
enum class ClusterState {
    NO_INSTANCES, OPERATIONAL, TRANSITIONING, FAILED, UNKNOWN
}
