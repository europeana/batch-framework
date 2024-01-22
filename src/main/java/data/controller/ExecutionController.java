package data.controller;


import data.repositories.ExecutionRecordRepository;
import data.utility.BatchJobType;
import java.lang.invoke.MethodHandles;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class ExecutionController {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final JobLauncher taskExecutorJobLauncher;
  private final Job defaultBatchJob;
  private final Job oaiHarvestBatchJob;
  private final Job validationBatchJob;
  private final Job tranformationBatchJob;
  private final Job normalizationBatchJob;
  private final Job enrichmentBatchJob;
  private final Job mediaBatchJob;
  private final JobExplorer jobExplorer;
  private final JobOperator jobOperator;
  private final ExecutionRecordRepository executionRecordRepository;

  public ExecutionController(
      JobLauncher taskExecutorJobLauncher,
      @Qualifier("defaultBatchJob") Job defaultBatchJob,
      @Qualifier("oaiHarvestBatchJob") Job oaiHarvestBatchJob,
      @Qualifier("validationBatchJob") Job validationBatchJob,
      @Qualifier("transformationBatchJob") Job tranformationBatchJob,
      @Qualifier("normalizationBatchJob") Job normalizationBatchJob,
      @Qualifier("enrichmentBatchJob") Job enrichmentBatchJob,
      @Qualifier("mediaBatchJob") Job mediaBatchJob,
      JobExplorer jobExplorer,
      JobOperator jobOperator,
      ExecutionRecordRepository executionRecordRepository) {
    this.taskExecutorJobLauncher = taskExecutorJobLauncher;
    this.defaultBatchJob = defaultBatchJob;
    this.oaiHarvestBatchJob = oaiHarvestBatchJob;
    this.validationBatchJob = validationBatchJob;
    this.tranformationBatchJob = tranformationBatchJob;
    this.normalizationBatchJob = normalizationBatchJob;
    this.enrichmentBatchJob = enrichmentBatchJob;
    this.mediaBatchJob = mediaBatchJob;
    this.jobExplorer = jobExplorer;
    this.jobOperator = jobOperator;
    this.executionRecordRepository = executionRecordRepository;
  }

  @GetMapping("/start")
  public ResponseEntity<String> handle(@RequestParam String datasetId, @RequestParam String executionId,
      @RequestParam String targetJob) throws Exception {
    JobParameters params = new JobParametersBuilder()
        /////////Common parameters
        .addString("datasetId", datasetId)
        .addString("executionId", executionId)
        .addString("targetJob", targetJob)
        //Add this to be able to re-run the same job. If parameters are identical the job is declined.
        .addString("executionUUID", String.valueOf(System.currentTimeMillis()))
        ////////Transformation parameters
        .addString("datasetName", "idA_metisDatasetNameA")
        .addString("datasetCountry", "Greece")
        .addString("datasetLanguage", "el")
        .addString("xsltUrl", "https://metis-core-rest.test.eanadev.org/datasets/xslt/6204e5e2514e773e6745f7e9")
        /////////Oai harvest parameters
        .addString("oaiEndpoint", "https://metis-repository-rest.test.eanadev.org/repository/oai")
        .addString("oaiSet", "spring_batch_test_9_valid")
        .addString("oaiMetadataPrefix", "edm")
        .toJobParameters();

    final Job batchJob = switch (BatchJobType.valueOf(targetJob)) {
      case DEFAULT -> defaultBatchJob;
      case OAI_HARVEST -> oaiHarvestBatchJob;
      case VALIDATION -> throw new IllegalStateException("Unexpected value: " + BatchJobType.valueOf(targetJob));
      case VALIDATION_EXTERNAL, VALIDATION_INTERNAL -> validationBatchJob;
      case TRANSFORMATION -> tranformationBatchJob;
      case NORMALIZATION -> normalizationBatchJob;
      case ENRICHMENT -> enrichmentBatchJob;
      case MEDIA -> mediaBatchJob;
    };

    final JobExecution jobExecution = taskExecutorJobLauncher.run(batchJob, params);
    LOGGER.info("JobInstance identifier: {}", jobExecution.getJobInstance().getInstanceId());
    return ResponseEntity.ok().body("Batch job has been invoked");
  }

  @GetMapping("/stop")
  public ResponseEntity<String> stopJob(@RequestParam Long jobInstanceId)
      throws NoSuchJobExecutionException, JobExecutionNotRunningException {
    final JobInstance jobInstance = jobExplorer.getJobInstance(jobInstanceId);
    if (jobInstance == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No job instance found");
    }
    final JobExecution lastJobExecution = jobExplorer.getLastJobExecution(jobInstance);
    if (lastJobExecution == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No job execution found");
    }

    final boolean returnStatus = jobOperator.stop(lastJobExecution.getId());
    return ResponseEntity.ok().body(String.format("Job stop executed with status: %s", returnStatus));
  }

  @GetMapping("/restart")
  public ResponseEntity<String> restartJob(@RequestParam Long jobInstanceId)
      throws NoSuchJobExecutionException, JobInstanceAlreadyCompleteException, NoSuchJobException, JobParametersInvalidException, JobRestartException {
    final JobInstance jobInstance = jobExplorer.getJobInstance(jobInstanceId);
    if (jobInstance == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No job instance found");
    }
    final JobExecution lastJobExecution = jobExplorer.getLastJobExecution(jobInstance);
    if (lastJobExecution == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No job execution found");
    }

    final Long id = jobOperator.restart(lastJobExecution.getId());
    return ResponseEntity.ok().body(String.format("Job restart executed with id: %s", id));
  }


  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getJobStatus(@RequestParam Long jobInstanceId) {
    Map<String, Object> response = new HashMap<>();

    final JobInstance jobInstance = jobExplorer.getJobInstance(jobInstanceId);
    if (jobInstance == null) {
      response.put("message", "No job instance found");
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    final JobExecution lastJobExecution = jobExplorer.getLastJobExecution(jobInstance);
    if (lastJobExecution == null) {
      response.put("message", "No job execution found");
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    BatchStatus batchStatus = lastJobExecution.getStatus();
    response.put("status", batchStatus.toString());
    Collection<StepExecution> stepExecutions = lastJobExecution.getStepExecutions();
    for (StepExecution stepExecution : stepExecutions) {
      // In our case, there's only one step. If you have multiple steps, you might want to key by step name.
      response.put("readCount", stepExecution.getReadCount());
      response.put("writeCount", stepExecution.getWriteCount());
      response.put("commitCount", stepExecution.getCommitCount());
      response.put("skipCount", stepExecution.getSkipCount());
      response.put("rollbackCount", stepExecution.getRollbackCount());
      response.put("executionRecordsInDB", executionRecordRepository.count());
      if (stepExecution.getStartTime() != null && stepExecution.getEndTime() != null) {
        response.put("Duration in seconds", stepExecution.getStartTime().until(stepExecution.getEndTime(), ChronoUnit.SECONDS));
      }
    }
    return ResponseEntity.ok().body(response);
  }
}
