package data;

import static data.config.JobParameterConstants.ENRICHMENT_CHUNK_SIZE;
import static data.config.JobParameterConstants.ENRICHMENT_DEREFERENCE_URL;
import static data.config.JobParameterConstants.ENRICHMENT_ENTITY_API_KEY;
import static data.config.JobParameterConstants.ENRICHMENT_ENTITY_API_URL;
import static data.config.JobParameterConstants.ENRICHMENT_ENTITY_MANAGEMENT_URL;
import static data.config.JobParameterConstants.ENRICHMENT_PARALLELIZATION_SIZE;
import static data.config.JobParameterConstants.INDEXING_CHUNK_SIZE;
import static data.config.JobParameterConstants.INDEXING_MONGO_APPLICATION_NAME;
import static data.config.JobParameterConstants.INDEXING_MONGO_AUTH_DB;
import static data.config.JobParameterConstants.INDEXING_MONGO_DB_NAME;
import static data.config.JobParameterConstants.INDEXING_MONGO_INSTANCES;
import static data.config.JobParameterConstants.INDEXING_MONGO_PASSWORD;
import static data.config.JobParameterConstants.INDEXING_MONGO_POOL_SIZE;
import static data.config.JobParameterConstants.INDEXING_MONGO_PORT_NUMBER;
import static data.config.JobParameterConstants.INDEXING_MONGO_READ_PREFERENCE;
import static data.config.JobParameterConstants.INDEXING_MONGO_REDIRECTS_DB_NAME;
import static data.config.JobParameterConstants.INDEXING_MONGO_USERNAME;
import static data.config.JobParameterConstants.INDEXING_MONGO_USE_SSL;
import static data.config.JobParameterConstants.INDEXING_PARALLELIZATION_SIZE;
import static data.config.JobParameterConstants.INDEXING_PERFORM_REDIRECTS;
import static data.config.JobParameterConstants.INDEXING_PRESERVE_TIMESTAMPS;
import static data.config.JobParameterConstants.INDEXING_SOLR_INSTANCES;
import static data.config.JobParameterConstants.INDEXING_ZOOKEEPER_CHROOT;
import static data.config.JobParameterConstants.INDEXING_ZOOKEEPER_DEFAULT_COLLECTION;
import static data.config.JobParameterConstants.INDEXING_ZOOKEEPER_INSTANCES;
import static data.config.JobParameterConstants.INDEXING_ZOOKEEPER_PORT_NUMBER;
import static data.config.JobParameterConstants.MEDIA_CHUNK_SIZE;
import static data.config.JobParameterConstants.MEDIA_PARALLELIZATION_SIZE;
import static data.config.JobParameterConstants.NORMALIZATION_CHUNK_SIZE;
import static data.config.JobParameterConstants.NORMALIZATION_PARALLELIZATION_SIZE;
import static data.config.JobParameterConstants.OAIHARVEST_CHUNK_SIZE;
import static data.config.JobParameterConstants.OAIHARVEST_PARALLELIZATION_SIZE;
import static data.config.JobParameterConstants.TRANSFORMATION_CHUNK_SIZE;
import static data.config.JobParameterConstants.TRANSFORMATION_PARALLELIZATION_SIZE;
import static data.config.JobParameterConstants.VALIDATION_CHUNK_SIZE;
import static data.config.JobParameterConstants.VALIDATION_PARALLELIZATION_SIZE;
import static data.parameter.ArgumentString.ARGUMENT_BATCH_JOB_SUBTYPE;
import static data.parameter.ArgumentString.ARGUMENT_DATASET_COUNTRY;
import static data.parameter.ArgumentString.ARGUMENT_DATASET_ID;
import static data.parameter.ArgumentString.ARGUMENT_DATASET_LANGUAGE;
import static data.parameter.ArgumentString.ARGUMENT_DATASET_NAME;
import static data.parameter.ArgumentString.ARGUMENT_EXECUTION_ID;
import static data.parameter.ArgumentString.ARGUMENT_METADATA_PREFIX;
import static data.parameter.ArgumentString.ARGUMENT_OAI_ENDPOINT;
import static data.parameter.ArgumentString.ARGUMENT_OAI_SET;
import static data.parameter.ArgumentString.ARGUMENT_XSLT_URL;
import static data.parameter.DeployerString.DEPLOYER_KUBERNETES_LIMITS_CPU;
import static data.parameter.DeployerString.DEPLOYER_KUBERNETES_LIMITS_MEMORY;
import static data.parameter.DeployerString.DEPLOYER_KUBERNETES_REQUESTS_CPU;
import static data.parameter.DeployerString.DEPLOYER_KUBERNETES_REQUESTS_MEMORY;
import static data.parameter.DeploymentString.DEPLOYMENT_PARAMETER_APP_PREFIX;
import static data.parameter.DeploymentString.DEPLOYMENT_PARAMETER_DEPLOYER_PREFIX;
import static java.lang.String.format;
import static org.awaitility.Awaitility.await;

