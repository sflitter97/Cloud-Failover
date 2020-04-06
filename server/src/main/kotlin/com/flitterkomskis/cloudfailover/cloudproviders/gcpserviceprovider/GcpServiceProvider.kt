package com.flitterkomskis.cloudfailover.cloudproviders.gcpserviceprovider

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceDeletedException
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceHandle
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceInfo
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceState
import com.flitterkomskis.cloudfailover.cloudproviders.Provider
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.compute.Compute
import com.google.api.services.compute.model.AccessConfig
import com.google.api.services.compute.model.AttachedDisk
import com.google.api.services.compute.model.AttachedDiskInitializeParams
import com.google.api.services.compute.model.Instance
import com.google.api.services.compute.model.NetworkInterface
import com.google.api.services.compute.model.ZoneList
import java.io.IOException
import java.security.GeneralSecurityException
import java.time.Instant
import java.util.Collections
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Adapter for manipulating GCP instances. Provides a generic interface for interacting with instances on GCP.
 */
class GcpServiceProvider(private val projectId: String) {
    private val POLL_INTERVAL = 2000
    private val logger: Logger = LoggerFactory.getLogger(GcpServiceProvider::class.java)
    private val computeService: Compute = createComputeService()

    @Throws(IOException::class, GeneralSecurityException::class)
    private fun createComputeService(): Compute {
        val httpTransport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
        var credential: GoogleCredential = GoogleCredential.getApplicationDefault()
        if (credential.createScopedRequired())
            credential = credential.createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
        return Compute.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName(projectId)
            .build()
    }

    /**
     * Helper function to convert the status of an instance into the corresponding [InstanceState].
     * @param status The status of the instance from Azure
     * @return The matching [InstanceState] for the status
     */
    private fun getInstanceState(status: String): InstanceState {
        return when (status) {
            "PROVISIONING" -> InstanceState.PROVISIONING
            "STAGING" -> InstanceState.STAGING
            "RUNNING" -> InstanceState.RUNNING
            "STOPPING" -> InstanceState.STOPPING
            "REPAIRING" -> InstanceState.REPAIRING
            "TERMINATED" -> InstanceState.TERMINATED
            else -> InstanceState.UNKNOWN
        }
    }

    /**
     * Gets a list of all instances in a given zone.
     * @param zone The zone in which to list instances.
     * @return [List] of [InstanceInfo] describing the instances in the given zone.
     */
    private fun getInstancesInZone(zone: String): List<InstanceInfo> {
        logger.info("Getting instances from GCP $zone")
        try {
            val request = computeService.instances().list(projectId, zone)
            val instances = mutableListOf<InstanceInfo>()

            do {
                val response = request.execute()
                if (response.items == null)
                    continue
                for (instance in response.items) {
                    logger.debug(
                            "Name: ${instance.name} " +
                            "Image ID: ${instance.disks}" +
                            "Instance ID: ${instance.id}" +
                            "State: ${instance.status}"
                    )
                    instances.add(
                        InstanceInfo(
                            Provider.GCP,
                            instance.name,
                            instance.machineType,
                            getInstanceState(instance.status),
                            InstanceHandle(instance.name, zone, Provider.GCP),
                            instance.networkInterfaces.get(0).network
                    ))
                }
                request.pageToken = response.nextPageToken
            } while (response.nextPageToken != null)
            logger.info("Got ${instances.size} instances from GCP $zone")
            return instances
        } catch (e: Exception) {
            throw GcpServiceProviderException("Error retrieving instances.")
        }
    }

    /**
     * Helper function to created a disk for an instance.
     * @param image The image to use when creating the disk.
     * @return The [AttachedDisk] which will be attached to the instance.
     */
    private fun initializeDisk(image: String): AttachedDisk {
        val disk = AttachedDisk()
        disk.boot = true
        disk.autoDelete = true
        val params = AttachedDiskInitializeParams()
        params.sourceImage = image
        disk.initializeParams = params
        return disk
    }

    /**
     * Get a list of all the instances from GCP.
     * @return [List] of [InstanceInfo] describing all instances created by the GCP account across all GCP zones.
     */
    fun listInstances(): List<InstanceInfo> {
        try {
            val instances = mutableListOf<InstanceInfo>()
            val request = computeService.zones().list(projectId)
            var response: ZoneList
            do {
                response = request.execute()
                if (response.items == null)
                    continue
                val promises = response.items.map { zone ->
                    GlobalScope.async {
                        getInstancesInZone(zone.name)
                    }
                }
                runBlocking {
                    instances += promises.awaitAll().flatten()
                }
                request.pageToken = response.nextPageToken
            } while (response.nextPageToken != null)
            return instances
        } catch (e: Exception) {
            throw GcpServiceProviderException("Error listing instances ${e.message}.")
        }
    }

