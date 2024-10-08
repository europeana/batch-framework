package data.unit.processor;

import static data.job.BatchJobType.ENRICHMENT;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import data.unit.processor.listener.MetisItemProcessor;
import data.job.BatchJobType;
import data.utility.ExecutionRecordUtil;
import data.utility.ItemProcessorUtil;
import eu.europeana.enrichment.rest.client.EnrichmentWorker;
import eu.europeana.enrichment.rest.client.EnrichmentWorkerImpl;
import eu.europeana.enrichment.rest.client.dereference.DereferencerProvider;
import eu.europeana.enrichment.rest.client.enrichment.EnricherProvider;
import eu.europeana.enrichment.rest.client.exceptions.DereferenceException;
import eu.europeana.enrichment.rest.client.exceptions.EnrichmentException;
import eu.europeana.enrichment.rest.client.report.ProcessedResult;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

@Component
@StepScope
@Setter
public class EnrichmentItemProcessor implements MetisItemProcessor<ExecutionRecord, ExecutionRecordDTO, ProcessedResult<String>> {

  private static final BatchJobType batchJobType = ENRICHMENT;

  @Value("${enrichment.dereference-url}")
  private String dereferenceURL;
  @Value("${enrichment.entity-management-url}")
  private String enrichmentEntityManagementUrl;
  @Value("${enrichment.entity-api-url}")
  private String enrichmentEntityApiUrl;
  @Value("${enrichment.entity-api-key}")
  private String enrichmentEntityApiKey;
  @Value("#{jobParameters['overrideJobId'] ?: stepExecution.jobExecution.jobInstance.id}")
  private Long jobInstanceId;

  private final ItemProcessorUtil<ProcessedResult<String>> itemProcessorUtil;
  private EnrichmentWorker enrichmentWorker;

  public EnrichmentItemProcessor() {
    itemProcessorUtil = new ItemProcessorUtil<>(getFunction(), ProcessedResult::getProcessedRecord);
  }

  @Override
  public ThrowingFunction<ExecutionRecordDTO, ProcessedResult<String>> getFunction() {
    return executionRecord -> enrichmentWorker.process(executionRecord.getRecordData());
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

  @Override
  public ExecutionRecordDTO process(@NotNull ExecutionRecord executionRecord) {
    final ExecutionRecordDTO executionRecordDTO = ExecutionRecordUtil.converterToExecutionRecordDTO(executionRecord);
    return itemProcessorUtil.processCapturingException(executionRecordDTO, batchJobType, jobInstanceId.toString());
  }

}
