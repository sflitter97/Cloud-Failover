package com.flitterkomskis.cloudfailover.cloudproviders

import com.flitterkomskis.cloudfailover.cloudproviders.awsserviceprovider.AwsServiceProvider
import com.flitterkomskis.cloudfailover.cloudproviders.azureserviceprovider.AzureServiceProvider
import com.flitterkomskis.cloudfailover.cloudproviders.gcpserviceprovider.GcpServiceProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Facade / service layer between the instance management API and the cloud providers. Provides a generalized single
 * point of access for manipulating instances.
 */
@Service
class ServiceProvider() {
    private val logger: Logger = LoggerFactory.getLogger(ServiceProvider::class.java)
    private val AWS_NOT_INITIALIZED_MESSAGE = "AWS not initialized."
    private val GCP_NOT_INITIALIZED_MESSAGE = "GCP not initialized."
    private val AZURE_NOT_INITIALIZED_MESSAGE = "AZURE not initialized."
    private var awsProvider: AwsServiceProvider? = null
    private var gcpProvider: GcpServiceProvider? = null
    private var azureProvider: AzureServiceProvider? = null

    init {
        initAws(System.getenv("AWS_ACCESS_KEY"), System.getenv("AWS_SECRET_KEY"))
        initGcp(System.getenv("GCP_PROJECT"))
        initAzure()
    }

    final fun initAws(accessKey: String, secretKey: String) {
        awsProvider = AwsServiceProvider(accessKey, secretKey)
        logger.info("AWS initialized")
    }

    final fun initGcp(projectId: String) {
        gcpProvider = GcpServiceProvider(projectId)
        logger.info("GCP initialized")
    }

    final fun initAzure() {
        azureProvider = AzureServiceProvider()
        logger.info("AZURE initialized")
    }

    /**
     * Lists all instances from all providers
     * @return A list of [InstanceInfo]s describing the instances.
     */
    fun listInstances(): List<InstanceInfo> {
        var instances = listOf<InstanceInfo>()
        val awsInstances = GlobalScope.async {
            logger.info("Getting instances from AWS")
            val partInstances = awsProvider?.listInstances() ?: mutableListOf()
            logger.info("Got ${partInstances.size} instances from AWS")
            partInstances
        }
        val gcpInstances = GlobalScope.async {
            logger.info("Getting instances from GCP")
            val partInstances = gcpProvider?.listInstances() ?: mutableListOf()
            logger.info("Got ${partInstances.size} instances from GCP")
            partInstances
        }
        val azureInstances = GlobalScope.async {
            logger.info("Getting instances from Azure")
            val partInstances = azureProvider?.listInstances() ?: mutableListOf()
            logger.info("Got ${partInstances.size} instances from Azure")
            partInstances
        }
        runBlocking {
            instances = awsInstances.await() + gcpInstances.await() + azureInstances.await()
        }
        return instances
    }

    /**
     * Retrieves the [InstanceInfo] for the instance identified by the given handle. Will call a method on the
     * handle to determine which of the overloaded functions below to call.
     * @param handle Stringified [InstanceHandle] that uniquely identifies the instance.
     * @return The [InstanceInfo] for the given handle.
     */
    fun getInstance(handle: InstanceHandle): InstanceInfo {
        return when (handle.provider) {
            Provider.GCP -> gcpProvider?.getInstance(handle) ?: throw ServiceProviderException(GCP_NOT_INITIALIZED_MESSAGE)
            Provider.AWS -> awsProvider?.getInstance(handle) ?: throw ServiceProviderException(GCP_NOT_INITIALIZED_MESSAGE)
            Provider.AZURE -> azureProvider?.getInstance(handle) ?: throw ServiceProviderException(AZURE_NOT_INITIALIZED_MESSAGE)
        }
    }

    fun getInstances(handles: List<InstanceHandle>): List<InstanceInfo> {
        return runBlocking {
            coroutineScope {
                handles.map {
                    async {
                        getInstance(it)
                    }
                }
            }.awaitAll()
        }
    }

