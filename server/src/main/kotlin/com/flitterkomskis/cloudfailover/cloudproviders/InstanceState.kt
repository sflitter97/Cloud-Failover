package com.flitterkomskis.cloudfailover.cloudproviders

enum class InstanceState {
    STOPPED, RUNNING, PENDING, STOPPING, DELETED, DELETING, UNKNOWN, PROVISIONING, STAGING, REPAIRING, TERMINATED
}
