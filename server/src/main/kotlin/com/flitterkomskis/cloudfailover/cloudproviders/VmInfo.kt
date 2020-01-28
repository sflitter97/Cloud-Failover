package com.flitterkomskis.cloudfailover.cloudproviders

class VmInfo(
        val identifier: String,
        val provider: String,
        val region: String,
        val address: String,
        val instanceType: String
)