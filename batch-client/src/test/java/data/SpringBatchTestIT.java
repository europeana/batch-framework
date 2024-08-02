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
import static data.parameter.ArgumentString.ARGUMENT_OVERRIDE_JOB_ID;
import static data.parameter.ArgumentString.ARGUMENT_XSLT_URL;
import static data.parameter.DeploymentString.DEPLOYMENT_PARAMETER_APP_PREFIX;
import static data.parameter.DeploymentString.DEPLOYMENT_PARAMETER_DEPLOYER_PREFIX;
import static java.lang.String.format;
import static org.awaitility.Awaitility.await;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.map.TransformedMap;
import org.apache.commons.lang3.time.StopWatch;
import org.awaitility.core.ConditionFactory;
import org.awaitility.core.ConditionTimeoutException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.client.DataFlowClientException;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.resource.LaunchResponseResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionStatus;
import org.springframework.web.client.RestClientException;
import static eu.europeana.cloud.flink.client.JobExecutor.MAX_RETRIES;
import static eu.europeana.cloud.flink.client.JobExecutor.SLEEP_BETWEEN_RETRIES;

class SpringBatchTestIT extends AbstractPerformanceTest{

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
  void step1_shouldExecuteOAIHarvestCompletellyWithoutErrors() throws InterruptedException {
    beforeEach(1);

    final RegisterConfigurationProperties registerProperties = batchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getOaiHarvestName();
    final JobConfigurationProperties jobProperties = batchConfigurationProperties.getJobProperties();
    final Map<String, String> additionalAppProperties = new HashMap<>();
    additionalAppProperties.put(OAIHARVEST_CHUNK_SIZE, jobProperties.getOaiHarvest().getChunkSize());
    additionalAppProperties.put(OAIHARVEST_PARALLELIZATION_SIZE, jobProperties.getOaiHarvest().getParallelizationSize());

    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add(ARGUMENT_DATASET_ID + "=" + testProperties.getDatasetId());
    arguments.add(ARGUMENT_EXECUTION_ID + "=1");
    arguments.add(ARGUMENT_OVERRIDE_JOB_ID+"=1");

    arguments.add(ARGUMENT_OAI_ENDPOINT + "="+sourceProperties.getUrl());
    arguments.add(ARGUMENT_OAI_SET + "="+sourceProperties.getSetSpec());
    arguments.add(ARGUMENT_METADATA_PREFIX + "="+sourceProperties.getMetadataPrefix());


    pollingStatus(launchTask(taskName, batchConfigurationProperties.getDeployerProperties(), additionalAppProperties, arguments));

    validateResult(1);
  }

  @Test
  void step2_shouldExecuteExternalValidationWithoutErrors() throws InterruptedException {
    beforeEach(2);

    final RegisterConfigurationProperties registerProperties = batchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getValidationName();
    final JobConfigurationProperties jobProperties = batchConfigurationProperties.getJobProperties();
    final Map<String, String> additionalAppProperties = new HashMap<>();
    additionalAppProperties.put(VALIDATION_CHUNK_SIZE, jobProperties.getValidation().getChunkSize());
    additionalAppProperties.put(VALIDATION_PARALLELIZATION_SIZE, jobProperties.getValidation().getParallelizationSize());

    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add(ARGUMENT_DATASET_ID + "=" + testProperties.getDatasetId());
    arguments.add(ARGUMENT_EXECUTION_ID + "=1");
    arguments.add(ARGUMENT_OVERRIDE_JOB_ID+"=2");
    arguments.add(ARGUMENT_BATCH_JOB_SUBTYPE + "=EXTERNAL");

    pollingStatus(launchTask(taskName, batchConfigurationProperties.getDeployerProperties(), additionalAppProperties, arguments));
    validateResult(2);
  }

  @Test
  void step3_shouldExecuteXsltTransformationWithoutErrors() throws InterruptedException {
    beforeEach(3);

    final RegisterConfigurationProperties registerProperties = batchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getTransformationName();
    final JobConfigurationProperties jobProperties = batchConfigurationProperties.getJobProperties();
    final Map<String, String> additionalAppProperties = new HashMap<>();
    additionalAppProperties.put(TRANSFORMATION_CHUNK_SIZE, jobProperties.getTransformation().getChunkSize());
    additionalAppProperties.put(TRANSFORMATION_PARALLELIZATION_SIZE, jobProperties.getTransformation().getParallelizationSize());

    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add(ARGUMENT_DATASET_ID + "=" + testProperties.getDatasetId());
    arguments.add(ARGUMENT_EXECUTION_ID + "=2");
    arguments.add(ARGUMENT_OVERRIDE_JOB_ID+"=3");
    arguments.add(ARGUMENT_DATASET_NAME + "=idA_metisDatasetNameA");
    arguments.add(ARGUMENT_DATASET_COUNTRY + "=Greece");
    arguments.add(ARGUMENT_DATASET_LANGUAGE + "=el");
    arguments.add(ARGUMENT_XSLT_URL + "=https://metis-core-rest.test.eanadev.org/datasets/xslt/6204e5e2514e773e6745f7e9");

    pollingStatus(launchTask(taskName, batchConfigurationProperties.getDeployerProperties(), additionalAppProperties, arguments));
    validateResult(3);
  }