import data.config.MetisDataflowClientConfig;
import data.config.properties.BatchConfigurationProperties;
import data.config.properties.JobConfigurationProperties;
import data.config.properties.RegisterConfigurationProperties;
import data.entity.ExecutionRecord;
import data.repositories.ExecutionRecordExceptionLogRepository;
import data.repositories.ExecutionRecordExternalIdentifierRepository;
import data.repositories.ExecutionRecordRepository;
import jakarta.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.map.TransformedMap;
import org.awaitility.core.ConditionTimeoutException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.resource.LaunchResponseResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionStatus;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {MetisDataflowClientConfig.class})
@EnableAutoConfiguration
class ApplicationTestIT {

  public static final String SCHEMA_TARGET = "boot3";
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  DataFlowOperations dataFlowOperations;

  @Autowired
  BatchConfigurationProperties batchConfigurationProperties;
  @Resource
  ExecutionRecordRepository<ExecutionRecord> executionRecordRepository;
  @Resource
  ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository;
  @Resource
  ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository;

  @Test
  void launchOaiTask() {
    final RegisterConfigurationProperties registerProperties = batchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getOaiHarvestName();
    final JobConfigurationProperties jobProperties = batchConfigurationProperties.getJobProperties();
    final Map<String, String> additionalAppProperties = new HashMap<>();
    additionalAppProperties.put(OAIHARVEST_CHUNK_SIZE, jobProperties.getOaiHarvest().getChunkSize());
    additionalAppProperties.put(OAIHARVEST_PARALLELIZATION_SIZE, jobProperties.getOaiHarvest().getParallelizationSize());

    final Map<String, String> deployerProperties = new HashMap<>();
    deployerProperties.put(DEPLOYER_KUBERNETES_LIMITS_CPU, "2000m");
    deployerProperties.put(DEPLOYER_KUBERNETES_LIMITS_MEMORY, "800M");
    deployerProperties.put(DEPLOYER_KUBERNETES_REQUESTS_CPU, "2000m");
    deployerProperties.put(DEPLOYER_KUBERNETES_REQUESTS_MEMORY, "800M");

    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add(ARGUMENT_DATASET_ID + "=1");
    arguments.add(ARGUMENT_EXECUTION_ID + "=1");
    arguments.add(ARGUMENT_OAI_ENDPOINT + "=https://metis-repository-rest.test.eanadev.org/repository/oai");
    arguments.add(ARGUMENT_OAI_SET + "=oai_flink_poc");
    arguments.add(ARGUMENT_METADATA_PREFIX + "=edm");

    pollingStatus(launchTask(taskName, deployerProperties, additionalAppProperties, arguments));
  }

  @Test
  void launchValidationExternalTask() {
    final RegisterConfigurationProperties registerProperties = batchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getValidationName();
    final JobConfigurationProperties jobProperties = batchConfigurationProperties.getJobProperties();
    final Map<String, String> additionalAppProperties = new HashMap<>();
    additionalAppProperties.put(VALIDATION_CHUNK_SIZE, jobProperties.getValidation().getChunkSize());
    additionalAppProperties.put(VALIDATION_PARALLELIZATION_SIZE, jobProperties.getValidation().getParallelizationSize());

    final Map<String, String> deployerProperties = new HashMap<>();
    deployerProperties.put(DEPLOYER_KUBERNETES_LIMITS_CPU, "2000m");
    deployerProperties.put(DEPLOYER_KUBERNETES_LIMITS_MEMORY, "800M");
    deployerProperties.put(DEPLOYER_KUBERNETES_REQUESTS_CPU, "2000m");
    deployerProperties.put(DEPLOYER_KUBERNETES_REQUESTS_MEMORY, "800M");

    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add(ARGUMENT_DATASET_ID + "=1");
    arguments.add(ARGUMENT_EXECUTION_ID + "=37");
    arguments.add(ARGUMENT_BATCH_JOB_SUBTYPE + "=EXTERNAL");

    pollingStatus(launchTask(taskName, deployerProperties, additionalAppProperties, arguments));
  }

