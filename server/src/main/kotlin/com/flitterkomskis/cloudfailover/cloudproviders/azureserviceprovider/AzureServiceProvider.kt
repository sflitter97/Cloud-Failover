package com.flitterkomskis.cloudfailover.cloudproviders.azureserviceprovider

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceInfo
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceState
import com.flitterkomskis.cloudfailover.cloudproviders.Provider
import com.microsoft.azure.management.Azure
import com.microsoft.azure.management.compute.PowerState
import com.microsoft.azure.management.network.Network
import com.microsoft.azure.management.network.NetworkInterface
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.rest.LogLevel
import java.io.File
import java.time.Instant
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AzureServiceProvider {
    private val POLL_INTERVAL = 2000
    private val logger: Logger = LoggerFactory.getLogger(AzureServiceProvider::class.java)
    private val resourceGroup: String = "SeniorDesign"
    private val azure: Azure = initAzure()

    private fun initAzure(): Azure {
        try {
            val credFile = File(System.getenv("AZURE_AUTH_LOCATION"))
            return Azure.configure()
                    .withLogLevel(LogLevel.BASIC)
                    .authenticate(credFile)
                    .withDefaultSubscription()
        } catch (e: Exception) {
            println(e.message)
            throw AzureServiceProviderException("Error initializing Azure Control object")
        }
    }

    private fun getInstanceState(state: PowerState): InstanceState {
        return when (state) {
            PowerState.DEALLOCATED -> InstanceState.DEALLOCATED
            PowerState.DEALLOCATING -> InstanceState.DEALLOCATING
            PowerState.RUNNING -> InstanceState.RUNNING
            PowerState.STARTING -> InstanceState.STARTING
            PowerState.STOPPED -> InstanceState.STOPPED
            PowerState.STOPPING -> InstanceState.STOPPING
            PowerState.UNKNOWN -> InstanceState.UNKNOWN
            else -> throw AzureServiceProviderException("Unable to recognize instance state")
        }
    }

    fun listInstances(): List<InstanceInfo> {
        logger.info("Getting instances from Azure")
        try {
            val instances = mutableListOf<InstanceInfo>()
            azure.virtualMachines().listByResourceGroup(resourceGroup).forEach {
                instances.add(
                        InstanceInfo(
                                Provider.AZURE,
                                it.name(),
                                it.size().toString(),
                                getInstanceState(it.powerState()),
                                AzureInstanceHandle(it.name(), resourceGroup),
                                it.primaryNetworkInterface.toString()
                        )
                )
                logger.debug(
                        "Name: ${it.name()}" +
                                "Image ID: ${it.osType()}" +
                                "Instance ID: ${it.id()}" +
                                "State: ${it.powerState()}"
                )
            }
            logger.info("Got ${instances.size} instances from Azure")
            return instances
        } catch (e: Exception) {
            throw AzureServiceProviderException("Error listing instances.")
        }
    }

    fun createInstance(name: String, type: String, imageId: String, region: String): AzureInstanceHandle {
        try {
            val regionFromString: Region = Region.findByLabelOrName(region)

            // Creating public IP address
            val publicIPAddress = azure.publicIPAddresses()
                    .define(name)
                    .withRegion(regionFromString)
                    .withExistingResourceGroup(resourceGroup)
                    .withDynamicIP()
                    .create()

            // Creating Virtual Network
            val network: Network = azure.networks()
                    .define("myVN")
                    .withRegion(regionFromString)
                    .withExistingResourceGroup(resourceGroup)
                    .withAddressSpace("10.0.0.0/16")
                    .withSubnet("mySubnet", "10.0.0.0/24")
                    .create()

            // Creating network interface
            val networkInterface: NetworkInterface = azure.networkInterfaces()
                    .define(name)
                    .withRegion(regionFromString)
                    .withExistingResourceGroup(resourceGroup)
                    .withExistingPrimaryNetwork(network)
                    .withSubnet("mySubnet")
                    .withPrimaryPrivateIPAddressDynamic()
                    .withExistingPrimaryPublicIPAddress(publicIPAddress)
                    .create()

            // Creating virtual machine
            azure.virtualMachines()
                    .define(name)
                    .withRegion(regionFromString)
                    .withExistingResourceGroup(resourceGroup)
                    .withExistingPrimaryNetworkInterface(networkInterface)
                    .withLatestLinuxImage("Canonical", "UbuntuServer", "18.04-LTS")
                    .withRootUsername("azureuser")
                    .withRootPassword("Azure12345678")
                    .withComputerName(name)
                    .withSize(type)
                    .create()

            return AzureInstanceHandle(name, resourceGroup)
        } catch (e: Exception) {
            println(e.message)
            throw AzureServiceProviderException("Error creating instance")
        }
    }

    fun deleteInstance(handle: AzureInstanceHandle): Boolean {
        try {
            val vm =
                    azure.virtualMachines().getByResourceGroup(handle.region, handle.instanceId)
            azure.virtualMachines().deleteById(vm.id())
            return true
        } catch (e: Exception) {
            throw AzureServiceProviderException("Error deleting instance")
        }
    }

    fun startInstance(handle: AzureInstanceHandle): Boolean {
        try {
            val vm =
                    azure.virtualMachines().getByResourceGroup(handle.region, handle.instanceId)
            vm.start()
            return true
        } catch (e: Exception) {
            throw AzureServiceProviderException("Error starting instance")
        }
    }

    fun stopInstance(handle: AzureInstanceHandle): Boolean {
        try {
            val vm =
                    azure.virtualMachines().getByResourceGroup(handle.region, handle.instanceId)
            vm.deallocate()
            return true
        } catch (e: Exception) {
            throw AzureServiceProviderException("Error stopping instance")
        }
    }

    fun getInstance(handle: AzureInstanceHandle): InstanceInfo {
        try {
            val vm =
                    azure.virtualMachines().getByResourceGroup(handle.region, handle.instanceId)
            return InstanceInfo(
                    Provider.AZURE,
                    handle.instanceId,
                    vm.size().toString(),
                    getInstanceState(vm.powerState()),
                    handle,
                    vm.primaryNetworkInterface.toString()
            )
        } catch (e: Exception) {
            throw AzureServiceProviderException("Error getting instance")
        }
    }

    fun waitForState(handle: AzureInstanceHandle, state: InstanceState, timeout: Int): Boolean {
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
