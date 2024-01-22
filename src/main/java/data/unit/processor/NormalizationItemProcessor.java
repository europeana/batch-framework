package data.unit.processor;

import data.entity.ExecutionRecord;
import data.utility.BatchJobType;
import data.utility.ExecutionRecordUtil;
import eu.europeana.normalization.Normalizer;
import eu.europeana.normalization.NormalizerFactory;
import eu.europeana.normalization.model.NormalizationResult;
import lombok.Setter;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Setter
public class NormalizationItemProcessor implements ItemProcessor<ExecutionRecord, ExecutionRecord> {

  private final NormalizerFactory normalizerFactory = new NormalizerFactory();

  @Value("#{stepExecution.jobExecution.jobInstance.id}")
  private Long jobInstanceId;

  @Override
  public ExecutionRecord process(ExecutionRecord executionRecord) throws Exception {
    final Normalizer normalizer = normalizerFactory.getNormalizer();
    NormalizationResult normalizationResult = normalizer.normalize(executionRecord.getRecordData());

    return ExecutionRecordUtil.prepareResultExecutionRecord(executionRecord, normalizationResult.getNormalizedRecordInEdmXml(), BatchJobType.NORMALIZATION.name(), jobInstanceId.toString());
  }
}
