package data.unit.reader;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.ReportingIteration.IterationResult;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeaderIterator;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Setter
public class OaiHarvestItemReader implements ItemReader<OaiRecord> {

  private static final Logger LOGGER = LoggerFactory.getLogger(OaiHarvestItemReader.class);

  @Value("#{jobParameters['oaiEndpoint']}")
  private String oaiEndpoint;
  @Value("#{jobParameters['oaiSet']}")
  private String oaiSet;
  @Value("#{jobParameters['oaiMetadataPrefix']}")
  private String oaiMetadataPrefix;

  final OaiHarvester oaiHarvester = HarvesterFactory.createOaiHarvester();
  private OaiHarvest oaiHarvest;
  private final List<OaiRecordHeader> oaiRecordHeaders = new LinkedList<>();
  private boolean isEntryPoint = true;

  @Override
  public OaiRecord read() throws Exception {
    synchronized (this) {
      if (oaiRecordHeaders.isEmpty() && isEntryPoint) {
        prepareHeaders();
        isEntryPoint = false;
      }
    }

    synchronized (this) {
      if (!oaiRecordHeaders.isEmpty()) {
        final OaiRecordHeader oaiRecordHeader = oaiRecordHeaders.getFirst();
        LOGGER.info("OaiHarvestItemReader thread: {}", Thread.currentThread());
        final OaiRecord oaiRecord = oaiHarvester.harvestRecord(oaiHarvest, oaiRecordHeader.getOaiIdentifier());
        oaiRecordHeaders.removeFirst();
        return oaiRecord;
      }
    }

    return null;
  }

  public void prepareHeaders() throws HarvesterException, IOException {
    oaiHarvest = new OaiHarvest(oaiEndpoint, oaiMetadataPrefix, oaiSet);
    //Only for repos with resumptionToken
    //    final Integer datasetSize = oaiHarvester.countRecords(oaiHarvest);
    try (OaiRecordHeaderIterator headerIterator = oaiHarvester.harvestRecordHeaders(oaiHarvest)) {
      headerIterator.forEach(oaiRecordHeader -> {
        oaiRecordHeaders.add(oaiRecordHeader);
        return IterationResult.CONTINUE;
      });
    }
  }
}
