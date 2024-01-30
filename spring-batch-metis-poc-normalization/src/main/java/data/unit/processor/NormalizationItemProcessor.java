package data.unit.processor;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import data.unit.processor.listener.MetisItemProcessor;
import data.utility.BatchJobType;
import data.utility.ExecutionRecordUtil;
import data.utility.MethodUtil;
import eu.europeana.normalization.Normalizer;
import eu.europeana.normalization.NormalizerFactory;
import eu.europeana.normalization.model.NormalizationResult;
import eu.europeana.normalization.util.NormalizationConfigurationException;
import eu.europeana.normalization.util.NormalizationException;
import java.util.function.Function;
import lombok.Setter;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Setter
public class NormalizationItemProcessor implements MetisItemProcessor<ExecutionRecord, ExecutionRecordDTO, NormalizationResult> {

  @Value("#{stepExecution.jobExecution.jobInstance.id}")
  private Long jobInstanceId;

  private static final BatchJobType batchJobType = BatchJobType.NORMALIZATION;
  private MethodUtil<NormalizationResult> methodUtil = new MethodUtil<>();
  private final Function<ExecutionRecordDTO, NormalizationResult> function = getFunction();
  private final NormalizerFactory normalizerFactory = new NormalizerFactory();

  @Override
  public Function<ExecutionRecordDTO, NormalizationResult> getFunction() {
    return executionRecord -> {
      final Normalizer normalizer;
      try {
        normalizer = normalizerFactory.getNormalizer();
        return normalizer.normalize(executionRecord.getRecordData());
      } catch (NormalizationConfigurationException e) {
        throw new RuntimeException(e);
      } catch (NormalizationException e) {
        throw new RuntimeException(e);
      }
    };
  }

  @Override
  public ExecutionRecordDTO process(ExecutionRecord executionRecord) throws Exception {
    final ExecutionRecordDTO executionRecordDTO = ExecutionRecordUtil.converter(executionRecord);
    return methodUtil.executeCapturing(executionRecordDTO, function, NormalizationResult::getNormalizedRecordInEdmXml, batchJobType,
        jobInstanceId.toString());
  }
}
