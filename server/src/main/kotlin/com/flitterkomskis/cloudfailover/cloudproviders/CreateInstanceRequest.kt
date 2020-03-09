package com.flitterkomskis.cloudfailover.cloudproviders

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