  @Test
  void launchTransformationTask() {
    final RegisterConfigurationProperties registerProperties = batchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getTransformationName();
    final JobConfigurationProperties jobProperties = batchConfigurationProperties.getJobProperties();
    final Map<String, String> additionalAppProperties = new HashMap<>();
    additionalAppProperties.put(TRANSFORMATION_CHUNK_SIZE, jobProperties.getTransformation().getChunkSize());
    additionalAppProperties.put(TRANSFORMATION_PARALLELIZATION_SIZE, jobProperties.getTransformation().getParallelizationSize());

    final Map<String, String> deployerProperties = new HashMap<>();
    deployerProperties.put(DEPLOYER_KUBERNETES_LIMITS_CPU, "2000m");
    deployerProperties.put(DEPLOYER_KUBERNETES_LIMITS_MEMORY, "800M");
    deployerProperties.put(DEPLOYER_KUBERNETES_REQUESTS_CPU, "2000m");
    deployerProperties.put(DEPLOYER_KUBERNETES_REQUESTS_MEMORY, "800M");

    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add(ARGUMENT_DATASET_ID + "=1");
    arguments.add(ARGUMENT_EXECUTION_ID + "=2");
    arguments.add(ARGUMENT_DATASET_NAME + "=idA_metisDatasetNameA");
    arguments.add(ARGUMENT_DATASET_COUNTRY + "=Greece");
    arguments.add(ARGUMENT_DATASET_LANGUAGE + "=el");
    arguments.add(ARGUMENT_XSLT_URL + "=https://metis-core-rest.test.eanadev.org/datasets/xslt/6204e5e2514e773e6745f7e9");

    pollingStatus(launchTask(taskName, deployerProperties, additionalAppProperties, arguments));
  }

  @Test
  void launchValidationInternalTask() {
    final RegisterConfigurationProperties registerProperties = batchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getValidationName();
    final JobConfigurationProperties jobProperties = batchConfigurationProperties.getJobProperties();
    final Map<String, String> additionalAppProperties = new HashMap<>();
    additionalAppProperties.put(VALIDATION_CHUNK_SIZE, jobProperties.getValidation().getChunkSize());
    additionalAppProperties.put(VALIDATION_PARALLELIZATION_SIZE, jobProperties.getValidation().getParallelizationSize());

    final Map<String, String> deployerProperties = new HashMap<>();
    deployerProperties.put(DEPLOYER_KUBERNETES_LIMITS_CPU, "2000m");
    deployerProperties.put(DEPLOYER_KUBERNETES_LIMITS_MEMORY, "800M");
    deployerProperties.put(DEPLOYER_KUBERNETES_REQUESTS_CPU, "2000m");
    deployerProperties.put(DEPLOYER_KUBERNETES_REQUESTS_MEMORY, "800M");

    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add(ARGUMENT_DATASET_ID + "=1");
    arguments.add(ARGUMENT_EXECUTION_ID + "=278");
    arguments.add(ARGUMENT_BATCH_JOB_SUBTYPE + "=INTERNAL");

    pollingStatus(launchTask(taskName, deployerProperties, additionalAppProperties, arguments));
  }

  @Test
  void launchNormalizationTask() {
    final RegisterConfigurationProperties registerProperties = batchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getNormalizationName();
    final JobConfigurationProperties jobProperties = batchConfigurationProperties.getJobProperties();
    final Map<String, String> additionalAppProperties = new HashMap<>();
    additionalAppProperties.put(NORMALIZATION_CHUNK_SIZE, jobProperties.getNormalization().getChunkSize());
    additionalAppProperties.put(NORMALIZATION_PARALLELIZATION_SIZE, jobProperties.getNormalization().getParallelizationSize());

    final Map<String, String> deployerProperties = new HashMap<>();
    deployerProperties.put(DEPLOYER_KUBERNETES_LIMITS_CPU, "2000m");
    deployerProperties.put(DEPLOYER_KUBERNETES_LIMITS_MEMORY, "800M");
    deployerProperties.put(DEPLOYER_KUBERNETES_REQUESTS_CPU, "2000m");
    deployerProperties.put(DEPLOYER_KUBERNETES_REQUESTS_MEMORY, "800M");

    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add(ARGUMENT_DATASET_ID + "=1");
    arguments.add(ARGUMENT_EXECUTION_ID + "=279");

    pollingStatus(launchTask(taskName, deployerProperties, additionalAppProperties, arguments));
  }

