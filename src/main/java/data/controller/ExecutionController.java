package data.controller;


import data.repositories.ExecutionRecordRepository;
import data.utility.BatchJobType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionController.class);

  private final JobLauncher taskExecutorJobLauncher;
  private final Job defaultBatchJob;
  private final Job oaiHarvestBatchJob;
  private final Job validationBatchJob;
  private final Job tranformationBatchJob;
  private final JobExplorer jobExplorer;
  private final ExecutionRecordRepository executionRecordRepository;

  public ExecutionController(
      JobLauncher taskExecutorJobLauncher,
      @Qualifier("defaultBatchJob") Job defaultBatchJob,
      @Qualifier("oaiHarvestBatchJob") Job oaiHarvestBatchJob,
      @Qualifier("validationBatchJob") Job validationBatchJob,
      @Qualifier("transformationBatchJob") Job tranformationBatchJob,
      JobExplorer jobExplorer,
      ExecutionRecordRepository executionRecordRepository) {
    this.taskExecutorJobLauncher = taskExecutorJobLauncher;
    this.defaultBatchJob = defaultBatchJob;
    this.oaiHarvestBatchJob = oaiHarvestBatchJob;
    this.validationBatchJob = validationBatchJob;
    this.tranformationBatchJob = tranformationBatchJob;
    this.jobExplorer = jobExplorer;
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
      case VALIDATION_EXTERNAL, VALIDATION_INTERNAL -> validationBatchJob;
      case TRANSFORMATION -> tranformationBatchJob;
      default -> throw new IllegalStateException("Unexpected value: " + BatchJobType.valueOf(targetJob));
    };

    final JobExecution jobExecution = taskExecutorJobLauncher.run(batchJob, params);
    LOGGER.info("JobExecution identifier: {}", jobExecution.getJobId());
    return ResponseEntity.ok().body("Batch job has been invoked");
  }

  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getJobStatus(@RequestParam Long jobExecutionId) {
    Map<String, Object> response = new HashMap<>();
    final JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);
    if (jobExecution == null) {
      response.put("message", "No job execution found");
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    BatchStatus batchStatus = jobExecution.getStatus();
    response.put("status", batchStatus.toString());
    Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
    for (StepExecution stepExecution : stepExecutions) {
      // In our case, there's only one step. If you have multiple steps, you might want to key by step name.
      response.put("readCount", stepExecution.getReadCount());
      response.put("writeCount", stepExecution.getWriteCount());
      response.put("commitCount", stepExecution.getCommitCount());
      response.put("skipCount", stepExecution.getSkipCount());
      response.put("rollbackCount", stepExecution.getRollbackCount());
      response.put("executionRecordsInDB", executionRecordRepository.count());
      // Progress indicator. Assuming you know the total records in advance (100,000 in this case).
      int progress = (int) (((double) stepExecution.getReadCount() / 4) * 100);
      response.put("progress", progress + "%");
    }
    return ResponseEntity.ok().body(response);
  }
}
