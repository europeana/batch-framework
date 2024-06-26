package data.config;

import static data.job.BatchJobType.INDEXING;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import data.job.incrementer.TimestampJobParametersIncrementer;
import data.repositories.ExecutionRecordRepository;
import data.unit.processor.listener.LoggingItemProcessListener;
import data.unit.reader.DefaultRepositoryItemReader;
import data.util.IndexingProperties;
import data.util.IndexingSettingsGenerator;
import eu.europeana.indexing.IndexingSettings;
import eu.europeana.indexing.exception.IndexingException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableTask
public class IndexingJobConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String BATCH_JOB = INDEXING.name();
  public static final String STEP_NAME = "indexingStep";

  @Value("${indexing.chunkSize}")
  public int chunkSize;
  @Value("${indexing.parallelizationSize}")
  public int parallelization;

  @Bean
  public Job indexingJob(JobRepository jobRepository, Step indexingStep) {
    LOGGER.info("Chunk size: {}, Parallelization size: {}", chunkSize, parallelization);
    return new JobBuilder(BATCH_JOB, jobRepository)
        .incrementer(new TimestampJobParametersIncrementer())
        .start(indexingStep)
        .build();
  }

  @Bean
  public Step indexingStep(
      JobRepository jobRepository,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
      RepositoryItemReader<ExecutionRecord> indexingRepositoryItemReader,
      ItemProcessor<ExecutionRecord, Future<ExecutionRecordDTO>> indexingAsyncItemProcessor,
      ItemWriter<Future<ExecutionRecordDTO>> executionRecordDTOAsyncItemWriter,
      LoggingItemProcessListener<ExecutionRecord> loggingItemProcessListener) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<ExecutionRecord, Future<ExecutionRecordDTO>>chunk(chunkSize, transactionManager)
        .reader(indexingRepositoryItemReader)
        .processor(indexingAsyncItemProcessor)
        .listener(loggingItemProcessListener)
        .writer(executionRecordDTOAsyncItemWriter)
        .build();
  }

  @Bean
  @StepScope
  public RepositoryItemReader<ExecutionRecord> indexingRepositoryItemReader(
      ExecutionRecordRepository<ExecutionRecord> executionRecordRepository) {
    return new DefaultRepositoryItemReader(executionRecordRepository, chunkSize);
  }

  @Bean
  public ItemProcessor<ExecutionRecord, Future<ExecutionRecordDTO>> indexingAsyncItemProcessor(
      ItemProcessor<ExecutionRecord, ExecutionRecordDTO> indexingItemProcessor,
      @Qualifier("indexingStepAsyncTaskExecutor") TaskExecutor indexingAsyncTaskExecutor) {
    AsyncItemProcessor<ExecutionRecord, ExecutionRecordDTO> asyncItemProcessor = new AsyncItemProcessor<>();
    asyncItemProcessor.setDelegate(indexingItemProcessor);
    asyncItemProcessor.setTaskExecutor(indexingAsyncTaskExecutor);
    return asyncItemProcessor;
  }

  @Bean
  @ConfigurationProperties(prefix = "indexing")
  public IndexingProperties previewIndexingProperties() {
    return new IndexingProperties();
  }

  @Bean
  public IndexingSettings indexingSettings(IndexingProperties indexingProperties) throws IndexingException {
    return new IndexingSettingsGenerator(indexingProperties).generate();
  }

  @Bean
  public TaskExecutor indexingStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix(INDEXING.name() + "-");
    executor.setCorePoolSize(parallelization);
    executor.setMaxPoolSize(parallelization);
    executor.initialize();
    return executor;
  }
}