  @Test
  void launchEnrichmentTask() {
    final RegisterConfigurationProperties registerProperties = batchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getEnrichmentName();
    final JobConfigurationProperties jobProperties = batchConfigurationProperties.getJobProperties();
    final Map<String, String> additionalAppProperties = new HashMap<>();
    additionalAppProperties.put(ENRICHMENT_CHUNK_SIZE, jobProperties.getEnrichment().getChunkSize());
    additionalAppProperties.put(ENRICHMENT_PARALLELIZATION_SIZE, jobProperties.getEnrichment().getParallelizationSize());
    additionalAppProperties.put(ENRICHMENT_DEREFERENCE_URL, jobProperties.getEnrichment().getDereferenceUrl());
    additionalAppProperties.put(ENRICHMENT_ENTITY_MANAGEMENT_URL, jobProperties.getEnrichment().getEntityManagementUrl());
    additionalAppProperties.put(ENRICHMENT_ENTITY_API_URL, jobProperties.getEnrichment().getEntityApiUrl());
    additionalAppProperties.put(ENRICHMENT_ENTITY_API_KEY, jobProperties.getEnrichment().getEntityApiKey());

    final Map<String, String> deployerProperties = new HashMap<>();
    deployerProperties.put(DEPLOYER_KUBERNETES_LIMITS_CPU, "2000m");
    deployerProperties.put(DEPLOYER_KUBERNETES_LIMITS_MEMORY, "800M");
    deployerProperties.put(DEPLOYER_KUBERNETES_REQUESTS_CPU, "2000m");
    deployerProperties.put(DEPLOYER_KUBERNETES_REQUESTS_MEMORY, "800M");

    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add(ARGUMENT_DATASET_ID + "=1");
    arguments.add(ARGUMENT_EXECUTION_ID + "=280");

    pollingStatus(launchTask(taskName, deployerProperties, additionalAppProperties, arguments));
  }

  @Test
  void launchMediaTask() {
    final RegisterConfigurationProperties registerProperties = batchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getMediaName();
    final JobConfigurationProperties jobProperties = batchConfigurationProperties.getJobProperties();

    final Map<String, String> additionalAppProperties = new HashMap<>();
    additionalAppProperties.put(MEDIA_CHUNK_SIZE, jobProperties.getMedia().getChunkSize());
    additionalAppProperties.put(MEDIA_PARALLELIZATION_SIZE, jobProperties.getMedia().getParallelizationSize());

    final Map<String, String> deployerProperties = new HashMap<>();
    deployerProperties.put(DEPLOYER_KUBERNETES_LIMITS_CPU, "2000m");
    deployerProperties.put(DEPLOYER_KUBERNETES_LIMITS_MEMORY, "1500M");
    deployerProperties.put(DEPLOYER_KUBERNETES_REQUESTS_CPU, "2000m");
    deployerProperties.put(DEPLOYER_KUBERNETES_REQUESTS_MEMORY, "1500M");

    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add(ARGUMENT_DATASET_ID + "=1");
    arguments.add(ARGUMENT_EXECUTION_ID + "=288");

    pollingStatus(launchTask(taskName, deployerProperties, additionalAppProperties, arguments));
  }

