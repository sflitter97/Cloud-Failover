package com.flitterkomskis.cloudfailover.cloudproviders.awsserviceprovider

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceHandle
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceInfo
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceState
import com.flitterkomskis.cloudfailover.cloudproviders.Provider
import java.lang.Thread.sleep
import java.time.Instant
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse
import software.amazon.awssdk.services.ec2.model.Ec2Exception
import software.amazon.awssdk.services.ec2.model.Instance
import software.amazon.awssdk.services.ec2.model.InstanceType
import software.amazon.awssdk.services.ec2.model.Reservation
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest
import software.amazon.awssdk.services.ec2.model.Tag
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest

/**
 * Adapter for manipulating AWS instances. Provides a generic interface for interacting with instances on AWS.
 */
class AwsServiceProvider(private val accessKey: String, private val secretKey: String) {
    private val logger: Logger = LoggerFactory.getLogger(AwsServiceProvider::class.java)
    private val MAX_RESULTS_ALLOWED = 1000
    private val POLL_INTERVAL = 2000
    private val ec2Clients: MutableMap<Region, Ec2Client> = mutableMapOf()

    /**
     * Get an ec2 client for the given [Region], creating it if it doesn't exist.
     * @param region Region in which to create the client.
     * @return Client object for interacting with AWS.
     */
    private fun getClient(region: Region): Ec2Client {
        return ec2Clients.getOrPut(region) {
            Ec2Client.builder().credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                            accessKey,
                            secretKey)
            ))
                    .region(region)
                    .build()
        }
    }

    /**
     * Get an ec2 client for the given [Region], creating it if it doesn't exist.
     * @param region Region in which to create the client.
     * @return Client object for interacting with AWS.
     */
    private fun getClient(region: String): Ec2Client {
        return getClient(Region.of(region))
    }

    /**
     * Gets a list of [Region]s available for the current AWS account.
     * @return A list of [Region] which the current account can access.
     */
    private fun getRegions(): List<Region> {
        // us-east-1 is always active, so we can use it to find which other regions are enabled
        val client = getClient(Region.US_EAST_1)
        return client.describeRegions().regions().map { region -> Region.of(region.regionName()) }
    }

    /**
     * Helper function to convert the state code of an instance into the corresponding [InstanceState]. Base on
     * https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/ec2/model/InstanceState.html#code--
     * @param stateCode The state code of the instance from AWS
     * @return The matching [InstanceState] for the state code
     */
    private fun getInstanceState(stateCode: Int): InstanceState {
        return when (stateCode and 0b11111111) {
            0 -> InstanceState.PENDING
            16 -> InstanceState.RUNNING
            32 -> InstanceState.DELETING
            48 -> InstanceState.DELETED
            64 -> InstanceState.STOPPING
            80 -> InstanceState.STOPPED
            else -> InstanceState.UNKNOWN
        }
    }

    /**
     * Get a list of all the instances across all AWS regions.
     * @return [List] of [InstanceInfo] describing all instances available to this AWS account across all available
     * AWS regions.
     */
    fun listInstances(): List<InstanceInfo> {
        var ret = listOf<InstanceInfo>()
        val promises = getRegions().map { region ->
            GlobalScope.async {
                logger.info("Getting instances from AWS $region")
                val instances = mutableListOf<InstanceInfo>()
                val client = getClient(region)

                var nextToken: String?
                val reservations = mutableListOf<Reservation>()
                do {
                    val listRequest: DescribeInstancesRequest = DescribeInstancesRequest.builder().maxResults(MAX_RESULTS_ALLOWED).build()
                    val listResponse: DescribeInstancesResponse = client.describeInstances(listRequest)

                    reservations.addAll(listResponse.reservations())

                    nextToken = listResponse.nextToken()
                } while (nextToken != null)

                for (reservation in reservations) {
                    for (instance in reservation.instances()) {
                        val name = instance.tags().stream().filter { pair ->
                            pair.key() == "Name"
                        }.findFirst()
                            .orElse(Tag.builder().key("Name").value("No name").build())
                            .value()
                        logger.debug("Name: $name Image ID: ${instance.imageId()} Instance ID: ${instance.instanceId()} State: ${instance.state().name()}")
                        instances.add(InstanceInfo(
                            Provider.AWS,
                            name,
                            instance.instanceType().toString(),
                            getInstanceState(instance.state().code()),
                            InstanceHandle(instance.instanceId(), region.toString(), Provider.AWS),
                            instance.publicDnsName()
                        ))
                    }
                }
                logger.info("Got ${instances.size} instances from AWS $region")
                instances
            }
        }
        runBlocking {
            ret = promises.awaitAll().flatten()
        }
        return ret
    }

    /**
     * Creates an instance in the given region with the given details.
     * @param name The name of the instance.
     * @param type The type / size of the instance.
     * @param imageId The image with which the instance will be created.
     * @param region The region in which to create the instance.
     * @return A handle to the instance that uniquely identifies it.
     */
    fun createInstance(name: String, type: String, imageId: String, region: String, securityGroup: String = "sg-0b7eba9f3e3d6f139"): InstanceHandle {
        val client = getClient(region)
        val runRequest = RunInstancesRequest.builder()
                .imageId(imageId)
                .instanceType(InstanceType.valueOf(type))
                .securityGroupIds(securityGroup)
                .maxCount(1)
                .minCount(1)
                .build()

        val response = client.runInstances(runRequest)

        val instanceId = response.instances()[0].instanceId()

        val tag = Tag.builder()
                .key("Name")
                .value(name)
                .build()

        val tagRequest = CreateTagsRequest.builder()
                .resources(instanceId)
                .tags(tag)
                .build()

        try {
            client.createTags(tagRequest)
        } catch (e: Ec2Exception) {
            throw AwsServiceProviderException("Error adding tags to instance.")
        }
        return InstanceHandle(instanceId, region, Provider.AWS)
    }

    /**
     * Deletes the instance with the given handle.
     * @param handle The handle that uniquely identifies the instance to be deleted.
     * @return True if the instance was successfully deleted and false otherwise.
     */
    fun deleteInstance(handle: InstanceHandle): Boolean {
        val client = getClient(handle.region)

        val terminateRequest = TerminateInstancesRequest.builder()
                .instanceIds(handle.instanceId)
                .build()

        val response = client.terminateInstances(terminateRequest)

        return response.terminatingInstances().find { stateChange -> stateChange.instanceId() == handle.instanceId } != null
    }

    /**
     * Starts the instance with the given handle.
     * @param handle The handle that uniquely identifies the instance to be started.
     * @return True if the instance was successfully started and false otherwise.
     */
    fun startInstance(handle: InstanceHandle): Boolean {
        val client = getClient(handle.region)

        val startRequest = StartInstancesRequest.builder()
                .instanceIds(handle.instanceId)
                .build()

        val response = client.startInstances(startRequest)

        return response.startingInstances().find { stateChange -> stateChange.instanceId() == handle.instanceId } != null
    }

    /**
     * Stops the instance with the given handle.
     * @param handle The handle that uniquely identifies the instance to be stopped.
     * @return True if the instance was successfully stopped and false otherwise.
     */
    fun stopInstance(handle: InstanceHandle): Boolean {
        val client = getClient(handle.region)

        val stopRequest = StopInstancesRequest.builder()
                .instanceIds(handle.instanceId)
                .build()

        val response = client.stopInstances(stopRequest)

        return response.stoppingInstances().find { stateChange -> stateChange.instanceId() == handle.instanceId } != null
    }

    /**
     * Gets the instance information for the instance with the given handle
     * @param handle The handle that uniquely identifies the instance.
     * @return [InstanceInfo] describing the instance.
     */
    fun getInstance(handle: InstanceHandle): InstanceInfo {
        logger.info("Getting instance for handle $handle")
        val client = getClient(handle.region)
        val getRequest: DescribeInstancesRequest = DescribeInstancesRequest.builder().instanceIds(handle.instanceId).build()
        val getResponse: DescribeInstancesResponse = client.describeInstances(getRequest)
        var instance: Instance? = null
        for (reservation in getResponse.reservations()) {
            instance = reservation.instances().find { inst -> inst.instanceId() == handle.instanceId }
            if (instance != null) {
                break
            }
        }
        if (instance != null) {
            val name = instance.tags().stream().filter { pair ->
                pair.key() == "Name"
            }.findFirst()
                    .orElse(Tag.builder().key("Name").value("No name").build())
                    .value()

            val info = InstanceInfo(
                    Provider.AWS,
                    name,
                    instance.instanceType().toString(),
                    getInstanceState(instance.state().code()),
                    handle,
                    instance.publicDnsName()
            )
            logger.info("Instance info for handle $handle is $info")
            return info
        } else {
            throw AwsServiceProviderException("Instance with ID ${handle.instanceId} not found.")
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
            sleep((POLL_INTERVAL.toLong()))
            currTime = Instant.now()
        }
        return getInstance(handle).state == state
    }
}
