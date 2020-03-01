package com.flitterkomskis.cloudfailover.cloudproviders.awsserviceprovider

import com.flitterkomskis.cloudfailover.cloudproviders.InstanceInfo
import com.flitterkomskis.cloudfailover.cloudproviders.InstanceState
import com.flitterkomskis.cloudfailover.cloudproviders.Provider
import java.lang.Thread.sleep
import java.time.Instant
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

class AwsServiceProvider(private val accessKey: String, private val secretKey: String) {
    private val logger: Logger = LoggerFactory.getLogger(AwsServiceProvider::class.java)
    private val MAX_RESULTS_ALLOWED = 1000
    private val POLL_INTERVAL = 2000
    private val ec2Clients: MutableMap<Region, Ec2Client> = mutableMapOf()

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

    private fun getClient(region: String): Ec2Client {
        return getClient(Region.of(region))
    }

    private fun getRegions(): List<Region> {
        // us-east-1 is always active, so we can use it to find which other regions are enabled
        val client = getClient(Region.US_EAST_1)
        return client.describeRegions().regions().map { region -> Region.of(region.regionName()) }
    }

    // from https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/ec2/model/InstanceState.html#code--
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

    fun listInstances(): List<InstanceInfo> {
        val instances = mutableListOf<InstanceInfo>()
        for (region in getRegions()) {
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
                            AwsInstanceHandle(instance.instanceId(), region.toString()),
                            instance.publicDnsName()
                    ))
                }
            }
        }
        return instances
    }

    fun createInstance(name: String, type: String, imageId: String, region: String): AwsInstanceHandle {
        val client = getClient(region)
        val runRequest = RunInstancesRequest.builder()
                .imageId(imageId)
                .instanceType(InstanceType.valueOf(type))
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
        return AwsInstanceHandle(instanceId, region)
    }

    fun deleteInstance(handle: AwsInstanceHandle): Boolean {
        val client = getClient(handle.region)

        val terminateRequest = TerminateInstancesRequest.builder()
                .instanceIds(handle.instanceId)
                .build()

        val response = client.terminateInstances(terminateRequest)

        return response.terminatingInstances().find { stateChange -> stateChange.instanceId() == handle.instanceId } != null
    }

    fun startInstance(handle: AwsInstanceHandle): Boolean {
        val client = getClient(handle.region)

        val startRequest = StartInstancesRequest.builder()
                .instanceIds(handle.instanceId)
                .build()

        val response = client.startInstances(startRequest)

        return response.startingInstances().find { stateChange -> stateChange.instanceId() == handle.instanceId } != null
    }

    fun stopInstance(handle: AwsInstanceHandle): Boolean {
        val client = getClient(handle.region)

        val stopRequest = StopInstancesRequest.builder()
                .instanceIds(handle.instanceId)
                .build()

        val response = client.stopInstances(stopRequest)

        return response.stoppingInstances().find { stateChange -> stateChange.instanceId() == handle.instanceId } != null
    }

    fun getInstance(handle: AwsInstanceHandle): InstanceInfo {
        logger.info("Getting instance for handle ${handle.toString()}")
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
            logger.info("Instance info for handle ${handle.toString()} is ${info.toString()}")
            return info
        } else {
            throw AwsServiceProviderException("Instance with ID ${handle.instanceId} not found.")
        }
    }

    fun waitForState(handle: AwsInstanceHandle, state: InstanceState, timeout: Int): Boolean {
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
