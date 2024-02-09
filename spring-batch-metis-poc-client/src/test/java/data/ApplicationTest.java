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
import static java.lang.String.format;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.cloud.dataflow.schema.AppBootSchemaVersion.BOOT3;

import data.config.MetisDataflowClientConfig;
import data.config.properties.JobConfigurationProperties;
import data.config.properties.MetisBatchConfigurationProperties;
import data.config.properties.RegisterConfigurationProperties;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.apache.commons.collections4.map.TransformedMap;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
class ApplicationTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @Autowired
  DataFlowOperations dataFlowOperations;
  @Autowired
  MetisBatchConfigurationProperties metisBatchConfigurationProperties;

  @Test
  void registerApplications() {
    final RegisterConfigurationProperties registerProperties = metisBatchConfigurationProperties.getRegisterProperties();
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
    final RegisterConfigurationProperties registerProperties = metisBatchConfigurationProperties.getRegisterProperties();
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
    final RegisterConfigurationProperties registerProperties = metisBatchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getOaiHarvestName();
    final JobConfigurationProperties jobProperties = metisBatchConfigurationProperties.getJobProperties();
    final Map<String, String> deploymentProperties = new HashMap<>();
    deploymentProperties.put(OAIHARVEST_CHUNK_SIZE, jobProperties.getOaiHarvest().getChunkSize());
    deploymentProperties.put(OAIHARVEST_PARALLELIZATION_SIZE, jobProperties.getOaiHarvest().getParallelizationSize());
    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add("datasetId=1");
    arguments.add("executionId=1");
    arguments.add("targetJob=OAI_HARVEST");
    arguments.add("oaiEndpoint=https://metis-repository-rest.test.eanadev.org/repository/oai");
    arguments.add("oaiSet=spring_poc_dataset_with_validation_error");
    arguments.add("oaiMetadataPrefix=edm");

    polingStatus(launchTask(taskName, deploymentProperties, arguments));
  }

  @Test
  void launchValidationExternalTask() {
    final RegisterConfigurationProperties registerProperties = metisBatchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getValidationName();
    final JobConfigurationProperties jobProperties = metisBatchConfigurationProperties.getJobProperties();
    final Map<String, String> deploymentProperties = new HashMap<>();
    deploymentProperties.put(VALIDATION_CHUNK_SIZE, jobProperties.getValidation().getChunkSize());
    deploymentProperties.put(VALIDATION_PARALLELIZATION_SIZE, jobProperties.getValidation().getParallelizationSize());
    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add("datasetId=1");
    arguments.add("executionId=2");
    arguments.add("targetJob=VALIDATION_EXTERNAL");

    polingStatus(launchTask(taskName, deploymentProperties, arguments));
  }

  @Test
  void launchTransformationTask() {
    final RegisterConfigurationProperties registerProperties = metisBatchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getTransformationName();
    final JobConfigurationProperties jobProperties = metisBatchConfigurationProperties.getJobProperties();
    final Map<String, String> deploymentProperties = new HashMap<>();
    deploymentProperties.put(TRANSFORMATION_CHUNK_SIZE, jobProperties.getTransformation().getChunkSize());
    deploymentProperties.put(TRANSFORMATION_PARALLELIZATION_SIZE, jobProperties.getTransformation().getParallelizationSize());
    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add("datasetId=1");
    arguments.add("executionId=25");
    arguments.add("targetJob=TRANSFORMATION");
    arguments.add("datasetName=idA_metisDatasetNameA");
    arguments.add("datasetCountry=Greece");
    arguments.add("datasetLanguage=el");
    arguments.add("xsltUrl=https://metis-core-rest.test.eanadev.org/datasets/xslt/6204e5e2514e773e6745f7e9");

    polingStatus(launchTask(taskName, deploymentProperties, arguments));
  }

  @Test
  void launchValidationInternalTask() {
    final RegisterConfigurationProperties registerProperties = metisBatchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getValidationName();
    final JobConfigurationProperties jobProperties = metisBatchConfigurationProperties.getJobProperties();
    final Map<String, String> deploymentProperties = new HashMap<>();
    deploymentProperties.put(VALIDATION_CHUNK_SIZE, jobProperties.getValidation().getChunkSize());
    deploymentProperties.put(VALIDATION_PARALLELIZATION_SIZE, jobProperties.getValidation().getParallelizationSize());
    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add("datasetId=1");
    arguments.add("executionId=36");
    arguments.add("targetJob=VALIDATION_INTERNAL");

    polingStatus(launchTask(taskName, deploymentProperties, arguments));
  }

  @Test
  void launchNormalizationTask() {
    final RegisterConfigurationProperties registerProperties = metisBatchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getNormalizationName();
    final JobConfigurationProperties jobProperties = metisBatchConfigurationProperties.getJobProperties();
    final Map<String, String> deploymentProperties = new HashMap<>();
    deploymentProperties.put(NORMALIZATION_CHUNK_SIZE, jobProperties.getNormalization().getChunkSize());
    deploymentProperties.put(NORMALIZATION_PARALLELIZATION_SIZE, jobProperties.getNormalization().getParallelizationSize());
    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add("datasetId=1");
    arguments.add("executionId=37");
    arguments.add("targetJob=NORMALIZATION");

    polingStatus(launchTask(taskName, deploymentProperties, arguments));
  }

  @Test
  void launchEnrichmentTask() {
    final RegisterConfigurationProperties registerProperties = metisBatchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getEnrichmentName();
    final JobConfigurationProperties jobProperties = metisBatchConfigurationProperties.getJobProperties();
    final Map<String, String> deploymentProperties = new HashMap<>();
    deploymentProperties.put(ENRICHMENT_CHUNK_SIZE, jobProperties.getEnrichment().getChunkSize());
    deploymentProperties.put(ENRICHMENT_PARALLELIZATION_SIZE, jobProperties.getEnrichment().getParallelizationSize());
    deploymentProperties.put(ENRICHMENT_DEREFERENCE_URL, jobProperties.getEnrichment().getDereferenceUrl());
    deploymentProperties.put(ENRICHMENT_ENTITY_MANAGEMENT_URL, jobProperties.getEnrichment().getEntityManagementUrl());
    deploymentProperties.put(ENRICHMENT_ENTITY_API_URL, jobProperties.getEnrichment().getEntityApiUrl());
    deploymentProperties.put(ENRICHMENT_ENTITY_API_KEY, jobProperties.getEnrichment().getEntityApiKey());

    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add("datasetId=1");
    arguments.add("executionId=37");
    arguments.add("targetJob=ENRICHMENT");

    polingStatus(launchTask(taskName, deploymentProperties, arguments));
  }

  @Test
  void launchMediaTask() {
    final RegisterConfigurationProperties registerProperties = metisBatchConfigurationProperties.getRegisterProperties();
    final String taskName = registerProperties.getMediaName();
    final JobConfigurationProperties jobProperties = metisBatchConfigurationProperties.getJobProperties();
    final Map<String, String> deploymentProperties = new HashMap<>();
    deploymentProperties.put(MEDIA_CHUNK_SIZE, jobProperties.getMedia().getChunkSize());
    deploymentProperties.put(MEDIA_PARALLELIZATION_SIZE, jobProperties.getMedia().getParallelizationSize());
    final ArrayList<String> arguments = new ArrayList<>();
    arguments.add("datasetId=1");
    arguments.add("executionId=39");
    arguments.add("targetJob=MEDIA");

    polingStatus(launchTask(taskName, deploymentProperties, arguments));
  }

  private void polingStatus(LaunchResponseResource launchResponseResource) {

    final Supplier<TaskExecutionResource> getTaskExecutionResource = () ->
        dataFlowOperations.taskOperations().taskExecutionStatus(launchResponseResource.getExecutionId(), "boot3");
    TaskExecutionResource taskExecutionResource = getTaskExecutionResource.get();
    LOGGER.info("Task launched with taskId: {}", taskExecutionResource.getExecutionId());
    pollingUnkown(getTaskExecutionResource);
    pollingRunning(getTaskExecutionResource);
    taskExecutionResource = getTaskExecutionResource.get();
    LOGGER.info("Task finished with details:");
    LOGGER.info("Task finished with jobIds: {}, taskId: {}, status: {}, startTime: {}, endTime: {}",
        taskExecutionResource.getJobExecutionIds(), taskExecutionResource.getExecutionId(),
        taskExecutionResource.getTaskExecutionStatus(), taskExecutionResource.getStartTime(), taskExecutionResource.getEndTime());
  }

  private static void pollingRunning(Supplier<TaskExecutionResource> getTaskExecutionResource) {
    await().forever().until(() -> {
      final TaskExecutionStatus taskExecutionStatus = getTaskExecutionResource.get().getTaskExecutionStatus();
      return taskExecutionStatus == TaskExecutionStatus.COMPLETE || taskExecutionStatus == TaskExecutionStatus.ERROR;
    });
  }

  private static void pollingUnkown(Supplier<TaskExecutionResource> getTaskExecutionResource) {
    //Await for potential UNKNOWN status in case of pod deployment failure.
    try {
      await().atMost(30, TimeUnit.SECONDS).until(() -> {
        TaskExecutionStatus taskExecutionStatus = getTaskExecutionResource.get().getTaskExecutionStatus();
        return taskExecutionStatus != TaskExecutionStatus.UNKNOWN;
      });
    } catch (ConditionTimeoutException ex) {
      LOGGER.error("Launch failed with status: {}", TaskExecutionStatus.UNKNOWN);
      throw ex;
    }
  }

  LaunchResponseResource launchTask(String taskName, Map<String, String> additionalDeploymentProperties, List<String> arguments) {
    final Map<String, String> deploymentProperties = metisBatchConfigurationProperties.getDeploymentProperties();
    deploymentProperties.putAll(additionalDeploymentProperties);
    final Map<String, String> appPrefixedDeploymentProperties = prefixMap(taskName, deploymentProperties);
    return dataFlowOperations.taskOperations().launch(taskName, appPrefixedDeploymentProperties, arguments);
  }

  private Map<String, String> prefixMap(String prefix, Map<String, String> map) {
    return TransformedMap.transformedMap(map, key -> format("app.%s.%s", prefix, key), value -> value);
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
