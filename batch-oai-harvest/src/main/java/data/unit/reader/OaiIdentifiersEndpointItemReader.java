package data.unit.reader;

import data.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeaderIterator;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class OaiIdentifiersEndpointItemReader implements ItemReader<ExecutionRecordExternalIdentifier> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("#{jobParameters['oaiEndpoint']}")
    private String oaiEndpoint;
    @Value("#{jobParameters['oaiSet']}")
    private String oaiSet;
    @Value("#{jobParameters['oaiMetadataPrefix']}")
    private String oaiMetadataPrefix;
    @Value("#{jobParameters['datasetId']}")
    private String datasetId;
    @Value("#{stepExecution.jobExecution.jobInstance.id}")
    private Long jobInstanceId;

    final OaiHarvester oaiHarvester = HarvesterFactory.createOaiHarvester();
    private final List<OaiRecordHeader> oaiRecordHeaders = new LinkedList<>();

    @PostConstruct
    private void prepare() throws HarvesterException, IOException {
        harvestIdentifiers();
    }

    @Override
    public ExecutionRecordExternalIdentifier read() {

        final OaiRecordHeader oaiRecordHeader = takeIdentifier();
        if (oaiRecordHeader != null) {
            ExecutionRecordExternalIdentifier recordIdentifier = new ExecutionRecordExternalIdentifier();
            recordIdentifier.setDatasetId(datasetId);
            recordIdentifier.setRecordId(oaiRecordHeader.getOaiIdentifier());
            recordIdentifier.setExecutionId(jobInstanceId.toString());
            recordIdentifier.setDeleted(oaiRecordHeader.isDeleted());

            return recordIdentifier;
        } else {
            return null;
        }
    }

    private void harvestIdentifiers() throws HarvesterException, IOException {
        LOGGER.info("Harvesting identifiers for {}", oaiEndpoint);
        OaiHarvest oaiHarvest = new OaiHarvest(oaiEndpoint, oaiMetadataPrefix, oaiSet);
        try (OaiRecordHeaderIterator headerIterator = oaiHarvester.harvestRecordHeaders(oaiHarvest)) {
            headerIterator.forEach(oaiRecordHeader -> {
                oaiRecordHeaders.add(oaiRecordHeader);
                return ReportingIteration.IterationResult.CONTINUE;
            });
        }
        LOGGER.info("Identifiers harvested");
    }

    private synchronized OaiRecordHeader takeIdentifier() {
        if (!oaiRecordHeaders.isEmpty()) {
            return oaiRecordHeaders.removeFirst();
        } else {
            return null;
        }
    }
}
