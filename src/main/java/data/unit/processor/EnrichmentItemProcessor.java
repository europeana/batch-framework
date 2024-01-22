package data.unit.processor;

import data.entity.ExecutionRecord;
import data.utility.BatchJobType;
import data.utility.ExecutionRecordUtil;
import eu.europeana.enrichment.rest.client.EnrichmentWorker;
import eu.europeana.enrichment.rest.client.EnrichmentWorkerImpl;
import eu.europeana.enrichment.rest.client.dereference.DereferencerProvider;
import eu.europeana.enrichment.rest.client.enrichment.EnricherProvider;
import eu.europeana.enrichment.rest.client.exceptions.DereferenceException;
import eu.europeana.enrichment.rest.client.exceptions.EnrichmentException;
import eu.europeana.enrichment.rest.client.report.ProcessedResult;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Setter
public class EnrichmentItemProcessor implements ItemProcessor<ExecutionRecord, ExecutionRecord> {

  @Value("${enrichment.dereference-url}")
  private String dereferenceURL;
  @Value("${enrichment.entity-management-url}")
  private String enrichmentEntityManagementUrl;
  @Value("${enrichment.entity-api-url}")
  private String enrichmentEntityApiUrl;
  @Value("${enrichment.entity-api-key}")
  private String enrichmentEntityApiKey;

  @Value("#{stepExecution.jobExecution.jobInstance.id}")
  private Long jobInstanceId;
  private EnrichmentWorker enrichmentWorker;

  @Override
  public ExecutionRecord process(ExecutionRecord executionRecord) {
    ProcessedResult<String> result = enrichmentWorker.process(executionRecord.getRecordData());
    return ExecutionRecordUtil.prepareResultExecutionRecord(executionRecord, result.getProcessedRecord(),
        BatchJobType.ENRICHMENT.name(), jobInstanceId.toString());
  }

  @PostConstruct
  private void postConstruct() throws DereferenceException, EnrichmentException {
    final EnricherProvider enricherProvider = new EnricherProvider();
    enricherProvider.setEnrichmentPropertiesValues(enrichmentEntityManagementUrl, enrichmentEntityApiUrl, enrichmentEntityApiKey);
    final DereferencerProvider dereferencerProvider = new DereferencerProvider();
    dereferencerProvider.setDereferenceUrl(dereferenceURL);
    dereferencerProvider.setEnrichmentPropertiesValues(enrichmentEntityManagementUrl, enrichmentEntityApiUrl,
        enrichmentEntityApiKey);
    enrichmentWorker = new EnrichmentWorkerImpl(dereferencerProvider.create(), enricherProvider.create());
  }

}
