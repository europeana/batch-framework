package data.config;

import static data.job.BatchJobType.VALIDATION;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import data.job.BatchJobType;
import data.job.incrementer.TimestampJobParametersIncrementer;
import data.repositories.ExecutionRecordRepository;
import data.unit.processor.listener.LoggingItemProcessListener;
import data.unit.reader.DefaultRepositoryItemReader;
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
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableTask
public class ValidationJobConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final BatchJobType BATCH_JOB = VALIDATION;
  public static final String STEP_NAME = "validationStep";

  @Value("${validation.chunk.size}")
  public int chunkSize;
  @Value("${validation.parallelization.size}")
  public int parallelization;

  @Bean
  public Job validationBatchJob(JobRepository jobRepository, Step validationStep) {
    LOGGER.info("Chunk size: {}, Parallelization size: {}", chunkSize, parallelization);
    return new JobBuilder(BATCH_JOB.name(), jobRepository)
        .incrementer(new TimestampJobParametersIncrementer())
        .start(validationStep)
        .build();
  }

  @Bean
  public Step validationStep(JobRepository jobRepository,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
      RepositoryItemReader<ExecutionRecord> validationRepositoryItemReader,
      ItemProcessor<ExecutionRecord, Future<ExecutionRecordDTO>> validationAsyncItemProcessor,
      ItemWriter<Future<ExecutionRecordDTO>> executionRecordDTOAsyncItemWriter,
      LoggingItemProcessListener<ExecutionRecord> loggingItemProcessListener) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<ExecutionRecord, Future<ExecutionRecordDTO>>chunk(chunkSize, transactionManager)
        .reader(validationRepositoryItemReader)
        .processor(validationAsyncItemProcessor)
        .listener(loggingItemProcessListener)
        .writer(executionRecordDTOAsyncItemWriter)
        .build();
  }

  @Bean
  @StepScope
  public RepositoryItemReader<ExecutionRecord> validationRepositoryItemReader(
      ExecutionRecordRepository<ExecutionRecord> executionRecordRepository) {
    final DefaultRepositoryItemReader defaultRepositoryItemReader = new DefaultRepositoryItemReader(executionRecordRepository);
    defaultRepositoryItemReader.setPageSize(chunkSize);
    return defaultRepositoryItemReader;
  }

  @Bean
  public TaskExecutor validationStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix(VALIDATION.name() + "-");
    executor.setCorePoolSize(parallelization);
    executor.setMaxPoolSize(parallelization);
    executor.initialize();
    return executor;
  }

  @Bean
  public ItemProcessor<ExecutionRecord, Future<ExecutionRecordDTO>> validationAsyncItemProcessor(
      ItemProcessor<ExecutionRecord, ExecutionRecordDTO> validationItemProcessor,
      @Qualifier("validationStepAsyncTaskExecutor") TaskExecutor validationStepAsyncTaskExecutor) {
    AsyncItemProcessor<ExecutionRecord, ExecutionRecordDTO> asyncItemProcessor = new AsyncItemProcessor<>();
    asyncItemProcessor.setDelegate(validationItemProcessor);
    asyncItemProcessor.setTaskExecutor(validationStepAsyncTaskExecutor);
    return asyncItemProcessor;
  }
}
