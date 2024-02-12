package data.unit.processor;

import static data.job.BatchJobType.NORMALIZATION;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import data.unit.processor.listener.MetisItemProcessor;
import data.job.BatchJobType;
import data.utility.ExecutionRecordUtil;
import data.utility.ItemProcessorUtil;
import eu.europeana.normalization.NormalizerFactory;
import eu.europeana.normalization.model.NormalizationResult;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

@Component
@StepScope
@Setter
public class NormalizationItemProcessor implements MetisItemProcessor<ExecutionRecord, ExecutionRecordDTO, NormalizationResult> {

  private static final BatchJobType batchJobType = NORMALIZATION;

  @Value("#{stepExecution.jobExecution.jobInstance.id}")
  private Long jobInstanceId;

  private final ItemProcessorUtil<NormalizationResult> itemProcessorUtil;
  private final NormalizerFactory normalizerFactory = new NormalizerFactory();

  public NormalizationItemProcessor() {
    itemProcessorUtil = new ItemProcessorUtil<>(getFunction(), NormalizationResult::getNormalizedRecordInEdmXml);
  }

  @Override
  public ThrowingFunction<ExecutionRecordDTO, NormalizationResult> getFunction() {
    return executionRecord -> normalizerFactory.getNormalizer().normalize(executionRecord.getRecordData());
  }

  @Override
  public ExecutionRecordDTO process(@NotNull ExecutionRecord executionRecord) {
    final ExecutionRecordDTO executionRecordDTO = ExecutionRecordUtil.converterToExecutionRecordDTO(executionRecord);
    return itemProcessorUtil.processCapturingException(executionRecordDTO, batchJobType, jobInstanceId.toString());
  }
}
