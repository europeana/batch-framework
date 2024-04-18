package data.config;

import static data.job.BatchJobType.OAI_HARVEST;

import data.entity.ExecutionRecordDTO;
import data.entity.ExecutionRecordExternalIdentifier;
import data.job.incrementer.TimestampJobParametersIncrementer;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.Future;

import data.unit.reader.OaiIdentifiersEndpointItemReader;
import data.unit.reader.OaiIdentifiersRepositoryItemReader;
import data.unit.writer.OaiIdentifiersWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
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
    public static final String IDENTIFIERS_HARVEST_STEP_NAME = "identifiersHarvest";
    public static final String RECORDS_HARVEST_STEP_NAME = "recordsHarvest";

    @Value("${oaiharvest.chunk.size}")
    public int chunkSize;
    @Value("${oaiharvest.parallelization.size}")
    public int parallelization;

    @Bean
    public Job oaiHarvestJob(
            JobRepository jobRepository,
            @Qualifier("oaiIdentifiersHarvestStep") Step oaiIdentifiersHarvestStep,
            @Qualifier("oaiRecordsHarvestStep") Step oaiRecordsHarvestStep) {

        LOGGER.info("Chunk size: {}, Parallelization size: {}", chunkSize, parallelization);
        return new JobBuilder(BATCH_JOB, jobRepository)
                .incrementer(new TimestampJobParametersIncrementer())
                .start(oaiIdentifiersHarvestStep)
                .next(oaiRecordsHarvestStep)
                .build();
    }

    //TODO: 2024-04-18 - Potentially update this step in a parallelized way
    @Bean("oaiIdentifiersHarvestStep")
    public Step oaiRepositoryIdentifiersHarvestStep(
            OaiIdentifiersEndpointItemReader oaiIdentifiersEndpointItemReader,
            OaiIdentifiersWriter oaiIdentifiersWriter,
            JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("oaiHarvestStepAsyncTaskExecutor") TaskExecutor oaiHarvestStepAsyncTaskExecutor) {

        return new StepBuilder(IDENTIFIERS_HARVEST_STEP_NAME, jobRepository)
                .<ExecutionRecordExternalIdentifier, ExecutionRecordExternalIdentifier>chunk(chunkSize, transactionManager)
                .reader(oaiIdentifiersEndpointItemReader)
                .writer(oaiIdentifiersWriter)
//                .taskExecutor(oaiHarvestStepAsyncTaskExecutor)
                .build();
    }

    @Bean("oaiRecordsHarvestStep")
    public Step oaiRecordsHarvestStep(
            JobRepository jobRepository,
            OaiIdentifiersRepositoryItemReader oaiIdentifiersRepositoryItemReader,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            ItemProcessor<ExecutionRecordExternalIdentifier, Future<ExecutionRecordDTO>> oaiRecordAsyncItemProcessor,
            ItemWriter<Future<ExecutionRecordDTO>> executionRecordDTOAsyncItemWriter) {

        return new StepBuilder(RECORDS_HARVEST_STEP_NAME, jobRepository)
                .<ExecutionRecordExternalIdentifier, Future<ExecutionRecordDTO>>chunk(chunkSize, transactionManager)
                .reader(oaiIdentifiersRepositoryItemReader)
                .processor(oaiRecordAsyncItemProcessor)
                .writer(executionRecordDTOAsyncItemWriter)
                .build();
    }

    @Bean
    public ItemProcessor<ExecutionRecordExternalIdentifier, Future<ExecutionRecordDTO>> oaiRecordAsyncItemProcessor(
            ItemProcessor<ExecutionRecordExternalIdentifier, ExecutionRecordDTO> oaiRecordItemProcessor,
            @Qualifier("oaiHarvestStepAsyncTaskExecutor") TaskExecutor taskExecutor) {
        AsyncItemProcessor<ExecutionRecordExternalIdentifier, ExecutionRecordDTO> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setDelegate(oaiRecordItemProcessor);
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        return asyncItemProcessor;
    }

    @Bean
    public TaskExecutor oaiHarvestStepAsyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(OAI_HARVEST.name() + "Thread-");
        executor.setCorePoolSize(parallelization);
        executor.setMaxPoolSize(parallelization);
        executor.initialize();
        return executor;
    }
}
