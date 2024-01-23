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
  public static final int CHUNK_SIZE = 10;
  public static final int PARALLELIZATION = 10;

  @Bean
  public Job oaiHarvestBatchJob(JobRepository jobRepository, Step oaiHarvestStep) {
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
        .<OaiRecord, ExecutionRecord>chunk(CHUNK_SIZE, transactionManager)
        .reader(oaiHarvestItemReader)
        .processor(oaiHarvestItemProcessor)
        .writer(writer)
        .listener(delayLoggingItemProcessListener)
        .taskExecutor(oaiHarvestStepAsyncTaskExecutor)
        .build();
  }

  @Bean
  public TaskExecutor oaiHarvestStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(PARALLELIZATION);
    executor.setMaxPoolSize(PARALLELIZATION);
    executor.initialize();
    return executor;
  }
}
