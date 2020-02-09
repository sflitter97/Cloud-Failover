package com.flitterkomskis.cloudfailover.cloudproviders.gcpserviceprovider

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceInfo
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceState
import com.flitterkomskis.cloudfailover.cloudproviders.Provider
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.compute.Compute
import com.google.api.services.compute.model.AttachedDisk
import com.google.api.services.compute.model.AttachedDiskInitializeParams
import com.google.api.services.compute.model.Instance
import com.google.api.services.compute.model.NetworkInterface
import com.google.api.services.compute.model.ZoneList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.security.GeneralSecurityException
import java.time.Instant
import java.util.Arrays
import java.util.Collections

class GcpServiceProvider() {
    private val POLL_INTERVAL = 2000
    private val PROJECT_ID = "vaulted-harbor-266817"
    private val logger: Logger = LoggerFactory.getLogger(GcpServiceProvider::class.java)
    private var computeService = createComputeService()

    @Throws(IOException::class, GeneralSecurityException::class)
    private fun createComputeService(): Compute? {
        val httpTransport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
        var credential: GoogleCredential = GoogleCredential.getApplicationDefault()
        if (credential.createScopedRequired())
            credential = credential.createScoped(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"))
        return Compute.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName(PROJECT_ID)
            .build()
    }

    private fun getInstanceState(status: String): InstanceState {
        return when(status) {
            "PROVISIONING" -> InstanceState.PROVISIONING
            "STAGING" -> InstanceState.STAGING
            "RUNNING" -> InstanceState.RUNNING
            "STOPPING" -> InstanceState.STOPPING
            "REPAIRING" -> InstanceState.REPAIRING
            "TERMINATED" -> InstanceState.TERMINATED
            else -> InstanceState.UNKNOWN
        }
    }

    private fun getInstancesInZone(zone: String): List<InstanceInfo> {
        try {
            val request = computeService!!.instances().list(PROJECT_ID, zone)
            val instances = mutableListOf<InstanceInfo>()

            do {
                val response = request.execute()
                if (response.items == null)
                    continue
                for (instance in response.items) {
                    logger.debug(
                            "Name: ${instance.name} " +
                            "Image ID: ${instance.disks.toString()}" +
                            "Instance ID: ${instance.id}" +
                            "State: ${instance.status}"
                    )
                    instances.add(
                        InstanceInfo(
                            Provider.GCP,
                            instance.name,
                            instance.machineType,
                            getInstanceState(instance.status),
                            instance.networkInterfaces.get(0).network
                    ))
                }
                request.pageToken = response.nextPageToken
            } while (response.nextPageToken != null)
            return instances
        } catch (e: Exception) {
            throw GcpServiceProviderException("Error retrieving instances.")
        }
    }

    private fun initializeDisk(image: String): AttachedDisk {
        val disk = AttachedDisk()
        disk.boot = true
        disk.autoDelete = true
        val params = AttachedDiskInitializeParams()
        params.sourceImage = image
        disk.initializeParams = params
        return disk
    }

    fun listInstances(): List<InstanceInfo> {
        try {
            val instances = mutableListOf<InstanceInfo>()
            val request = computeService!!.zones().list(PROJECT_ID)
            var response: ZoneList
            do {
                response = request.execute()
                if (response.items == null)
                    continue
                for (zone in response.items)
                    instances.addAll(getInstancesInZone(zone.name))
                request.pageToken = response.nextPageToken
            } while (response.nextPageToken != null)
            return instances
        } catch (e: Exception) {
            throw GcpServiceProviderException("Error listing instances.")
        }
    }

    fun createInstance(name: String, type: String, image: String, zone: String): GcpInstanceHandle {
        try {
            val disk = initializeDisk(image)

            val requestBody = Instance()
                .setName(name)
                .setMachineType(type)
                .setZone(zone)
                .setDisks(Collections.singletonList(disk))
                .setNetworkInterfaces(Collections.singletonList(NetworkInterface()))

            val request = computeService!!.instances().insert(PROJECT_ID, zone, requestBody)
            request.execute()

            return GcpInstanceHandle(name, zone)
        }catch (e: Exception) {
            throw GcpServiceProviderException("Error creating instance")
        }
    }

    fun deleteInstance(handle: GcpInstanceHandle): Boolean {
        try {
            val request = computeService!!.instances().delete(PROJECT_ID, handle.region, handle.instanceId)
            request.execute()
            return true
        } catch (e: Exception) {
            throw GcpServiceProviderException("Error deleting instance.")
        }
    }

    fun startInstance(handle: GcpInstanceHandle): Boolean {
        try {
            val request: Compute.Instances.Start =
                computeService!!.instances().start(PROJECT_ID, handle.region, handle.instanceId)
            request.execute()
            return true
        } catch (e: Exception) {
            throw GcpServiceProviderException("Error starting instance.")
        }
    }

    fun stopInstance(handle: GcpInstanceHandle): Boolean {
        try {
            val request: Compute.Instances.Stop =
                computeService!!.instances().stop(PROJECT_ID, handle.region, handle.instanceId)
            request.execute()
            return true
        } catch (e: Exception) {
            throw GcpServiceProviderException("Error stopping instance.")
        }
    }

    fun getInstance(handle: GcpInstanceHandle): InstanceInfo {
        try {
            val request: Compute.Instances.Get =
                computeService!!.instances().get(PROJECT_ID, handle.region, handle.instanceId)
            val response = request.execute()
            return InstanceInfo(
                Provider.GCP,
                response.name,
                response.machineType,
                getInstanceState(response.status),
                response.networkInterfaces.get(0).network)
        } catch (e: Exception) {
            throw GcpServiceProviderException("Error getting instance.")
        }
    }

    fun waitForState(handle: GcpInstanceHandle, state: InstanceState, timeout: Int): Boolean {
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