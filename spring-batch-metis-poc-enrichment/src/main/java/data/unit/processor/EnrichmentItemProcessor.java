package data.unit.processor;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import data.unit.processor.listener.MetisItemProcessor;
import data.utility.BatchJobType;
import data.utility.MethodUtil;
import eu.europeana.enrichment.rest.client.EnrichmentWorker;
import eu.europeana.enrichment.rest.client.EnrichmentWorkerImpl;
import eu.europeana.enrichment.rest.client.dereference.DereferencerProvider;
import eu.europeana.enrichment.rest.client.enrichment.EnricherProvider;
import eu.europeana.enrichment.rest.client.exceptions.DereferenceException;
import eu.europeana.enrichment.rest.client.exceptions.EnrichmentException;
import eu.europeana.enrichment.rest.client.report.ProcessedResult;
import jakarta.annotation.PostConstruct;
import java.util.function.Function;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Setter
public class EnrichmentItemProcessor implements MetisItemProcessor<ExecutionRecord, ExecutionRecordDTO, ProcessedResult<String>> {

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

  private static final BatchJobType batchJobType = BatchJobType.ENRICHMENT;
  private MethodUtil<ProcessedResult<String>> methodUtil = new MethodUtil<>();
  private final Function<ExecutionRecord, ProcessedResult<String>> function = getFunction();
  private EnrichmentWorker enrichmentWorker;

  @Override
  public Function<ExecutionRecord, ProcessedResult<String>> getFunction() {
    return executionRecord -> enrichmentWorker.process(executionRecord.getRecordData());
  }

  @Override
  public ExecutionRecordDTO process(@NotNull ExecutionRecord executionRecord) {
    return methodUtil.executeCapturing(executionRecord, function, ProcessedResult::getProcessedRecord, batchJobType,
        jobInstanceId.toString());
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
