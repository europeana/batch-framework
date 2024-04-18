package data;

import static data.config.JobParameterConstants.ENRICHMENT_CHUNK_SIZE;
import static data.config.JobParameterConstants.ENRICHMENT_DEREFERENCE_URL;
import static data.config.JobParameterConstants.ENRICHMENT_ENTITY_API_KEY;
import static data.config.JobParameterConstants.ENRICHMENT_ENTITY_API_URL;
import static data.config.JobParameterConstants.ENRICHMENT_ENTITY_MANAGEMENT_URL;
import static data.config.JobParameterConstants.ENRICHMENT_PARALLELIZATION_SIZE;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.cloud.dataflow.schema.AppBootSchemaVersion.BOOT3;

import data.config.MetisDataflowClientConfig;
import data.config.properties.BatchConfigurationProperties;
import data.config.properties.JobConfigurationProperties;
import data.config.properties.RegisterConfigurationProperties;
import data.repositories.ExecutionRecordExceptionLogRepository;
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
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.DetailedAppRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.LaunchResponseResource;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionStatus;
import org.springframework.hateoas.PagedModel;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {MetisDataflowClientConfig.class})
@EnableAutoConfiguration
class ApplicationTestIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  DataFlowOperations dataFlowOperations;
  @Autowired
  BatchConfigurationProperties batchConfigurationProperties;
  @Resource
  ExecutionRecordRepository executionRecordRepository;
  @Resource
  ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository;

  @Test
  void progressCounts() {
    System.out.println(executionRecordRepository.count());
    System.out.println(executionRecordExceptionLogRepository.count());
  }

  @Test
  void registerApplications() {
    final RegisterConfigurationProperties registerProperties = batchConfigurationProperties.getRegisterProperties();
    registerApplication(registerProperties.getOaiHarvestName(), registerProperties.getOaiHarvestUri());
    registerApplication(registerProperties.getValidationName(), registerProperties.getValidationUri());
    registerApplication(registerProperties.getTransformationName(), registerProperties.getTransformationUri());
    registerApplication(registerProperties.getNormalizationName(), registerProperties.getNormalizationUri());
    registerApplication(registerProperties.getEnrichmentName(), registerProperties.getEnrichmentUri());
    registerApplication(registerProperties.getMediaName(), registerProperties.getMediaUri());
  }

  private void registerApplication(String name, String uri) {
    dataFlowOperations.appRegistryOperations()
                      .register(name, ApplicationType.task, uri, "", BOOT3, true);
    final DetailedAppRegistrationResource detailedAppRegistrationResource =
        dataFlowOperations.appRegistryOperations().info(name, ApplicationType.task, false);
    assertEquals(name, detailedAppRegistrationResource.getName());
  }

  @Test
  void unregisterApplications() {
    dataFlowOperations.appRegistryOperations().unregisterAll();
    assertEquals(0, dataFlowOperations.appRegistryOperations().list().getContent().size());
  }

  @Test
  void createTasks() {
    final RegisterConfigurationProperties registerProperties = batchConfigurationProperties.getRegisterProperties();
    final PagedModel<TaskDefinitionResource> taskDefinitionResources = dataFlowOperations.taskOperations().list();
    createTask(taskDefinitionResources, registerProperties.getOaiHarvestName(), registerProperties.getOaiHarvestName());
    createTask(taskDefinitionResources, registerProperties.getValidationName(), registerProperties.getValidationName());
    createTask(taskDefinitionResources, registerProperties.getTransformationName(), registerProperties.getTransformationName());
    createTask(taskDefinitionResources, registerProperties.getNormalizationName(), registerProperties.getNormalizationName());
    createTask(taskDefinitionResources, registerProperties.getEnrichmentName(), registerProperties.getEnrichmentName());
    createTask(taskDefinitionResources, registerProperties.getMediaName(), registerProperties.getMediaName());
  }

  @Test
  void destroyTasks() {
    dataFlowOperations.taskOperations().destroyAll();
    final PagedModel<TaskExecutionResource> taskExecutionResources = dataFlowOperations.taskOperations().executionList();
    for (TaskExecutionResource taskExecutionResource : taskExecutionResources) {
      dataFlowOperations.taskOperations().cleanupAllTaskExecutions(false, taskExecutionResource.getTaskName());
    }
    assertEquals(0, dataFlowOperations.taskOperations().list().getContent().size());
  }

  private void createTask(PagedModel<TaskDefinitionResource> taskDefinitionResources, String name, String definition) {
    //Filter already existing
    for (TaskDefinitionResource taskDefinitionResource : taskDefinitionResources) {
      if (taskDefinitionResource.getName().equals(name)) {
        assertEquals(name, taskDefinitionResource.getName());
        return;
      }
    }
    final TaskDefinitionResource taskDefinitionResource = dataFlowOperations.taskOperations().create(name, definition, "");
    assertEquals(name, taskDefinitionResource.getName());
  }

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
    arguments.add(ARGUMENT_OAI_SET + "=spring_poc_dataset_with_validation_error");
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
    arguments.add(ARGUMENT_EXECUTION_ID + "=86");
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
    arguments.add(ARGUMENT_EXECUTION_ID + "=9");
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
    arguments.add(ARGUMENT_EXECUTION_ID + "=10");
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
    arguments.add(ARGUMENT_EXECUTION_ID + "=11");

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
    arguments.add(ARGUMENT_EXECUTION_ID + "=12");

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
    deployerProperties.put(DEPLOYER_KUBERNETES_LIMITS_MEMORY, "800M");
    deployerProperties.put(DEPLOYER_KUBERNETES_REQUESTS_CPU, "2000m");
    deployerProperties.put(DEPLOYER_KUBERNETES_REQUESTS_MEMORY, "800M");

    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add(ARGUMENT_DATASET_ID + "=1");
    arguments.add(ARGUMENT_EXECUTION_ID + "=13");

    pollingStatus(launchTask(taskName, deployerProperties, additionalAppProperties, arguments));
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
    await().forever().until(() -> {
      final TaskExecutionResource taskExecutionResource = getTaskExecutionResource.get();
      final TaskExecutionStatus taskExecutionStatus = taskExecutionResource.getTaskExecutionStatus();
      final String executionId = Long.toString(taskExecutionResource.getJobExecutionIds().getFirst());
      final String datasetId = getArgumentValue(taskExecutionResource, ARGUMENT_DATASET_ID);
      final String sourceExecutionId = getArgumentValue(taskExecutionResource, ARGUMENT_EXECUTION_ID);

      final long successProcessed = executionRecordRepository.countByExecutionRecordKeyDatasetIdAndExecutionRecordKeyExecutionId(
          datasetId, executionId);
      final long exceptions = executionRecordExceptionLogRepository.countByExecutionRecordKeyDatasetIdAndExecutionRecordKeyExecutionId(
          datasetId, executionId);
      final long processed = successProcessed + exceptions;
      //TODO: 2024-04-18 - Fix this for upcoming OAI changes
      final long sourceTotal = executionRecordRepository.countByExecutionRecordKeyDatasetIdAndExecutionRecordKeyExecutionId(
          datasetId,
          sourceExecutionId);

      LOGGER.info(
          String.format("Task progress - Processed/SourceTotal: %s/%s, Exceptions: %s", processed, sourceTotal, exceptions));
      return taskExecutionStatus == TaskExecutionStatus.COMPLETE || taskExecutionStatus == TaskExecutionStatus.ERROR;
    });
  }

  private static @NotNull String getArgumentValue(TaskExecutionResource taskExecutionResource, String argumentDatasetId) {
    return taskExecutionResource.getArguments().stream().filter(value -> value.contains(argumentDatasetId))
                                .map(value -> value.split("=")[1])
                                .findFirst().orElseThrow();
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

  LaunchResponseResource launchTask(String taskName, Map<String, String> deployerProperties,
      Map<String, String> additionalDeploymentProperties, List<String> arguments) {
    final Map<String, String> deploymentProperties = batchConfigurationProperties.getDeploymentProperties();
    deploymentProperties.putAll(additionalDeploymentProperties);
    final Map<String, String> appPrefixedDeploymentProperties = prefixMap(DEPLOYMENT_PARAMETER_APP_PREFIX, taskName, deploymentProperties);

    final Map<String, String> deployerPrefixedDeploymentProperties = prefixMap(DEPLOYMENT_PARAMETER_DEPLOYER_PREFIX, taskName,
        deployerProperties);

    final Stream<Entry<String, String>> concat = Stream.concat(deployerPrefixedDeploymentProperties.entrySet().stream(),
        appPrefixedDeploymentProperties.entrySet().stream());
    Map<String, String> properties = concat.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return dataFlowOperations.taskOperations().launch(taskName, properties, arguments);
  }

  private Map<String, String> prefixMap(String prefix, String suffix, Map<String, String> map) {
    return TransformedMap.transformedMap(map, key -> format("%s.%s.%s", prefix, suffix, key), value -> value);
  }

  @Test
  void logPresentObjects() {
    final PagedModel<AppRegistrationResource> appRegistrationResources = dataFlowOperations.appRegistryOperations().list();

    LOGGER.info("All Registered Applications:");
    for (AppRegistrationResource appRegistration : appRegistrationResources) {
      LOGGER.info("Application name: {}", appRegistration.getName());
      LOGGER.info("Type: {}", appRegistration.getType());
      LOGGER.info("URI: {}", appRegistration.getUri());
      LOGGER.info("");
    }

    LOGGER.info("All Created Tasks:");
    final PagedModel<TaskDefinitionResource> taskDefinitionResources = dataFlowOperations.taskOperations().list();
    for (TaskDefinitionResource taskDefinitionResource : taskDefinitionResources) {
      LOGGER.info("Task name: {}", taskDefinitionResource.getName());
      LOGGER.info("Description: {}", taskDefinitionResource.getDescription());
      LOGGER.info("DSL: {}", taskDefinitionResource.getDslText());
      LOGGER.info("");
    }
    assertTrue(true);
  }
}
