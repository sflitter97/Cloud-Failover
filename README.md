# Cloud Failover Service

Application agnostic service that resides between end users and cloud providers to provide high availability and automatic failover to applications without code changes or vendor lockin.

## Starting the Service for Development

1. Download the repo
2. Set the following environmental variables
   1. Set AWS_ACCESS_KEY to your access key from AWS
   2. Set AWS_SECRET_KEY to your secret key from AWS
   3. Set AZURE_AUTH_LOCATION to the path of your azureauth.properties file
   4. Set GCP_PROJECT to your GCP project name
   5. Set GOOGLE_APPLICATION_CREDENTIALS to the path of your GCP credentials file
   6. (Optional) Set MULTICLOUD_FAILOVER_MONGO_CONN_STR to the connection string of your Mongo instance. If unspecified it will default to localhost on port 27017
3. Open the server folder in IntelliJ and run the bootRun task
4. Open the react-ui folder in a terminal and run `npm start`
5. During development, the frontend will run on port 3000 and the backend will run on port 8080.

## Building the Service

1. Open the root of the repo in a terminal and run `gradle build`. This will built the frontend, copy the built files to the backend,  build the backend as a single jar with all dependencies, and copy the jar to the root of the repo.
2. To run the service, run `java -jar <name_of_built_jar_file>`.
3. The service will run on port 8080 when started.
