package data.processor;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import data.job.BatchJobType;
import data.utility.ExecutionRecordUtil;
import eu.europeana.indexing.IndexerFactory;
import eu.europeana.indexing.IndexingSettings;
import eu.europeana.indexing.tiers.model.MediaTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

@Component
@StepScope
public class IndexingItemProcessor implements ItemProcessor<ExecutionRecord, ExecutionRecordDTO> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("#{jobParameters['preserveTimestamps']}")
    public boolean preserveTimestamps;

    @Value("#{jobParameters['performRedirects']}")
    public boolean performRedirect;

    @Value("#{jobParameters['recordDate']}")
    private String recordDate;

    private final IndexingSettings indexingSettings;

    public IndexingItemProcessor(IndexingSettings indexingSettings) {
        this.indexingSettings = indexingSettings;
    }

    @Override
    public ExecutionRecordDTO process(ExecutionRecord executionRecord) {
        LOGGER.info("Indexing record: {}", executionRecord.getExecutionRecordKey().getRecordId());

        try (eu.europeana.indexing.Indexer indexer = new IndexerFactory(indexingSettings).getIndexer()) {
            final var properties = new eu.europeana.indexing.IndexingProperties(
                    Optional.ofNullable(recordDate)
                            .map(Instant::parse)
                            .map(Date::from)
                            .orElse(null),
                    preserveTimestamps,
                    Collections.emptyList(),
                    performRedirect,
                    true);

            LOGGER.info("Indexing: {}", executionRecord.getExecutionRecordKey().getRecordId());
            indexer.index(executionRecord.getRecordData(), properties, tier -> tier.getMediaTier() != MediaTier.T0);
            LOGGER.info("Indexed: {}", executionRecord.getExecutionRecordKey().getRecordId());
        } catch (Exception exception) {
            LOGGER.error("Unable to index record {}", executionRecord.getExecutionRecordKey().getRecordId(), exception);
            final ExecutionRecordDTO resultExecutionRecordDTO = new ExecutionRecordDTO();
            resultExecutionRecordDTO.setDatasetId(executionRecord.getExecutionRecordKey().getDatasetId());
            resultExecutionRecordDTO.setExecutionId(executionRecord.getExecutionRecordKey().getExecutionId());
            resultExecutionRecordDTO.setRecordId(executionRecord.getExecutionRecordKey().getRecordId());
            resultExecutionRecordDTO.setRecordData("");
            resultExecutionRecordDTO.setExecutionName(BatchJobType.INDEXING.name());
            resultExecutionRecordDTO.setException(exception.getMessage());
            return resultExecutionRecordDTO;
        }

        return ExecutionRecordUtil.converterToExecutionRecordDTO(executionRecord);
    }
}
