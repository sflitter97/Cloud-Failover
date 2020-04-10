package com.flitterkomskis.cloudfailover.cluster

import java.time.Instant
import java.util.LinkedList

/**
 * Represents the response time state of a cluster. This is used for autoswitching to determine when a cluster should
 * switch traffic from one instance to another. This is not persisted because it's okay for this information to
 * be cleared periodically.
 */
data class ClusterResponseTimeInfo(
    /**
     * The number of recorded requests.
     */
    var requestCount: Long = 0,
    /**
     * The average number of milliseconds taken for the cluster to respond to a request.
     */
    var rtt: Long = 0,
    /**
     * The variance (in milliseconds) in response times for the cluster.
     */
    var rttVar: Long = 0,
    /**
     * The timestamps of the flagged requests for the cluster.
     */
    var flags: LinkedList<Instant> = LinkedList(),
    /**
     * The last time a request was recorded for the cluster.
     */
    var lastUpdateTime: Instant = Instant.EPOCH,
    /**
     * The last time the cluster transitioned from one instance to another.
     */
    var lastTransitionTime: Instant = Instant.EPOCH
) {
    constructor(copy: ClusterResponseTimeInfo) : this(
        copy.requestCount,
        copy.rtt,
        copy.rttVar,
        copy.flags,
        copy.lastUpdateTime,
        copy.lastTransitionTime) {}
}
