package data;

import static data.DbCleaner.JUNIT_DATASET;

import static eu.europeana.cloud.flink.client.constants.postgres.JobParamName.*;
import static eu.europeana.cloud.flink.client.constants.postgres.JobParamValue.VALIDATION_EXTERNAL;
import static eu.europeana.cloud.flink.client.constants.postgres.JobParamValue.VALIDATION_INTERNAL;
import static java.util.Collections.emptyMap;

import data.config.properties.JobConfigurationProperties;
import eu.europeana.cloud.flink.client.JobExecutor;
import eu.europeana.cloud.flink.client.entities.SubmitJobRequest;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.AbstractEnvironment;


public class FlinkPerformaceTest extends AbstractPerformanceTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static JobExecutor executor;

  @BeforeAll
  public static void createExecutor(@Autowired AbstractEnvironment environment) {
    executor = new JobExecutor(environment);
  }

  @Test
  public void step1_shouldExecuteOAIHarvestCompletellyWithoutErrors() throws InterruptedException {

    executeStep(1,
        "eu.europeana.cloud.job.oai.OAIJob",
        Map.of(OAI_REPOSITORY_URL, sourceProperties.getUrl()
            , SET_SPEC, sourceProperties.getSetSpec()
            , METADATA_PREFIX, sourceProperties.getMetadataPrefix()));
  }

  @Test
  public void step2_shouldExecuteExternalValidationWithoutErrors() throws InterruptedException {
    executeStep(2,
        "eu.europeana.cloud.job.validation.ValidationJobWithPostgresMultiThreadedOperation",
        Map.of(VALIDATION_TYPE, VALIDATION_EXTERNAL));
  }


  @Test
  public void step3_shouldExecuteXsltTransformationWithoutErrors() throws InterruptedException {
    executeStep(3,
        "eu.europeana.cloud.job.transformation.TransformationJobWithPostgresMultiThreadedOperation",
        Map.of(METIS_DATASET_NAME, "idA_metisDatasetNameA"
            , METIS_DATASET_COUNTRY, "Greece"
            , METIS_DATASET_LANGUAGE, "el"
            , METIS_XSLT_URL, "https://metis-core-rest.test.eanadev.org/datasets/xslt/6204e5e2514e773e6745f7e9"
        ));
  }

  @Test
  public void step4_shouldExecuteIternalValidationWithoutErrors() throws InterruptedException {
    executeStep(4,
        "eu.europeana.cloud.job.validation.ValidationJobWithPostgresMultiThreadedOperation",
        Map.of(VALIDATION_TYPE, VALIDATION_INTERNAL));
  }

  @Test
  public void step5_shouldExecuteNormalizationWithoutErrors() throws InterruptedException {
    executeStep(5,
        "eu.europeana.cloud.job.normalization.NormalizationJobWithPostgresMultiThreadedOperation",
        emptyMap()
    );
  }

  @Test
  public void step6_shouldExecuteEnrichmentWithoutErrors() throws InterruptedException {
    JobConfigurationProperties jobProperties = batchConfigurationProperties.getJobProperties();
    executeStep(6,
        "eu.europeana.cloud.job.enrichment.EnrichmentJobWithPostgresMultiThreadedOperation",
        Map.of(
            DEREFERENCE_SERVICE_URL, jobProperties.getEnrichment().getDereferenceUrl()
            , ENRICHMENT_ENTITY_MANAGEMENT_URL, jobProperties.getEnrichment().getEntityManagementUrl()
            , ENRICHMENT_ENTITY_API_URL, jobProperties.getEnrichment().getEntityApiUrl()
            , ENRICHMENT_ENTITY_API_KEY, jobProperties.getEnrichment().getEntityApiKey()
        )
    );

  }

  @Test
  public void step7_shouldExecuteMediaWithoutErrors() throws InterruptedException {
    executeStep(7,
        "eu.europeana.cloud.job.media.MediaJobWithPostgresMultiThreadedOperation",
        emptyMap()
    );
  }

  @Test
  public void step8_shouldExecuteIndexingWithoutErrors() throws InterruptedException {
    JobConfigurationProperties jobProperties = batchConfigurationProperties.getJobProperties();
    Map<String, String> specialParameters = new HashMap<>();
    specialParameters.put(INDEXING_PRESERVETIMESTAMPS, jobProperties.getIndexing().getPreserveTimestamps());
    specialParameters.put(INDEXING_PERFORMREDIRECTS, jobProperties.getIndexing().getPerformRedirects());
    specialParameters.put(INDEXING_MONGOINSTANCES, jobProperties.getIndexing().getMongoInstances());
    specialParameters.put(INDEXING_MONGOPORTNUMBER, jobProperties.getIndexing().getMongoPortNumber());
    specialParameters.put(INDEXING_MONGODBNAME, jobProperties.getIndexing().getMongoDbName());
    specialParameters.put(INDEXING_MONGOREDIRECTDBNAME, jobProperties.getIndexing().getMongoRedirectsDbName());
    specialParameters.put(INDEXING_MONGOUSERNAME, jobProperties.getIndexing().getMongoUsername());
    specialParameters.put(INDEXING_MONGOPASSWORD, jobProperties.getIndexing().getMongoPassword());
    specialParameters.put(INDEXING_MONGOAUTHDB, jobProperties.getIndexing().getMongoAuthDB());
    specialParameters.put(INDEXING_MONGOUSESSL, jobProperties.getIndexing().getMongoUseSSL());
    specialParameters.put(INDEXING_MONGOREADPREFERENCE, jobProperties.getIndexing().getMongoReadPreference());
    specialParameters.put(INDEXING_MONGOPOOLSIZE, jobProperties.getIndexing().getMongoPoolSize());
    specialParameters.put(INDEXING_SOLRINSTANCES, jobProperties.getIndexing().getMongoApplicationName());
    specialParameters.put(INDEXING_ZOOKEEPERINSTANCES, jobProperties.getIndexing().getZookeeperInstances());
    specialParameters.put(INDEXING_ZOOKEEPERPORTNUMBER, jobProperties.getIndexing().getZookeeperPortNumber());
    specialParameters.put(INDEXING_ZOOKEEPERCHROOT, jobProperties.getIndexing().getZookeeperChroot());
    specialParameters.put(INDEXING_ZOOKEEPERDEFAULTCOLLECTION, jobProperties.getIndexing().getZookeeperChroot());
    specialParameters.put(INDEXING_MONGOAPPLICATIONNAME, jobProperties.getIndexing().getZookeeperDefaultCollection());

    executeStep(8,
        "eu.europeana.cloud.job.indexing.IndexingJobWithPostgresMultiThreadedOperation"
        , specialParameters);
  }

  public void executeStep(int stepNumber, String jobClass, Map<String, String> specialParameters) throws InterruptedException {
    enforceDbClear(stepNumber);
    String datasetId = JUNIT_DATASET;
    String taskId = String.valueOf(stepNumber); // String.valueOf(new Random().nextLong(Long.MAX_VALUE));

    LOGGER.info("Submitting job request datasetId: {}, taskId: {}", datasetId, taskId);
    Map<String, Object> jobParams = new HashMap(Map.of(
        DATASOURCE_URL, dbConfig.getJdbcUrl()
        , DATASOURCE_USERNAME, dbConfig.getUsername()
        , DATASOURCE_PASSWORD, dbConfig.getPassword()
        , DATASET_ID, datasetId
        , CHUNK_SIZE, "100"
        , TASK_ID, taskId
    ));
    if (stepNumber > 1) {
      jobParams.put(EXECUTION_ID, stepNumber - 1);
    }
    jobParams.putAll(specialParameters);

    SubmitJobRequest request = SubmitJobRequest
        .builder()
        .entryClass(jobClass)
        .parallelism(String.valueOf(PARALLELISM))
        .programArgs(jobParams)
        .build();
    startWatch = StopWatch.createStarted();
    executor.execute(request);
    validateResult(stepNumber);
  }

}
