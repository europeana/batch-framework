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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class TransformationJobConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String BATCH_JOB = BatchJobType.TRANSFORMATION.name();
  public static final String STEP_NAME = "transformationStep";
  @Value("${transformation.chunk.size}")
  public int chunkSize;
  @Value("${transformation.parallelization.size}")
  public int parallelization;

  @Bean
  public Job transformationBatchJob(JobRepository jobRepository, Step tranformationStep) {
    LOGGER.info("Chunk size: {}, Parallelization size: {}", chunkSize, parallelization);
    return new JobBuilder(BATCH_JOB, jobRepository)
        .start(tranformationStep)
        .build();
  }

  @Bean
  public Step tranformationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
      RepositoryItemReader<ExecutionRecord> trasnformationRepositoryItemReader,
      ItemProcessor<ExecutionRecord, ExecutionRecord> transformerItemProcessor,
      RepositoryItemWriter<ExecutionRecord> writer,
      DelayLoggingItemProcessListener<ExecutionRecord> delayLoggingItemProcessListener,
      TaskExecutor transformationStepAsyncTaskExecutor) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<ExecutionRecord, ExecutionRecord>chunk(chunkSize, transactionManager)
        .reader(trasnformationRepositoryItemReader)
        .processor(transformerItemProcessor)
        .writer(writer)
        .listener(delayLoggingItemProcessListener)
        .taskExecutor(transformationStepAsyncTaskExecutor)
        .throttleLimit(parallelization)
        .build();
  }

  @Bean
  @StepScope
  public RepositoryItemReader<ExecutionRecord> trasnformationRepositoryItemReader(
      ExecutionRecordRepository executionRecordRepository) {
    final DefaultRepositoryItemReader defaultRepositoryItemReader = new DefaultRepositoryItemReader(executionRecordRepository);
    defaultRepositoryItemReader.setPageSize(chunkSize);
    return defaultRepositoryItemReader;
  }

  @Bean
  public TaskExecutor transformationStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(parallelization);
    executor.setMaxPoolSize(parallelization);
    executor.initialize();
    return executor;
  }
}
