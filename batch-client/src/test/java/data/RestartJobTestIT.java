package data;

import data.config.MetisDataflowClientConfig;
import data.config.properties.BatchConfigurationProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.JobInstanceResource;
import org.springframework.cloud.dataflow.rest.resource.StepExecutionResource;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {MetisDataflowClientConfig.class})
@EnableAutoConfiguration
class RestartJobTestIT {

  public static final String SCHEMA_TARGET = "boot3";

  @Autowired
  DataFlowOperations dataFlowOperations;

  @Autowired
  JobRepository jobRepository;

  @Autowired
  JobOperator jobOperator;

  @Autowired
  BatchConfigurationProperties batchConfigurationProperties;

  /**
   * An abrupt example of job will have Job Status=STARTED, ExitStatus=UNKNOWN and step Status=STARTED, ExitStatus=EXECUTING To
   * achieve a restart with the paging to properly continue we need to: - Mark the job Status and ExitStatus as STOPPED - Mark the
   * jobStep Status and ExitStatus as STOPPED(If only the job is marked and not the step the restart does not work) - There will
   * be a new taskExecutionId and a new JobExecutionId with the same JobInstanceId therefore the db records will have the same id.
   * The commits and continuation of the paging can be verified in the table boot3_batch_step_execution.
   * <br>
   * To achieve this the {@code .incrementer(new TimestampJobParametersIncrementer())} from the job configuration should be
   * removed, and the timestamp can be added and maintained from the client.
   * <br>
   * Other notes: The custom mapper works to get the jobExecution(from the jobOperations()). But registrations of applications
   * then starts to fail(from the taskOperations()).
   * <br>
   * Another approach is that the client will send an uuid with the request and on "restart" it Spring batch will be a completely
   * new instance but the paging will be first calculated based on what is already processed and will continue from the last
   * unsuccessful page(paging should be ordered).
   */
  @Test
  void launchJobRestart() {
    final String jobInstanceId = "38";

    final JobInstanceResource jobInstanceResource = dataFlowOperations.jobOperations()
                                                                      .jobInstance(Long.parseLong(jobInstanceId), SCHEMA_TARGET);
    final Long jobExecutionId = jobInstanceResource.getJobExecutions().getFirst().getExecutionId();
    //Need to get the jobExecution this way to get the correct versioning field.
    final JobExecutionResource jobExecutionResource = dataFlowOperations.jobOperations()
                                                                        .jobExecution(jobExecutionId, SCHEMA_TARGET);
    final JobExecution jobExecution = jobExecutionResource.getJobExecution();
    jobExecution.setExitStatus(ExitStatus.STOPPED);
    jobExecution.setStatus(BatchStatus.STOPPED);

    //We need to set manually every field, there seems to be no other way to "update" the existent object values.
    final List<StepExecution> stepExecutions = dataFlowOperations.jobOperations().stepExecutionList(jobExecutionId, SCHEMA_TARGET)
                                                                 .getContent().stream()
                                                                 .map(StepExecutionResource::getStepExecution)
                                                                 .map(originalStepExecution -> {
                                                                   StepExecution newStepExecution = new StepExecution(
                                                                       originalStepExecution.getStepName(),
                                                                       jobExecution,
                                                                       originalStepExecution.getId());
                                                                   newStepExecution.setStatus(BatchStatus.STOPPED);
                                                                   newStepExecution.setExitStatus(ExitStatus.STOPPED);
                                                                   // Copy other properties
                                                                   newStepExecution.setReadCount(
                                                                       originalStepExecution.getReadCount());
                                                                   newStepExecution.setWriteCount(
                                                                       originalStepExecution.getWriteCount());
                                                                   newStepExecution.setCommitCount(
                                                                       originalStepExecution.getCommitCount());
                                                                   newStepExecution.setRollbackCount(
                                                                       originalStepExecution.getRollbackCount());
                                                                   newStepExecution.setReadSkipCount(
                                                                       originalStepExecution.getReadSkipCount());
                                                                   newStepExecution.setProcessSkipCount(
                                                                       originalStepExecution.getProcessSkipCount());
                                                                   newStepExecution.setWriteSkipCount(
                                                                       originalStepExecution.getWriteSkipCount());
                                                                   newStepExecution.setStartTime(
                                                                       originalStepExecution.getStartTime());
                                                                   newStepExecution.setEndTime(
                                                                       originalStepExecution.getEndTime());
                                                                   newStepExecution.setLastUpdated(
                                                                       originalStepExecution.getLastUpdated());
                                                                   newStepExecution.setExecutionContext(
                                                                       originalStepExecution.getExecutionContext());
                                                                   newStepExecution.setVersion(
                                                                       originalStepExecution.getVersion());
                                                                   return newStepExecution;
                                                                 }).toList();

    jobRepository.update(jobExecution);
    stepExecutions.forEach(stepExecution -> jobRepository.update(stepExecution));
    dataFlowOperations.jobOperations().executionRestart(jobExecutionResource.getExecutionId(), SCHEMA_TARGET);
  }
}
