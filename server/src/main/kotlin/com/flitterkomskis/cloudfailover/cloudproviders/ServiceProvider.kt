package com.flitterkomskis.cloudfailover.cloudproviders

import com.flitterkomskis.cloudfailover.cloudproviders.awsserviceprovider.AwsInstanceHandle
import com.flitterkomskis.cloudfailover.cloudproviders.awsserviceprovider.AwsServiceProvider
import com.flitterkomskis.cloudfailover.cloudproviders.gcpserviceprovider.GcpInstanceHandle
import com.flitterkomskis.cloudfailover.cloudproviders.gcpserviceprovider.GcpServiceProvider

class ServiceProvider {
    private val AWS_NOT_INITIALIZED_MESSAGE = "AWS not initialized."
    private val GCP_NOT_INITIALIZED_MESSAGE = "GCP not initialized."
    private var awsProvider: AwsServiceProvider? = null
    private var gcpProvider: GcpServiceProvider? = null

    fun initAws(accessKey: String, secretKey: String) {
        awsProvider = AwsServiceProvider(accessKey, secretKey)
    }

    fun initGcp() {
        gcpProvider = GcpServiceProvider()
    }

    fun listInstances(): List<InstanceInfo> {
        val instances = mutableListOf<InstanceInfo>()
        instances += awsProvider?.listInstances() ?: mutableListOf()
        instances += gcpProvider?.listInstances() ?: mutableListOf()
        return instances
    }

    fun createInstance(provider: Provider, name: String, type: String, imageId: String, region: String): InstanceHandle {
        return when (provider) {
            Provider.AWS -> awsProvider?.createInstance(name, type, imageId, region) ?: throw ServiceProviderException(AWS_NOT_INITIALIZED_MESSAGE)
            Provider.GCP -> gcpProvider?.createInstance(name, type, imageId, region) ?: throw ServiceProviderException(GCP_NOT_INITIALIZED_MESSAGE)
            else -> throw ServiceProviderException("Invalid service provider.")
        }
    }

    fun deleteInstance(handle: InstanceHandle): Boolean {
        return handle.acceptDeleteInstance(this)
    }

    fun deleteInstance(handle: AwsInstanceHandle): Boolean {
        return awsProvider?.deleteInstance(handle) ?: throw ServiceProviderException(AWS_NOT_INITIALIZED_MESSAGE)
    }

    fun deleteInstance(handle: GcpInstanceHandle): Boolean {
        return gcpProvider?.deleteInstance(handle) ?: throw ServiceProviderException(GCP_NOT_INITIALIZED_MESSAGE)
    }

    fun startInstance(handle: InstanceHandle): Boolean {
        return handle.acceptStartInstance(this)
    }

    fun startInstance(handle: AwsInstanceHandle): Boolean {
        return awsProvider?.startInstance(handle) ?: throw ServiceProviderException(AWS_NOT_INITIALIZED_MESSAGE)
    }

    fun startInstance(handle: GcpInstanceHandle): Boolean {
        return gcpProvider?.startInstance(handle) ?: throw ServiceProviderException(GCP_NOT_INITIALIZED_MESSAGE)
    }

    fun stopInstance(handle: InstanceHandle): Boolean {
        return handle.acceptStopInstance(this)
    }

    fun stopInstance(handle: AwsInstanceHandle): Boolean {
        return awsProvider?.stopInstance(handle) ?: throw ServiceProviderException(AWS_NOT_INITIALIZED_MESSAGE)
    }

    fun stopInstance(handle: GcpInstanceHandle): Boolean {
        return gcpProvider?.stopInstance(handle) ?: throw ServiceProviderException(GCP_NOT_INITIALIZED_MESSAGE)
    }

    fun waitForState(handle: InstanceHandle, state: InstanceState, timeout: Int = 300): Boolean {
        return handle.acceptWaitForState(this, state, timeout)
    }

    fun waitForState(handle: AwsInstanceHandle, state: InstanceState, timeout: Int): Boolean {
        return awsProvider?.waitForState(handle, state, timeout) ?: throw ServiceProviderException(AWS_NOT_INITIALIZED_MESSAGE)
    }

    fun waitForState(handle: GcpInstanceHandle, state: InstanceState, timeout: Int): Boolean {
        return gcpProvider?.waitForState(handle, state, timeout) ?: throw ServiceProviderException(GCP_NOT_INITIALIZED_MESSAGE)
    }
}
