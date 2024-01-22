package data.unit.processor;

import data.entity.ExecutionRecord;
import data.utility.BatchJobType;
import data.utility.ExecutionRecordUtil;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import java.nio.charset.StandardCharsets;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Setter
public class OaiHarvestItemProcessor implements ItemProcessor<OaiRecord, ExecutionRecord> {

  private static final Logger LOGGER = LoggerFactory.getLogger(OaiHarvestItemProcessor.class);
  @Value("#{jobParameters['datasetId']}")
  private String datasetId;
  @Value("#{stepExecution.jobExecution.jobInstance.id}")
  private Long jobInstanceId;

  @Override
  public ExecutionRecord process(OaiRecord oaiRecord) throws Exception {
    LOGGER.info("OaiHarvestItemProcessor thread: {}", Thread.currentThread());
    String resultString = new String(oaiRecord.getRecord().readAllBytes(), StandardCharsets.UTF_8);
    EuropeanaIdCreator europeanIdCreator = new EuropeanaIdCreator();
    final EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = europeanIdCreator.constructEuropeanaId(resultString, datasetId);
    final String europeanaGeneratedId = europeanaGeneratedIdsMap.getEuropeanaGeneratedId();
    return ExecutionRecordUtil.prepareResultExecutionRecord(datasetId, jobInstanceId.toString(), europeanaGeneratedId,
        BatchJobType.OAI_HARVEST.name(), resultString);
  }
}
