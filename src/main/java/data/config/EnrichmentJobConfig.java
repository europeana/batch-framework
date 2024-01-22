package data.config;

import data.entity.ExecutionRecord;
import data.repositories.ExecutionRecordRepository;
import data.unit.processor.EnrichmentItemProcessor;
import data.unit.reader.DefaultRepositoryItemReader;
import data.utility.BatchJobType;
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
public class EnrichmentJobConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(EnrichmentJobConfig.class);
  public static final String BATCH_JOB = BatchJobType.ENRICHMENT.name();
  public static final String STEP_NAME = "enrichmentStep";
  public static final int CHUNK_SIZE = 10;
  public static final int PARALLELIZATION = 10;

  @Bean
  public Job enrichmentBatchJob(JobRepository jobRepository, Step enrichmentStep) {
    return new JobBuilder(BATCH_JOB, jobRepository)
        .start(enrichmentStep)
        .build();
  }

  @Bean
  public Step enrichmentStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
      RepositoryItemReader<ExecutionRecord> enrichmentRepositoryItemReader,
      ItemProcessor<ExecutionRecord, ExecutionRecord> compositeEnrichmentItemProcessor,
      RepositoryItemWriter<ExecutionRecord> writer,
      TaskExecutor enrichmentStepAsyncTaskExecutor) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<ExecutionRecord, ExecutionRecord>chunk(CHUNK_SIZE, transactionManager)
        .reader(enrichmentRepositoryItemReader)
        .processor(compositeEnrichmentItemProcessor)
        .writer(writer)
        .taskExecutor(enrichmentStepAsyncTaskExecutor)
        .build();
  }

  @Bean
  @StepScope
  public RepositoryItemReader<ExecutionRecord> enrichmentRepositoryItemReader(
      ExecutionRecordRepository executionRecordRepository) {
    final DefaultRepositoryItemReader defaultRepositoryItemReader = new DefaultRepositoryItemReader(executionRecordRepository);
    defaultRepositoryItemReader.setPageSize(CHUNK_SIZE);
    return defaultRepositoryItemReader;
  }

  @Bean
  public ItemProcessor<ExecutionRecord, ExecutionRecord> compositeEnrichmentItemProcessor(
      EnrichmentItemProcessor enrichmentItemProcessor,
      ItemProcessor<ExecutionRecord, ExecutionRecord> delayLoggingItemProcessor) {
    CompositeItemProcessor<ExecutionRecord, ExecutionRecord> compositeItemProcessor = new CompositeItemProcessor<>();
    compositeItemProcessor.setDelegates(Arrays.asList(enrichmentItemProcessor, delayLoggingItemProcessor));
    return compositeItemProcessor;
  }

  @Bean
  public TaskExecutor enrichmentStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(PARALLELIZATION);
    executor.setMaxPoolSize(PARALLELIZATION);
    executor.initialize();
    return executor;
  }

}
