package com.flitterkomskis.cloudfailover.cloudproviders

import com.flitterkomskis.cloudfailover.cloudproviders.awsserviceprovider.AwsInstanceHandle
import com.flitterkomskis.cloudfailover.cloudproviders.awsserviceprovider.AwsServiceProvider

class ServiceProvider {
    private val AWS_NOT_INITIALIZED_MESSAGE = "AWS not initialized."
    var awsProvider: AwsServiceProvider? = null

    fun initAws(accessKey: String, secretKey: String) {
        awsProvider = AwsServiceProvider(accessKey, secretKey)
    }

    fun listInstances(): List<InstanceInfo> {
        val instances = mutableListOf<InstanceInfo>()
        instances += awsProvider?.listInstances() ?: mutableListOf()
        return instances
    }

    fun createInstance(provider: Provider, name: String, type: String, imageId: String, region: String): InstanceHandle {
        when (provider) {
            Provider.AWS -> return awsProvider?.createInstance(name, type, imageId, region) ?: throw ServiceProviderException(AWS_NOT_INITIALIZED_MESSAGE)
            else -> throw ServiceProviderException("Invalid service provider.")
        }
    }

    fun deleteInstance(handle: InstanceHandle): Boolean {
        return handle.acceptDeleteInstance(this)
    }

    fun deleteInstance(handle: AwsInstanceHandle): Boolean {
        return awsProvider?.deleteInstance(handle) ?: throw ServiceProviderException(AWS_NOT_INITIALIZED_MESSAGE)
    }

    fun startInstance(handle: InstanceHandle): Boolean {
        return handle.acceptStartInstance(this)
    }

    fun startInstance(handle: AwsInstanceHandle): Boolean {
        return awsProvider?.startInstance(handle) ?: throw ServiceProviderException(AWS_NOT_INITIALIZED_MESSAGE)
    }

    fun stopInstance(handle: InstanceHandle): Boolean {
        return handle.acceptStopInstance(this)
    }

    fun stopInstance(handle: AwsInstanceHandle): Boolean {
        return awsProvider?.stopInstance(handle) ?: throw ServiceProviderException(AWS_NOT_INITIALIZED_MESSAGE)
    }

    fun waitForState(handle: InstanceHandle, state: InstanceState, timeout: Int = 300): Boolean {
        return handle.acceptWaitForState(this, state, timeout)
    }

    fun waitForState(handle: AwsInstanceHandle, state: InstanceState, timeout: Int): Boolean {
        return awsProvider?.waitForState(handle, state, timeout) ?: throw ServiceProviderException(AWS_NOT_INITIALIZED_MESSAGE)
    }
}
