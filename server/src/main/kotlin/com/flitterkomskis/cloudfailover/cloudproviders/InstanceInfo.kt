package com.flitterkomskis.cloudfailover.cloudproviders

import org.springframework.hateoas.server.core.Relation

/**
 * Object to encapsulate all the needed information about an instance, such as the provider, name, state, etc.
 * @property provider The cloud provider hosting the instance.
 * @property name The name of the instance.
 * @property type The type / size of the instance.
 * @property state The state of the instance. E.g. running, deleting, etc.
 * @property handle The handle that can be used to uniquely identify the instance within the server and cloud provider.
 * @property host The url to access the instance.
 */
@Relation(collectionRelation = "instances", itemRelation = "instance")
data class InstanceInfo(
    val provider: Provider,
    val name: String,
    val type: String,
    val state: InstanceState,
    val handle: InstanceHandle,
    val host: String
)