  @Test
  void step4_shouldExecuteIternalValidationWithoutErrors() throws InterruptedException {
    beforeEach(4);

    final RegisterConfigurationProperties registerProperties = batchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getValidationName();
    final JobConfigurationProperties jobProperties = batchConfigurationProperties.getJobProperties();
    final Map<String, String> additionalAppProperties = new HashMap<>();
    additionalAppProperties.put(VALIDATION_CHUNK_SIZE, jobProperties.getValidation().getChunkSize());
    additionalAppProperties.put(VALIDATION_PARALLELIZATION_SIZE, jobProperties.getValidation().getParallelizationSize());

    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add(ARGUMENT_DATASET_ID + "=" + testProperties.getDatasetId());
    arguments.add(ARGUMENT_EXECUTION_ID + "=3");
    arguments.add(ARGUMENT_OVERRIDE_JOB_ID+"=4");
    arguments.add(ARGUMENT_BATCH_JOB_SUBTYPE + "=INTERNAL");

    pollingStatus(launchTask(taskName, batchConfigurationProperties.getDeployerProperties(), additionalAppProperties, arguments));
    validateResult(4);
  }

  @Test
  void step5_shouldExecuteNormalizationWithoutErrors() throws InterruptedException {
    beforeEach(5);

    final RegisterConfigurationProperties registerProperties = batchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getNormalizationName();
    final JobConfigurationProperties jobProperties = batchConfigurationProperties.getJobProperties();
    final Map<String, String> additionalAppProperties = new HashMap<>();
    additionalAppProperties.put(NORMALIZATION_CHUNK_SIZE, jobProperties.getNormalization().getChunkSize());
    additionalAppProperties.put(NORMALIZATION_PARALLELIZATION_SIZE, jobProperties.getNormalization().getParallelizationSize());

    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add(ARGUMENT_DATASET_ID + "=" + testProperties.getDatasetId());
    arguments.add(ARGUMENT_EXECUTION_ID + "=4");
    arguments.add(ARGUMENT_OVERRIDE_JOB_ID+"=5");

    pollingStatus(launchTask(taskName, batchConfigurationProperties.getDeployerProperties(), additionalAppProperties, arguments));

    validateResult(5);
  }

  @Test
  void step6_shouldExecuteEnrichmentWithoutErrors() throws InterruptedException {
    beforeEach(6);

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

    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add(ARGUMENT_DATASET_ID + "=" + testProperties.getDatasetId());
    arguments.add(ARGUMENT_EXECUTION_ID + "=5");
    arguments.add(ARGUMENT_OVERRIDE_JOB_ID+"=6");

    pollingStatus(launchTask(taskName, batchConfigurationProperties.getDeployerProperties(), additionalAppProperties, arguments));

    validateResult(6);
  }

  @Test
  void step7_shouldExecuteMediaWithoutErrors() throws InterruptedException {
    beforeEach(7);

    final RegisterConfigurationProperties registerProperties = batchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getMediaName();
    final JobConfigurationProperties jobProperties = batchConfigurationProperties.getJobProperties();

    final Map<String, String> additionalAppProperties = new HashMap<>();
    additionalAppProperties.put(MEDIA_CHUNK_SIZE, jobProperties.getMedia().getChunkSize());
    additionalAppProperties.put(MEDIA_PARALLELIZATION_SIZE, jobProperties.getMedia().getParallelizationSize());

    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add(ARGUMENT_DATASET_ID + "=" + testProperties.getDatasetId());
    arguments.add(ARGUMENT_EXECUTION_ID + "=6");
    arguments.add(ARGUMENT_OVERRIDE_JOB_ID+"=7");

    pollingStatus(launchTask(taskName, batchConfigurationProperties.getDeployerProperties(), additionalAppProperties, arguments));

    validateResult(7);
  }

  @Test
  void step8_shouldExecuteIndexingWithoutErrors() throws InterruptedException {
    beforeEach(8);

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
    additionalAppProperties.put(INDEXING_ZOOKEEPER_DEFAULT_COLLECTION, jobProperties.getIndexing().getZookeeperDefaultCollection());

    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add(ARGUMENT_DATASET_ID + "=" + testProperties.getDatasetId());
    arguments.add(ARGUMENT_EXECUTION_ID + "=7");
    arguments.add(ARGUMENT_OVERRIDE_JOB_ID+"=8");

    pollingStatus(launchTask(taskName, batchConfigurationProperties.getDeployerProperties(), additionalAppProperties, arguments));

    validateResult(8);
  }

