package data.unit.processor.listener;

import data.entity.ExecutionRecordDTO;
import data.entity.ExecutionRecordIdentifier;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.Future;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Setter
public class LoggingItemProcessListener<T extends ExecutionRecordIdentifier> implements
    ItemProcessListener<T, Future<ExecutionRecordDTO>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("#{stepExecution.jobExecution.jobInstance.id}")
  private Long jobInstanceId;

  @Override
  public void beforeProcess(@NotNull T item) {
    LOGGER.debug("beforeProcess");
  }

  @Override
  public void afterProcess(@NotNull T item, Future<ExecutionRecordDTO> future) {
    LOGGER.info("Processing jobId {}, datasetId, executionId, recordId: {}, {}, {}",
        jobInstanceId, item.getDatasetId(), item.getExecutionId(), item.getRecordId());
  }

  @Override
  public void onProcessError(@NotNull T executionRecord, @NotNull Exception e) {
    LOGGER.error(" onProcessError");
  }

}
