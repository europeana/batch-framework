package data.config;

import data.entity.ExecutionRecord;
import data.unit.processor.listener.DelayLoggingItemProcessListener;
import data.unit.reader.OaiHarvestItemReader;
import data.utility.BatchJobType;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class OaiHarvestJobConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String BATCH_JOB = BatchJobType.OAI_HARVEST.name();
  public static final String STEP_NAME = "oaiHarvestStep";
  @Value("${oaiharvest.chunk.size}")
  public int chunkSize;
  @Value("${oaiharvest.parallelization.size}")
  public int parallelization;

  @Bean
  public Job oaiHarvestBatchJob(JobRepository jobRepository, Step oaiHarvestStep) {
    LOGGER.info("Chunk size: {}, Parallelization size: {}", chunkSize, parallelization);
    return new JobBuilder(BATCH_JOB, jobRepository)
        .start(oaiHarvestStep)
        .build();
  }

  @Bean
  public Step oaiHarvestStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
      OaiHarvestItemReader oaiHarvestItemReader,
      ItemProcessor<OaiRecord, ExecutionRecord> oaiHarvestItemProcessor,
      RepositoryItemWriter<ExecutionRecord> writer,
      DelayLoggingItemProcessListener<OaiRecord> delayLoggingItemProcessListener,
      TaskExecutor oaiHarvestStepAsyncTaskExecutor) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<OaiRecord, ExecutionRecord>chunk(chunkSize, transactionManager)
        .reader(oaiHarvestItemReader)
        .processor(oaiHarvestItemProcessor)
        .writer(writer)
        .listener(delayLoggingItemProcessListener)
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
}
