package data.config;

import data.entity.ExecutionRecord;
import data.repositories.ExecutionRecordRepository;
import data.unit.processor.XsltTransformerItemProcessor;
import data.unit.reader.DefaultRepositoryItemReader;
import data.utility.BatchJobType;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
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
import org.springframework.batch.item.support.CompositeItemProcessor;
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
  public static final int CHUNK_SIZE = 10;
  public static final int PARALLELIZATION = 2;

  @Bean
  public Job transformationBatchJob(JobRepository jobRepository, Step tranformationStep) {
    return new JobBuilder(BATCH_JOB, jobRepository)
        .start(tranformationStep)
        .build();
  }

  @Bean
  public Step tranformationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
      RepositoryItemReader<ExecutionRecord> trasnformationRepositoryItemReader,
      ItemProcessor<ExecutionRecord, ExecutionRecord> compositeTransformationItemProcessor,
      RepositoryItemWriter<ExecutionRecord> writer,
      TaskExecutor transformationStepAsyncTaskExecutor) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<ExecutionRecord, ExecutionRecord>chunk(CHUNK_SIZE, transactionManager)
        .reader(trasnformationRepositoryItemReader)
        .processor(compositeTransformationItemProcessor)
        .writer(writer)
        .taskExecutor(transformationStepAsyncTaskExecutor)
        .build();
  }

  @Bean
  @StepScope
  public RepositoryItemReader<ExecutionRecord> trasnformationRepositoryItemReader(
      ExecutionRecordRepository executionRecordRepository) {
    final DefaultRepositoryItemReader defaultRepositoryItemReader = new DefaultRepositoryItemReader(executionRecordRepository);
    defaultRepositoryItemReader.setPageSize(CHUNK_SIZE);
    return defaultRepositoryItemReader;
  }

  @Bean
  public ItemProcessor<ExecutionRecord, ExecutionRecord> compositeTransformationItemProcessor(
      XsltTransformerItemProcessor xsltTransformerItemProcessor,
      ItemProcessor<ExecutionRecord, ExecutionRecord> delayLoggingItemProcessor) {
    CompositeItemProcessor<ExecutionRecord, ExecutionRecord> compositeItemProcessor = new CompositeItemProcessor<>();
    compositeItemProcessor.setDelegates(Arrays.asList(xsltTransformerItemProcessor, delayLoggingItemProcessor));
    return compositeItemProcessor;
  }

  @Bean
  public TaskExecutor transformationStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(PARALLELIZATION);
    executor.setMaxPoolSize(PARALLELIZATION);
    executor.initialize();
    return executor;
  }
}
