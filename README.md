# Prepare local environment with minikube
## Install minikube  
Install minikube according to the official documentation.  
Install the minikube addons dashboard, ingress, metrics-server.  

## Install docker postgres outside minikube on host machine
Run the following command which will initialize and start the docker container:  
    `docker run -itd -e POSTGRES_USER=admin -e POSTGRES_PASSWORD=admin -p 5432:5432 -v ~/postgresql/data:/var/lib/postgresql/data --name postgres postgres`    
Check that container is running:  
    `docker ps -a`  
Stop container:  
    `docker stop postgres`  
Start container:  
    `docker start postgres`  
Test connection to postgres with example command (provide password on prompt):  
    `psql -h localhost -p 5432 -d postgres -U admin`  
Create sql database:  
    `create database "dataflow";`  
Connect to new database and create schema:  
    `create schema "batch-framework";`
Data for the SCDF server spring batch/task orchestration will be auto generated when the SCDF server is deployed.  
Data for the spring batch/task actual record results will be auto generated on the first connection to the database during registration of the applications and tasks.   

# Update SCDF server files:
Modify the following:
- Update the `batch-common/server/server-config.yaml` file with the postgres credentials and the minikube domain `host.minikube.internal` if it not there.  
- Update the `batch-common/server/ingress.yaml` file host and replace the ip in the `scdf-server-<minikube-ip>.nip.io` with the ip from minikube retrieved from:  
  `minikube ip`

# Deploying Spring Cloud Data Flow in k8s
Create the server objects:  
    `kubectl create -f batch-common/kubernetes/server`

Once it is started we should be able to see the auto generated spring batch/task tables in the database under the public schema.  

Delete the server objects:  
    `kubectl delete -f batch-common/kubernetes/server`

# Build the project
Maven build the project to generate the `.jar` files.

# Build docker images in the minikube registry
When building the docker images from a terminal first run:  
    `eval $(minikube docker-env)`  
Then run the bash script to build and deploy all images:  
    `cd batch-common/script`  
    `./docker-build.sh`

# Prepare the SCDF jobs
Copy the file `batch-client/src/test/resources/application.properties.sample` to `batch-client/src/test/resources/application.properties`.  
Update the `batch-client/src/test/resources/application.properties` with the correct credentials.  
Register the SCDF 'applications' by running the test method `data.RegistrationTestIT.registerApplications`.  
Create the SCDF 'tasks' by running the test method `data.RegistrationTestIT.createTasks`.
Access the SCDF server UI. From the address in the `batch-common/server/ingress.yaml` we should see a list of links as a response from the server.  
From those links we click on the link that has the prefix `/dashboard` to access the UI where we should now be able to see all applications and tasks registered.  
If needed we can destroy(will also delete k8s historical pods) the tasks using `data.RegistrationTestIT.destroyTasks`.
If needed we can unregister the applications using `data.RegistrationTestIT.unregisterApplications`

# Start a job in SCDF
1. You need to fill source.* properties in application.properties file with valid OAI source config
2. In the test file `data.SpringBatchTestIT` there are test methods for each plugin. 
The tests clear whole database before executing, leaving only the test dataset previous steps.
Single test could be run, but only if previous steps were completed on last previous test execution.
If you get the error `0/1 nodes are available: 1 Insufficient cpu. preemption: 0/1 nodes are available: 1 No preemption victims found for incoming pod.`, it 
means that there are not enough resources available on your machine. To fix that you can make sure there is availability of resources by removing/stopping 
other pods, or try reducing the resources of the job/pod when sending the launch command.



# Run Jobs/Tasks using Spring Cloud Data Flow Shell(old way of accessing the server, possibly outdated)
Download the script .jar from:  
https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-dataflow-shell/2.11.2/spring-cloud-dataflow-shell-2.11.2.jar  

Get into the shell(Update the uri to the data from server location):
```console
java -jar spring-cloud-dataflow-shell-2.11.2.jar --dataflow.uri=http://scdf-server-192.168.49.2.nip.io  
```

Some example commands:  
```console
app list    
app register --name batch-oai-harvest --type task --bootVersion 3 --uri docker:europeana/batch-oai-harvest:latest --metadata-uri file:///data/spring-batch-metis-poc/batch-application.properties  
app info --name batch-oai-harvest --type task
app unregister --name batch-oai-harvest --type task
task create --name batch-oai-harvest --definition batch-oai-harvest
task destroy --name batch-oai-harvest --cleanup true
task execution stop --ids 8
 
task launch --name batch-oai-harvest --properties "\
    app.batch-oai-harvest.spring.datasource.url=jdbc:postgresql://192.168.49.2:30432/dataflow?useSSL=false,\
    app.batch-oai-harvest.spring.datasource.driver-class-name=org.postgresql.Driver,\
    app.batch-oai-harvest.spring.datasource.username=username,\
    app.batch-oai-harvest.spring.datasource.password=password,\
    app.batch-oai-harvest.spring.datasource.hikari.maximumPoolSize=500,\
    app.batch-oai-harvest.spring.jpa.generate-ddl=true,\
    app.batch-oai-harvest.oaiharvest.chunk.size=10,\
    app.batch-oai-harvest.oaiharvest.parallelization.size=10,\
    app.batch-oai-harvest.spring.batch.jdbc.initialize-schema=always" --arguments "datasetId=1 executionId=1 oaiEndpoint=https://metis-repository-rest.test.eanadev.org/repository/oai oaiSet=spring_poc_dataset_with_validation_error oaiMetadataPrefix=edm"
    
task execution list --name batch-oai-harvest
task execution status --id 2 --schemaTarget boot3
task execution cleanup --id 2 --schemaTarget boot3
``` 

# Deploying on an openshift cluster
Deploying it on the openshift has similar approach like on the Kubernetes but with slight differences:
- We need to use openshift variant of the deployment script:  `batch-common/script/docker-build-openshift.sh`
- We need to use openshift variant of the server config yaml:
  - Use `batch-common/server/server-config-openshift.yaml`
  - We assume using external database, so we need not create Postgres container but only set valid url and credentials.
  - Replace <POSTGRES_JDBC_URL> with valid jdbc connection string for your Postgres server.
  - Replace <PASSWORD> with password to your Postgres server
- We need to use openshift variant of deployment yaml: `batch-common/server/server-deployment-openshift.yaml`
- We need to use route instead of ingress:
  - Edit `batch-common/server/server-openshift-route.yaml`
  - Replace <PROJECT_NAME> with valid openshift project name
  - Replace <IP_WHITELIST> with list of IPs from which you want to connect to the SCDF server. 
  - Replace <ENDPOINT_DOMAIN_NAME> with valid name which is a subdomain of your openshift server wildcard domain
  - Apply the configuration on your server.