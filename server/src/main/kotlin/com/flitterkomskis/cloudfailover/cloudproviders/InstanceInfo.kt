package com.flitterkomskis.cloudfailover.cloudproviders

data class InstanceInfo(
    val provider: Provider,
    val name: String,
    val type: String,
    val state: InstanceState,
    val handle: InstanceHandle,
    val host: String
)
