package com.flitterkomskis.cloudfailover.cloudproviders

/**
 * Helper class used to encapsulate the data involved in creating an instance.
 * @property provider The cloud provider on which the instance will be created.
 * @property name The name of the instance.
 * @property type The type / size of the instance.
 * @property imageId The image with which the instance will be created. Allows for creating preconfigured instances.
 * @property region The region in which to create the instance.
 */
class CreateInstanceRequest(
    val provider: Provider,
    val name: String,
    val type: String,
    val imageId: String,
    val region: String
) {
    override fun toString(): String {
        return "CreateInstanceRequest(provider=$provider, name=$name, type=$type, imageId=$imageId, region=$region)"
    }
}
