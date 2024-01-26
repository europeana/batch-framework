package data.config;

import data.entity.ExecutionRecord;
import data.repositories.ExecutionRecordRepository;
import data.unit.processor.listener.DelayLoggingItemProcessListener;
import data.unit.reader.DefaultRepositoryItemReader;
import data.utility.BatchJobType;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DefaultJobConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String BATCH_JOB = BatchJobType.DEFAULT.name();
  public static final String STEP_NAME = "defaultStep";
  public int chunkSize = 10;
  public int parallelization = 2;

  @Bean
  public Job defaultBatchJob(JobRepository jobRepository, Step defaultStep) {
    return new JobBuilder(BATCH_JOB, jobRepository)
        .start(defaultStep)
        .build();
  }

  @Bean
  public Step defaultStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
      RepositoryItemReader<ExecutionRecord> defaultRepositoryItemReader,
      ItemProcessor<ExecutionRecord, ExecutionRecord> defaultItemProcessor,
      RepositoryItemWriter<ExecutionRecord> writer,
      DelayLoggingItemProcessListener<ExecutionRecord> delayLoggingItemProcessListener,
      TaskExecutor defaultStepAsyncTaskExecutor) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<ExecutionRecord, ExecutionRecord>chunk(chunkSize, transactionManager)
        .reader(defaultRepositoryItemReader)
        .processor(defaultItemProcessor)
        .writer(writer)
        .listener(delayLoggingItemProcessListener)
        .taskExecutor(defaultStepAsyncTaskExecutor)
        .build();
  }

  @Bean
  @StepScope
  public RepositoryItemReader<ExecutionRecord> defaultRepositoryItemReader(ExecutionRecordRepository executionRecordRepository) {
    final DefaultRepositoryItemReader defaultRepositoryItemReader = new DefaultRepositoryItemReader(executionRecordRepository);
    defaultRepositoryItemReader.setPageSize(chunkSize);
    return defaultRepositoryItemReader;
  }

  @Bean
  public TaskExecutor defaultStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(parallelization);
    executor.setMaxPoolSize(parallelization);
    executor.initialize();
    return executor;
  }
}