  private void pollingStatus(LaunchResponseResource launchResponseResource) {
    final Supplier<TaskExecutionResource> getTaskExecutionResource = () ->
        dataFlowOperations.taskOperations().taskExecutionStatus(launchResponseResource.getExecutionId(), "boot3");
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
    pollingWithRetryOnException(await().forever(), () -> {
      final TaskExecutionResource taskExecutionResource = getTaskExecutionResource.get();
      final TaskExecutionStatus taskExecutionStatus = taskExecutionResource.getTaskExecutionStatus();
      printProgress(taskExecutionStatus, taskExecutionResource);
      return taskExecutionStatus == TaskExecutionStatus.COMPLETE || taskExecutionStatus == TaskExecutionStatus.ERROR;
    });
  }

  private void pollingWithRetryOnException(ConditionFactory conditionFactory, BooleanSupplier operation) {
    AtomicInteger consecutiveExceptionCount = new AtomicInteger(0);
    conditionFactory.until(() -> {
      try {
        return operation.getAsBoolean();
      } catch (DataFlowClientException | RestClientException e) {
        LOGGER.warn("Exception occurred during polling", e);
        Thread.sleep(SLEEP_BETWEEN_RETRIES);
        if (consecutiveExceptionCount.incrementAndGet() > MAX_RETRIES) {
          throw e;
        } else {
          return false;
        }
      }
    });
  }

  private void printProgress(TaskExecutionStatus taskExecutionStatus, TaskExecutionResource taskExecutionResource) {
    if (taskExecutionStatus != TaskExecutionStatus.ERROR) {
      if (taskExecutionResource.getJobExecutionIds().size() == 0) {
        LOGGER.warn("Task progress - no information, job id is not present!");
        return;
      }
      final String executionId = Long.toString(taskExecutionResource.getJobExecutionIds().getFirst());
      final String datasetId = getArgumentValue(taskExecutionResource, ARGUMENT_DATASET_ID);
      final String sourceExecutionId = getArgumentValue(taskExecutionResource, ARGUMENT_EXECUTION_ID);

      final RegisterConfigurationProperties registerProperties = batchConfigurationProperties.getRegisterProperties();
      final long sourceTotal;
      if (taskExecutionResource.getTaskName().equals(registerProperties.getOaiHarvestName())) {
        sourceTotal = executionRecordExternalIdentifierRepository.countByDatasetIdAndExecutionId(
            datasetId, executionId);
      } else {
        sourceTotal = executionRecordRepository.countByDatasetIdAndExecutionId(
            datasetId, sourceExecutionId);
      }

      final long successProcessed = executionRecordRepository.countByDatasetIdAndExecutionId(
          datasetId, executionId);
      final long exceptions = executionRecordExceptionLogRepository.countByDatasetIdAndExecutionId(
          datasetId, executionId);
      final long processed = successProcessed + exceptions;

      LOGGER.info(format("Task progress - Processed/SourceTotal: %s/%s, Exceptions: %s", processed, sourceTotal, exceptions));
    }
  }

  private static @NotNull String getArgumentValue(TaskExecutionResource taskExecutionResource, String argumentDatasetId) {
    return taskExecutionResource.getArguments().stream().filter(value -> value.contains(argumentDatasetId))
                                .map(value -> value.split("=")[1])
                                .findFirst().orElseThrow();
  }

  private void pollingUnknownDuringPodDeployment(Supplier<TaskExecutionResource> getTaskExecutionResource) {
    //Await for potential UNKNOWN status in case of pod deployment failure.
    try {
      pollingWithRetryOnException(await().atMost(24, TimeUnit.HOURS), () -> {
        TaskExecutionStatus taskExecutionStatus = getTaskExecutionResource.get().getTaskExecutionStatus();
        return taskExecutionStatus != TaskExecutionStatus.UNKNOWN;
      });
    } catch (ConditionTimeoutException ex) {
      LOGGER.error("Launch failed with status: {}", TaskExecutionStatus.UNKNOWN);
      throw ex;
    }
  }

  LaunchResponseResource launchTask(String taskName, Map<String, String> deployerProperties,
      Map<String, String> additionalDeploymentProperties, List<String> arguments) {
    final Map<String, String> deploymentProperties = batchConfigurationProperties.getDeploymentProperties();
    deployerProperties=new LinkedHashMap<>(deployerProperties);
    deploymentProperties.putAll(additionalDeploymentProperties);
    final Map<String, String> appPrefixedDeploymentProperties = prefixMap(DEPLOYMENT_PARAMETER_APP_PREFIX, taskName,
        deploymentProperties);

    final Map<String, String> deployerPrefixedDeploymentProperties = prefixMap(DEPLOYMENT_PARAMETER_DEPLOYER_PREFIX, taskName,
        deployerProperties);

    final Stream<Entry<String, String>> concat = Stream.concat(deployerPrefixedDeploymentProperties.entrySet().stream(),
        appPrefixedDeploymentProperties.entrySet().stream());
    Map<String, String> properties = concat.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    startWatch = StopWatch.createStarted();
    return dataFlowOperations.taskOperations().launch(taskName, properties, arguments);
  }

  private Map<String, String> prefixMap(String prefix, String suffix, Map<String, String> map) {
    return TransformedMap.transformedMap(map, key -> format("%s.%s.%s", prefix, suffix, key), value -> value);
  }
}