  @Test
  void launchIndexTask() {
    final RegisterConfigurationProperties registerProperties = batchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getIndexingName();
    final JobConfigurationProperties jobProperties = batchConfigurationProperties.getJobProperties();

    final Map<String, String> additionalAppProperties = new HashMap<>();
    additionalAppProperties.put(INDEXING_CHUNK_SIZE, jobProperties.getIndexing().getChunkSize());
    additionalAppProperties.put(INDEXING_PARALLELIZATION_SIZE, jobProperties.getIndexing().getParallelizationSize());

    additionalAppProperties.put(INDEXING_PRESERVE_TIMESTAMPS, jobProperties.getIndexing().getPreserveTimestamps());
    additionalAppProperties.put(INDEXING_PERFORM_REDIRECTS, jobProperties.getIndexing().getPerformRedirects());

    additionalAppProperties.put(INDEXING_MONGO_INSTANCES, jobProperties.getIndexing().getMongoInstances());
    additionalAppProperties.put(INDEXING_MONGO_PORT_NUMBER, jobProperties.getIndexing().getMongoPortNumber());
    additionalAppProperties.put(INDEXING_MONGO_DB_NAME, jobProperties.getIndexing().getMongoDbName());
    additionalAppProperties.put(INDEXING_MONGO_REDIRECTS_DB_NAME, jobProperties.getIndexing().getMongoRedirectsDbName());
    additionalAppProperties.put(INDEXING_MONGO_USERNAME, jobProperties.getIndexing().getMongoUsername());
    additionalAppProperties.put(INDEXING_MONGO_PASSWORD, jobProperties.getIndexing().getMongoPassword());
    additionalAppProperties.put(INDEXING_MONGO_AUTH_DB, jobProperties.getIndexing().getMongoAuthDB());
    additionalAppProperties.put(INDEXING_MONGO_USE_SSL, jobProperties.getIndexing().getMongoUseSSL());
    additionalAppProperties.put(INDEXING_MONGO_READ_PREFERENCE, jobProperties.getIndexing().getMongoReadPreference());
    additionalAppProperties.put(INDEXING_MONGO_POOL_SIZE, jobProperties.getIndexing().getMongoPoolSize());
    additionalAppProperties.put(INDEXING_MONGO_APPLICATION_NAME, jobProperties.getIndexing().getMongoApplicationName());

    additionalAppProperties.put(INDEXING_SOLR_INSTANCES, jobProperties.getIndexing().getSolrInstances());
    additionalAppProperties.put(INDEXING_ZOOKEEPER_INSTANCES, jobProperties.getIndexing().getZookeeperInstances());
    additionalAppProperties.put(INDEXING_ZOOKEEPER_PORT_NUMBER, jobProperties.getIndexing().getZookeeperPortNumber());
    additionalAppProperties.put(INDEXING_ZOOKEEPER_CHROOT, jobProperties.getIndexing().getZookeeperChroot());
    additionalAppProperties.put(INDEXING_ZOOKEEPER_DEFAULT_COLLECTION,
        jobProperties.getIndexing().getZookeeperDefaultCollection());

    final Map<String, String> deployerProperties = new HashMap<>();
    deployerProperties.put(DEPLOYER_KUBERNETES_LIMITS_CPU, "2000m");
    deployerProperties.put(DEPLOYER_KUBERNETES_LIMITS_MEMORY, "800M");
    deployerProperties.put(DEPLOYER_KUBERNETES_REQUESTS_CPU, "2000m");
    deployerProperties.put(DEPLOYER_KUBERNETES_REQUESTS_MEMORY, "800M");

    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add(ARGUMENT_DATASET_ID + "=1");
    arguments.add(ARGUMENT_EXECUTION_ID + "=289");

    pollingStatus(launchTask(taskName, deployerProperties, additionalAppProperties, arguments));
  }

  LaunchResponseResource launchTask(String taskName, Map<String, String> deployerProperties,
      Map<String, String> additionalDeploymentProperties, List<String> arguments) {
    final Map<String, String> deploymentProperties = batchConfigurationProperties.getDeploymentProperties();
    deploymentProperties.putAll(additionalDeploymentProperties);
    final Map<String, String> appPrefixedDeploymentProperties = prefixMap(DEPLOYMENT_PARAMETER_APP_PREFIX, taskName,
        deploymentProperties);

    final Map<String, String> deployerPrefixedDeploymentProperties = prefixMap(DEPLOYMENT_PARAMETER_DEPLOYER_PREFIX, taskName,
        deployerProperties);

    final Stream<Entry<String, String>> concat = Stream.concat(deployerPrefixedDeploymentProperties.entrySet().stream(),
        appPrefixedDeploymentProperties.entrySet().stream());
    Map<String, String> properties = concat.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return dataFlowOperations.taskOperations().launch(taskName, properties, arguments);
  }

