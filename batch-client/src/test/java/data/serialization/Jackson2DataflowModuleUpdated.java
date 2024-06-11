package data.serialization;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.cloud.dataflow.rest.job.StepExecutionHistory;
import org.springframework.cloud.dataflow.rest.support.jackson.ExecutionContextJacksonMixIn;
import org.springframework.cloud.dataflow.rest.support.jackson.ExitStatusJacksonMixIn;
import org.springframework.cloud.dataflow.rest.support.jackson.JobInstanceJacksonMixIn;
import org.springframework.cloud.dataflow.rest.support.jackson.JobParametersJacksonMixIn;
import org.springframework.cloud.dataflow.rest.support.jackson.StepExecutionHistoryJacksonMixIn;
import org.springframework.cloud.dataflow.rest.support.jackson.StepExecutionJacksonMixIn;

public class Jackson2DataflowModuleUpdated extends SimpleModule {

  public Jackson2DataflowModuleUpdated() {
    super("spring-cloud-dataflow-module", new Version(1, 0, 0, null, "org.springframework.cloud", "spring-cloud-dataflow"));

    setMixInAnnotation(JobExecution.class, JobExecutionJacksonMixInUpdated.class);
    setMixInAnnotation(JobParameters.class, JobParametersJacksonMixIn.class);
    setMixInAnnotation(JobParameter.class, JobParameterJacksonMixInUpdated.class);
    setMixInAnnotation(JobInstance.class, JobInstanceJacksonMixIn.class);
    setMixInAnnotation(ExitStatus.class, ExitStatusJacksonMixIn.class);
    setMixInAnnotation(StepExecution.class, StepExecutionJacksonMixIn.class);
    setMixInAnnotation(ExecutionContext.class, ExecutionContextJacksonMixIn.class);
    setMixInAnnotation(StepExecutionHistory.class, StepExecutionHistoryJacksonMixIn.class);
  }

}
