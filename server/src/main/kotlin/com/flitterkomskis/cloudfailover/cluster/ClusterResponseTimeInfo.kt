package com.flitterkomskis.cloudfailover.cluster

import java.time.Instant
import java.util.LinkedList

data class ClusterResponseTimeInfo(
    var requestCount: Long = 0,
    var rtt: Long = 0,
    var rttVar: Long = 0,
    var flags: LinkedList<Instant> = LinkedList(),
    var lastUpdateTime: Instant = Instant.EPOCH,
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
