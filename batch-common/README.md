# Deploying Spring Cloud Data Flow in k8s
Update the relevant configuration in the .yml files under the kubernetes directory. 
```console
kubectl create -f kubernetes/postgresql
kubectl create -f kubernetes/server
```

# Removing Spring Cloud Data Flow in k8s
```console
kubectl delete -f kubernetes/postgresql
kubectl delete -f kubernetes/server
```

# Deploying Spring Cloud Data Flow in k8s with PSQL in another location(e.g. IBM)
The postgres service has to already contain the required database for dataflow.
Modify the following:
- The server/server-config.yaml file needs to be updated with credentials
- The server/server-deployment.yaml file needs to be updated to remove the “database” volume and volumeMount
- The client application.properties needs to updated with new values on spring.datasource.x and batch.deploymentProperties.spring.datasource.x

# Build docker images in the minikube registry
When building the docker images from a terminal first run:  
```console
eval $(minikube docker-env)
```

# Run Jobs/Tasks using Spring Cloud Data Flow Shell
Download the script .jar from:  
https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-dataflow-shell/2.11.2/spring-cloud-dataflow-shell-2.11.2.jar  

Get into the shell(Update the uri to the data from server location):
```console
java -jar spring-cloud-dataflow-shell-2.11.2.jar --dataflow.uri=http://scdf-server-192.168.49.2.nip.io:9393  
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
