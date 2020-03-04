package com.flitterkomskis.cloudfailover.cloudproviders

import com.flitterkomskis.cloudfailover.cloudproviders.awsserviceprovider.AwsInstanceHandle
import com.flitterkomskis.cloudfailover.cloudproviders.awsserviceprovider.AwsServiceProvider
import com.flitterkomskis.cloudfailover.cloudproviders.azureserviceprovider.AzureInstanceHandle
import com.flitterkomskis.cloudfailover.cloudproviders.azureserviceprovider.AzureServiceProvider
import com.flitterkomskis.cloudfailover.cloudproviders.gcpserviceprovider.GcpInstanceHandle
import com.flitterkomskis.cloudfailover.cloudproviders.gcpserviceprovider.GcpServiceProvider
import javax.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Service
class ServiceProvider {
    private val logger: Logger = LoggerFactory.getLogger(ServiceProvider::class.java)
    private val AWS_NOT_INITIALIZED_MESSAGE = "AWS not initialized."
    private val GCP_NOT_INITIALIZED_MESSAGE = "GCP not initialized."
    private val AZURE_NOT_INITIALIZED_MESSAGE = "AZURE not initialized."
    private var awsProvider: AwsServiceProvider? = null
    private var gcpProvider: GcpServiceProvider? = null
    private var azureProvider: AzureServiceProvider? = null

    @PostConstruct
    fun initAws() {
        initAws(System.getenv("AWS_ACCESS_KEY"), System.getenv("AWS_SECRET_KEY"))
    }

    fun initAws(accessKey: String, secretKey: String) {
        awsProvider = AwsServiceProvider(accessKey, secretKey)
        logger.info("AWS initialized")
    }

    fun initGcp(projectId: String) {
        gcpProvider = GcpServiceProvider(projectId)
        logger.info("GCP initialized")
    }

    fun initAzure() {
        azureProvider = AzureServiceProvider()
        logger.info("AZURE initialized")
    }

    fun listInstances(): List<InstanceInfo> {
        val instances = mutableListOf<InstanceInfo>()
        instances += awsProvider?.listInstances() ?: mutableListOf()
        instances += gcpProvider?.listInstances() ?: mutableListOf()
        instances += azureProvider?.listInstances() ?: mutableListOf()
        return instances
    }

    fun getInstance(handle: InstanceHandle): InstanceInfo {
        return handle.acceptGetInstance(this)
    }

    fun getInstance(handle: AwsInstanceHandle): InstanceInfo {
        return awsProvider?.getInstance(handle) ?: throw ServiceProviderException(AWS_NOT_INITIALIZED_MESSAGE)
    }

    fun getInstance(handle: GcpInstanceHandle): InstanceInfo {
        return gcpProvider?.getInstance(handle) ?: throw ServiceProviderException(GCP_NOT_INITIALIZED_MESSAGE)
    }

    fun getInstance(handle: AzureInstanceHandle): InstanceInfo {
        return azureProvider?.getInstance(handle) ?: throw ServiceProviderException(AZURE_NOT_INITIALIZED_MESSAGE)
    }

    fun createInstance(provider: Provider, name: String, type: String, imageId: String, region: String): InstanceHandle {
        return when (provider) {
            Provider.AWS -> awsProvider?.createInstance(name, type, imageId, region) ?: throw ServiceProviderException(AWS_NOT_INITIALIZED_MESSAGE)
            Provider.GCP -> gcpProvider?.createInstance(name, type, imageId, region) ?: throw ServiceProviderException(GCP_NOT_INITIALIZED_MESSAGE)
            Provider.AZURE -> azureProvider?.createInstance(name, type, imageId, region) ?: throw ServiceProviderException(AZURE_NOT_INITIALIZED_MESSAGE)
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

    fun deleteInstance(handle: AzureInstanceHandle): Boolean {
        return azureProvider?.deleteInstance(handle) ?: throw ServiceProviderException(AZURE_NOT_INITIALIZED_MESSAGE)
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

    fun startInstance(handle: AzureInstanceHandle): Boolean {
        return azureProvider?.startInstance(handle) ?: throw ServiceProviderException(AZURE_NOT_INITIALIZED_MESSAGE)
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

    fun stopInstance(handle: AzureInstanceHandle): Boolean {
        return azureProvider?.stopInstance(handle) ?: throw ServiceProviderException(AZURE_NOT_INITIALIZED_MESSAGE)
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

    fun waitForState(handle: AzureInstanceHandle, state: InstanceState, timeout: Int): Boolean {
        return azureProvider?.waitForState(handle, state, timeout) ?: throw ServiceProviderException(AZURE_NOT_INITIALIZED_MESSAGE)
    }
}
