package com.flitterkomskis.cloudfailover.cloudproviders

import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "instances", itemRelation = "instance")
data class InstanceInfo(
    val provider: Provider,
    val name: String,
    val type: String,
    val state: InstanceState,
    val handle: InstanceHandle,
    val host: String
)
