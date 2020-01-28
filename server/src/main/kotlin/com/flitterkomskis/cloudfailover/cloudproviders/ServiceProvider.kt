package com.flitterkomskis.cloudfailover.cloudproviders

// Interface to provide a template for clouds
interface ServiceProvider {
    fun createVm(imageId: String, instanceType: String, region: String): Void
    fun deleteVm(identifier: String): Void
    fun startVm(identifier: String): Void
    fun stopVm(identifier: String): Void
    fun vmInfo(identifier: String): VmInfo
    fun listVms(): Array<VmInfo>
}