# <p style="text-align: center;">Spring Cloud Data Flow for Metis</p>

<br/><br/><br/>
<p align="center">
  <img style="margin-right: 30px" src="https://igcz.poznan.pl/wp-content/uploads/2016/01/PCSS-logo-300x110.png" />
  <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/4/49/Europeana_logo_black.svg/361px-Europeana_logo_black.svg.png" />
</p>

<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>

<hr style="height: 40px"/>

## <p style="text-align: center;">First layer (jvm and Spring)</p>

<br/>

- **Spring Framework** as a backbone of our processing engine;
- Separate application for each job (workflow step execution);
- Standalone/console applications, without any (rest) interface;
- Applications executed on demand and exclusively for only one, particular task (workflow step execution):
  - no idle processes/applications running in the background and waiting for some commands to be executed
  - full isolation between the tasks/jobs (not counting database and runtime environment - k8s)

<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>
<hr style="height: 40px"/>

## <p style="text-align: center;">Second layer (String Batch)</p>

- framework for processing of large volumes of information without user interaction:
- it is not a scheduling framework:
- provides: transactions management, job statistics, job restarts, ...
- typical use-case scenario:
    - read data from source (db, file, ...)
    - process data
    - write modified data to sink (db, file, ...)
- framework structure:
  - ![alt text](https://docs.spring.io/spring-batch/reference/_images/spring-batch-reference-model.png)
- main building blocks:
  - jobs, 
  - steps, 
    - readers, 
    - processors, 
    - writers;
    
- by definition used for short living applications

<br/><br/><br/><br/>
<hr style="height: 40px"/>

## <p style="text-align: center;">Third layer (String Cloud Task)</p>

- similar to Spring Batch but more general;
- allows a user to develop and run short lived micro-services using Spring Cloud and run them locally, in the cloud, or on Spring Cloud Data Flow
- integrates smoothly with Spring Batch;
  
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>
<hr style="height: 40px"/>

## <p style="text-align: center;">Fourth layer (Spring Cloud Data Flow)</p>
- used for orchestrating the tasks and maybe used as runtime environment for Spring Batch Jobs;
- can be installed on k8s, CloudFoundry, local;
- allows to run jobs on different environments: k8s, CloudFoundry, local;
- allows to chain multiple tasks together;
- exposes Rest API that can be used via:
  - web-browser
  - command line tool;
  - **java client**
- typical usage scenario for SCDF:
  - application(s) registration
  - creation of the pipeline if needed: multiple tasks/streams chained together
    - run the application(s) on demand: code + configuration
- Properties:
  - Deployer Properties: These properties customize how tasks are launched:
    - cpu/memory requests/limits;
    - Liveness, Readiness and Startup Probes;
  - Application Properties: These are application-specific properties;

<br/><br/><br/><br/><br/><br/><br/>
<hr style="height: 40px"/>

## <p style="text-align: center;">Typical development process</p>

- modify source code
- build application
- run/test locally
- prepare docker image and push it to repo
- register application in SCDF:

```console
app register --name batch-oai-harvest --type task --bootVersion 3 --uri docker:registry.paas.psnc.pl/europeana-cloud/batch-oai-harvest:latest
```
- run the task using SCDF infrastructure:

```console
task launch --name batch-oai-harvest --properties "\
    app.batch-oai-harvest.spring.datasource.url=jdbc:postgresql://postgresql.ecloud-spring-cdf-poc.svc.cluster.local:5432/dataflow?useSSL=false,\
    app.batch-oai-harvest.spring.datasource.driver-class-name=org.postgresql.Driver,\
    app.batch-oai-harvest.spring.datasource.username=username,\
    app.batch-oai-harvest.spring.datasource.password=password,\
    app.batch-oai-harvest.spring.datasource.hikari.maximumPoolSize=500,\
    app.batch-oai-harvest.spring.jpa.generate-ddl=true,\
    app.batch-oai-harvest.oaiharvest.chunk.size=10,\
    app.batch-oai-harvest.oaiharvest.parallelization.size=10,\
    app.batch-oai-harvest.spring.batch.jdbc.initialize-schema=always" --arguments "datasetId=1 executionId=1 oaiEndpoint=https://metis-repository-rest.test.eanadev.org/repository/oai oaiSet=spring_poc_dataset_with_validation_error oaiMetadataPrefix=edm"
```
<br/><br/>
<hr style="height: 40px"/>

## <p style="text-align: center;">QUESTIONS</p>
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>

---

## References

https://docs.spring.io/spring-batch/reference/index.html

https://dataflow.spring.io/getting-started/