    /**
     * Creates an instance in the given region with the given details.
     * @param name The name of the instance.
     * @param type The type / size of the instance.
     * @param image The image with which the instance will be created.
     * @param zone The region in which to create the instance.
     * @return A handle to the instance that uniquely identifies it.
     */
    fun createInstance(name: String, type: String, image: String, zone: String): InstanceHandle {
        try {
            val disk = initializeDisk(image)
            val networkInterface = NetworkInterface()
            networkInterface.accessConfigs = Collections.singletonList(AccessConfig())

            val requestBody = Instance()
                .setName(name)
                .setMachineType(type)
                .setZone(zone)
                .setDisks(Collections.singletonList(disk))
                .setNetworkInterfaces(Collections.singletonList(networkInterface))

            val request = computeService.instances().insert(projectId, zone, requestBody)
            request.execute()

            return InstanceHandle(name, zone, Provider.GCP)
        } catch (e: Exception) {
            throw GcpServiceProviderException("Error creating instance ${e.message}")
        }
    }

    /**
     * Deletes the instance with the given handle.
     * @param handle The handle that uniquely identifies the instance to be deleted.
     * @return True if the instance was successfully deleted and false otherwise.
     */
    fun deleteInstance(handle: InstanceHandle): Boolean {
        try {
            val request = computeService.instances().delete(projectId, handle.region, handle.instanceId)
            request.execute()
            return true
        } catch (e: Exception) {
            throw GcpServiceProviderException("Error deleting instance.")
        }
    }

    /**
     * Starts the instance with the given handle.
     * @param handle The handle that uniquely identifies the instance to be started.
     * @return True if the instance was successfully started and false otherwise.
     */
    fun startInstance(handle: InstanceHandle): Boolean {
        try {
            val request: Compute.Instances.Start =
                computeService.instances().start(projectId, handle.region, handle.instanceId)
            request.execute()
            return true
        } catch (e: Exception) {
            throw GcpServiceProviderException("Error starting instance.")
        }
    }

    /**
     * Stops the instance with the given handle.
     * @param handle The handle that uniquely identifies the instance to be stopped.
     * @return True if the instance was successfully stopped and false otherwise.
     */
    fun stopInstance(handle: InstanceHandle): Boolean {
        try {
            val request: Compute.Instances.Stop =
                computeService.instances().stop(projectId, handle.region, handle.instanceId)
            request.execute()
            return true
        } catch (e: Exception) {
            throw GcpServiceProviderException("Error stopping instance.")
        }
    }

    /**
     * Gets the instance information for the instance with the given handle
     * @param handle The handle that uniquely identifies the instance.
     * @return [InstanceInfo] describing the instance.
     */
    fun getInstance(handle: InstanceHandle): InstanceInfo {
        try {
            val request: Compute.Instances.Get =
                computeService.instances().get(projectId, handle.region, handle.instanceId)
            val response = request.execute()
            return InstanceInfo(
                Provider.GCP,
                response.name,
                response.machineType,
                getInstanceState(response.status),
                handle,
                response.networkInterfaces[0].accessConfigs[0].natIP
            )
        } catch (e: GoogleJsonResponseException) {
            if (e.statusCode == 404) {
                throw InstanceDeletedException(e.message ?: "")
            } else {
                throw GcpServiceProviderException("Error getting instance ${e.message}.")
            }
        }
    }

    /**
     * Polls an instance until it reaches the given state or until the timeout is reached.
     * @param handle The handle that uniquely identifies the instance.
     * @param state The [InstanceState] to wait for.
     * @param timeout The maximum amount of time to wait for the instance to reach the state.
     * @return True if the instance reaches state before the timeout and false otherwise.
     */
    fun waitForState(handle: InstanceHandle, state: InstanceState, timeout: Int): Boolean {
        val startTime = Instant.now()
        val timeoutTime = startTime.plusSeconds(timeout.toLong())
        var currTime = Instant.now()
        while (currTime.isBefore(timeoutTime) && getInstance(handle).state != state) {
            Thread.sleep((POLL_INTERVAL.toLong()))
            currTime = Instant.now()
        }
        return getInstance(handle).state == state
    }
}
