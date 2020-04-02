package com.flitterkomskis.cloudfailover.cloudproviders

/**
 * Exception thrown when attempting to get an instance no longer available.
 */
class InstanceDeletedException(message: String): Exception(message) {

}