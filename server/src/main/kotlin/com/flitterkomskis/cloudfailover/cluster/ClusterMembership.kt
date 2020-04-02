package com.flitterkomskis.cloudfailover.cluster

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceHandle

/**
 * Represents the a membership of an instance to a cluster.
 * @property handle The handle uniquely identifying the instance.
 * @property priority The priority of the instance.
 */
data class ClusterMembership(
    val handle: InstanceHandle,
    var priority: Int
)