    /**
     * Creates and instance with the given details. Will call a method on the handle to determine which of the
     * overloaded functions below to call.
     * @param provider The cloud provider on which the instance will be created.
     * @param name The name of the instance.
     * @param type The type / size of the instance.
     * @param imageId The image with which the instance will be created. Allows for creating preconfigured instances.
     * @param region The region in which to create the instance.
     * @return The [InstanceHandle] for the created instance.
     */
    fun createInstance(provider: Provider, name: String, type: String, imageId: String, region: String): InstanceHandle {
        return when (provider) {
            Provider.AWS -> awsProvider?.createInstance(name, type, imageId, region) ?: throw ServiceProviderException(AWS_NOT_INITIALIZED_MESSAGE)
            Provider.GCP -> gcpProvider?.createInstance(name, type, imageId, region) ?: throw ServiceProviderException(GCP_NOT_INITIALIZED_MESSAGE)
            Provider.AZURE -> azureProvider?.createInstance(name, type, imageId, region) ?: throw ServiceProviderException(AZURE_NOT_INITIALIZED_MESSAGE)
        }
    }

    /**
     * Deletes the instance identified by the given handle. Will call a method on the handle to determine which of
     * the overloaded functions below to call.
     * @param handle Stringified [InstanceHandle] that uniquely identifies the instance.
     * @return The [InstanceInfo] for the given handle.
     */
    fun deleteInstance(handle: InstanceHandle): Boolean {
        return when (handle.provider) {
            Provider.GCP -> gcpProvider?.deleteInstance(handle) ?: throw ServiceProviderException(GCP_NOT_INITIALIZED_MESSAGE)
            Provider.AWS -> awsProvider?.deleteInstance(handle) ?: throw ServiceProviderException(AWS_NOT_INITIALIZED_MESSAGE)
            Provider.AZURE -> azureProvider?.deleteInstance(handle) ?: throw ServiceProviderException(AZURE_NOT_INITIALIZED_MESSAGE)
        }
    }

    /**
     * Starts the instance identified by the given handle. Will call a method on the handle to determine which of
     * the overloaded functions below to call.
     * @param handle Stringified [InstanceHandle] that uniquely identifies the instance.
     * @return The [InstanceInfo] for the given handle.
     */
    fun startInstance(handle: InstanceHandle): Boolean {
        return when (handle.provider) {
            Provider.GCP -> gcpProvider?.startInstance(handle) ?: throw ServiceProviderException(GCP_NOT_INITIALIZED_MESSAGE)
            Provider.AWS -> awsProvider?.startInstance(handle) ?: throw ServiceProviderException(AWS_NOT_INITIALIZED_MESSAGE)
            Provider.AZURE -> azureProvider?.startInstance(handle) ?: throw ServiceProviderException(AZURE_NOT_INITIALIZED_MESSAGE)
        }
    }

    /**
     * Stops the instance identified by the given handle. Will call a method on the handle to determine which of
     * the overloaded functions below to call.
     * @param handle Stringified [InstanceHandle] that uniquely identifies the instance.
     * @return The [InstanceInfo] for the given handle.
     */
    fun stopInstance(handle: InstanceHandle): Boolean {
        return when (handle.provider) {
            Provider.GCP -> gcpProvider?.stopInstance(handle) ?: throw ServiceProviderException(GCP_NOT_INITIALIZED_MESSAGE)
            Provider.AWS -> awsProvider?.stopInstance(handle) ?: throw ServiceProviderException(AWS_NOT_INITIALIZED_MESSAGE)
            Provider.AZURE -> azureProvider?.stopInstance(handle) ?: throw ServiceProviderException(AZURE_NOT_INITIALIZED_MESSAGE)
        }
    }

    /**
     * Waits for the instance identified by the given handle to reach the given [InstanceState]. Returns true if the
     * instance reaches state before the timeout and false otherwise. Will call a method on the handle to determine
     * which of the overloaded functions below to call.
     * @param handle Stringified [InstanceHandle] that uniquely identifies the instance.
     * @param state The [InstanceState] to wait for.
     * @param timeout The maximum amount of time to spend waiting for state before giving up.
     * @return True if the instance reaches the given state by the timeout and false otherwise.
     */
    fun waitForState(handle: InstanceHandle, state: InstanceState, timeout: Int = 300): Boolean {
        return when (handle.provider) {
            Provider.GCP -> gcpProvider?.waitForState(handle, state, timeout) ?: throw ServiceProviderException(GCP_NOT_INITIALIZED_MESSAGE)
            Provider.AWS -> awsProvider?.waitForState(handle, state, timeout) ?: throw ServiceProviderException(AWS_NOT_INITIALIZED_MESSAGE)
            Provider.AZURE -> azureProvider?.waitForState(handle, state, timeout) ?: throw ServiceProviderException(AZURE_NOT_INITIALIZED_MESSAGE)
        }
    }
}
