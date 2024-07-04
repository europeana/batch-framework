package data.processor;

import static data.job.BatchJobType.INDEXING;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import data.job.BatchJobType;
import data.unit.processor.listener.MetisItemProcessor;
import data.utility.ExecutionRecordUtil;
import data.utility.ItemProcessorUtil;
import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.IndexerFactory;
import eu.europeana.indexing.IndexingSettings;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Date;
import java.util.function.Function;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

@Component
@StepScope
@Setter
public class IndexingItemProcessor implements MetisItemProcessor<ExecutionRecord, ExecutionRecordDTO, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final BatchJobType batchJobType = INDEXING;

    @Value("${indexing.preserveTimestamps}")
    public boolean preserveTimestamps;

    @Value("${indexing.performRedirects}")
    public boolean performRedirect;
    @Value("#{jobParameters['overrideJobId'] ?: stepExecution.jobExecution.jobInstance.id}")
    private Long jobInstanceId;

    private final Date recordDate;
    private final ItemProcessorUtil<String> itemProcessorUtil;
    private final IndexingSettings indexingSettings;

    public IndexingItemProcessor(IndexingSettings indexingSettings) {
        this.recordDate = new Date();
        this.indexingSettings = indexingSettings;
        itemProcessorUtil = new ItemProcessorUtil<>(getFunction(), Function.identity());
    }

    @Override
    public ThrowingFunction<ExecutionRecordDTO, String> getFunction() {
        return executionRecord -> {
            LOGGER.info("Indexing record: {}", executionRecord.getRecordId());

            try(Indexer indexer = new IndexerFactory(indexingSettings).getIndexer()) {
                final var properties = new eu.europeana.indexing.IndexingProperties(
                    recordDate, preserveTimestamps, Collections.emptyList(), performRedirect, true);

                LOGGER.info("Indexing: {}", executionRecord.getRecordId());
                indexer.index(executionRecord.getRecordData(), properties, tier -> true);
                LOGGER.info("Indexed: {}", executionRecord.getRecordId());
                return executionRecord.getRecordData();
            }
        };
    }

    @Override
    public ExecutionRecordDTO process(@NotNull ExecutionRecord executionRecord) {
        final ExecutionRecordDTO executionRecordDTO = ExecutionRecordUtil.converterToExecutionRecordDTO(executionRecord);
        return itemProcessorUtil.processCapturingException(executionRecordDTO, batchJobType, jobInstanceId.toString());
    }
}
