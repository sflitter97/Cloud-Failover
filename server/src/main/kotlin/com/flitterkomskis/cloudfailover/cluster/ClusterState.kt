package com.flitterkomskis.cloudfailover.cluster

enum class ClusterState {
    NO_INSTANCES, OPERATIONAL, TRANSITIONING, FAILED, UNKNOWN
}
