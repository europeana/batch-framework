package data.unit.reader;

import static data.job.BatchJobType.OAI_HARVEST;

import data.entity.ExecutionRecordDTO;
import data.job.BatchJobType;
import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.ReportingIteration.IterationResult;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeaderIterator;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

//@Component
//@StepScope
@Setter
public class OaiHarvestItemReader implements ItemReader<ExecutionRecordDTO> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final BatchJobType batchJobType = OAI_HARVEST;

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
  private OaiHarvest oaiHarvest;
  private final List<OaiRecordHeader> oaiRecordHeaders = new LinkedList<>();
  private boolean isEntryPoint = true;

  @Override
  public ExecutionRecordDTO read() throws Exception {
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
        String resultString = new String(oaiRecord.getRecord().readAllBytes(), StandardCharsets.UTF_8);
        final ExecutionRecordDTO executionRecordDTO = getExecutionRecordDTO(resultString);
        oaiRecordHeaders.removeFirst();
        return executionRecordDTO;
      }
    }

    return null;
  }

  @NotNull
  private ExecutionRecordDTO getExecutionRecordDTO(String resultString) throws EuropeanaIdException {
    EuropeanaIdCreator europeanIdCreator = new EuropeanaIdCreator();
    final EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = europeanIdCreator.constructEuropeanaId(resultString, datasetId);
    final String europeanaGeneratedId = europeanaGeneratedIdsMap.getEuropeanaGeneratedId();
    final ExecutionRecordDTO executionRecordDTO = new ExecutionRecordDTO();
    executionRecordDTO.setDatasetId(datasetId);
    executionRecordDTO.setExecutionId(jobInstanceId.toString());
    executionRecordDTO.setRecordId(europeanaGeneratedId);
    executionRecordDTO.setExecutionName(batchJobType.name());
    executionRecordDTO.setRecordData(resultString);
    return executionRecordDTO;
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
