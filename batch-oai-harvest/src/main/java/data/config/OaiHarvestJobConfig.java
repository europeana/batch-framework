package data.config;

import static data.job.BatchJobType.OAI_HARVEST;

import data.entity.ExecutionRecordDTO;
import data.job.incrementer.TimestampJobParametersIncrementer;
import data.unit.reader.OaiHarvestItemReader;
import data.unit.writer.ExecutionRecordDTOItemWriter;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableTask
public class OaiHarvestJobConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String BATCH_JOB = OAI_HARVEST.name();
  public static final String STEP_NAME = "oaiHarvestStep";

  @Value("${oaiharvest.chunk.size}")
  public int chunkSize;
  @Value("${oaiharvest.parallelization.size}")
  public int parallelization;

  @Bean
  public Job oaiHarvestBatchJob(JobRepository jobRepository, Step oaiHarvestStep) {
    LOGGER.info("Chunk size: {}, Parallelization size: {}", chunkSize, parallelization);
    return new JobBuilder(BATCH_JOB, jobRepository)
        .incrementer(new TimestampJobParametersIncrementer())
        .start(oaiHarvestStep)
        .build();
  }

  @Bean
  public Step oaiHarvestStep(JobRepository jobRepository,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
      OaiHarvestItemReader oaiHarvestItemReader,
      ExecutionRecordDTOItemWriter writer,
      @Qualifier("oaiHarvestStepAsyncTaskExecutor") TaskExecutor oaiHarvestStepAsyncTaskExecutor) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<ExecutionRecordDTO, ExecutionRecordDTO>chunk(chunkSize, transactionManager)
        .reader(oaiHarvestItemReader)
        .writer(writer)
        //TODO: 2024-01-31 - Update with a better parallelization reader and remove Step parallization.
        .taskExecutor(oaiHarvestStepAsyncTaskExecutor)
        .throttleLimit(parallelization)
        .build();
  }

  @Bean
  public TaskExecutor oaiHarvestStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(parallelization);
    executor.setMaxPoolSize(parallelization);
    executor.initialize();
    return executor;
  }

  @Bean
  @StepScope
  public OaiHarvestItemReader oaiHarvestItemReader(){
    return new OaiHarvestItemReader();
  }
}