  private static @NotNull String getArgumentValue(TaskExecutionResource taskExecutionResource, String argumentDatasetId) {
    return taskExecutionResource.getArguments().stream().filter(value -> value.contains(argumentDatasetId))
                                .map(value -> value.split("=")[1])
                                .findFirst().orElseThrow();
  }

  private void pollingStatus(LaunchResponseResource launchResponseResource) {
    final Supplier<TaskExecutionResource> getTaskExecutionResource = () ->
        dataFlowOperations.taskOperations().taskExecutionStatus(launchResponseResource.getExecutionId(), SCHEMA_TARGET);
    TaskExecutionResource taskExecutionResource = getTaskExecutionResource.get();
    LOGGER.info("Task launched with taskId: {}", taskExecutionResource.getExecutionId());
    pollingUnknownDuringPodDeployment(getTaskExecutionResource);
    pollingRunning(getTaskExecutionResource);
    taskExecutionResource = getTaskExecutionResource.get();
    LOGGER.info("Task finished with details:");
    LOGGER.info("Task finished with jobIds: {}, taskId: {}, status: {}, startTime: {}, endTime: {}",
        taskExecutionResource.getJobExecutionIds(), taskExecutionResource.getExecutionId(),
        taskExecutionResource.getTaskExecutionStatus(), taskExecutionResource.getStartTime(), taskExecutionResource.getEndTime());
  }

  private void pollingRunning(Supplier<TaskExecutionResource> getTaskExecutionResource) {
    await().forever().until(() -> {
      final TaskExecutionResource taskExecutionResource = getTaskExecutionResource.get();
      final TaskExecutionStatus taskExecutionStatus = taskExecutionResource.getTaskExecutionStatus();
      printProgress(taskExecutionStatus, taskExecutionResource);
      return taskExecutionStatus == TaskExecutionStatus.COMPLETE || taskExecutionStatus == TaskExecutionStatus.ERROR;
    });
  }

  private void printProgress(TaskExecutionStatus taskExecutionStatus, TaskExecutionResource taskExecutionResource) {
    if (taskExecutionStatus != TaskExecutionStatus.ERROR) {
      final String executionId = Long.toString(taskExecutionResource.getJobExecutionIds().getFirst());
      final String instanceId = Long.toString(
          dataFlowOperations.jobOperations().jobExecution(Long.parseLong(executionId), SCHEMA_TARGET).getJobId());
      final String datasetId = getArgumentValue(taskExecutionResource, ARGUMENT_DATASET_ID);
      final String sourceExecutionId = getArgumentValue(taskExecutionResource, ARGUMENT_EXECUTION_ID);

      final RegisterConfigurationProperties registerProperties = batchConfigurationProperties.getRegisterProperties();
      final long sourceTotal;
      if (taskExecutionResource.getTaskName().equals(registerProperties.getOaiHarvestName())) {
        sourceTotal = executionRecordExternalIdentifierRepository.countByDatasetIdAndExecutionId(datasetId, instanceId);
      } else {
        sourceTotal = executionRecordRepository.countByDatasetIdAndExecutionId(datasetId, sourceExecutionId);
      }

      final long successProcessed = executionRecordRepository.countByDatasetIdAndExecutionId(datasetId, instanceId);
      final long exceptions = executionRecordExceptionLogRepository.countByDatasetIdAndExecutionId(datasetId, instanceId);
      final long processed = successProcessed + exceptions;

      LOGGER.info(format("Task progress - Processed/SourceTotal: %s/%s, Exceptions: %s", processed, sourceTotal, exceptions));
    }
  }

  private void pollingUnknownDuringPodDeployment(Supplier<TaskExecutionResource> getTaskExecutionResource) {
    //Await for potential UNKNOWN status in case of pod deployment failure.
    try {
      await().atMost(1, TimeUnit.MINUTES).until(() -> {
        TaskExecutionStatus taskExecutionStatus = getTaskExecutionResource.get().getTaskExecutionStatus();
        return taskExecutionStatus != TaskExecutionStatus.UNKNOWN;
      });
    } catch (ConditionTimeoutException ex) {
      LOGGER.error("Launch failed with status: {}", TaskExecutionStatus.UNKNOWN);
      throw ex;
    }
  }

  private Map<String, String> prefixMap(String prefix, String suffix, Map<String, String> map) {
    return TransformedMap.transformedMap(map, key -> format("%s.%s.%s", prefix, suffix, key), value -> value);
  }
}
