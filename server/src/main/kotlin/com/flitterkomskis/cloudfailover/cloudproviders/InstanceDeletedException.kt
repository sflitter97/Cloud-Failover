package com.flitterkomskis.cloudfailover.cloudproviders

/**
 * Exception thrown when attempting to get an instance no longer available.
 * Used so we can recognize when an instance has been deleted without going through the service so we can remove
 * stale handles.
 */
class InstanceDeletedException(message: String) : Exception(message)
