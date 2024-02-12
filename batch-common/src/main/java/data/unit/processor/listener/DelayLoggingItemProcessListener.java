package data.unit.processor.listener;

import data.entity.ExecutionRecordDTO;
import java.lang.invoke.MethodHandles;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
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
public class DelayLoggingItemProcessListener<T> implements ItemProcessListener<T, ExecutionRecordDTO> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("#{stepExecution.jobExecution.jobInstance.id}")
  private Long jobInstanceId;

  @Override
  public void beforeProcess(@NotNull T item) {
    LOGGER.debug("beforeProcess");
  }

  @Override
  public void afterProcess(@NotNull T item, ExecutionRecordDTO executionRecordDTO) {
    LOGGER.info("DelayLoggingItemProcessListener thread: {}", Thread.currentThread());
    if (StringUtils.isNotBlank(executionRecordDTO.getRecordData())){
      LOGGER.info("AfterProcess LOG_DELAY success jobId {}, datasetId, executionId, recordId: {}, {}, {}",
          jobInstanceId,
          executionRecordDTO.getDatasetId(),
          executionRecordDTO.getExecutionId(),
          executionRecordDTO.getRecordId());
    }
    else{
      LOGGER.info("AfterProcess LOG_DELAY failure jobId {}, datasetId: {}, executionId: {}, recordId: {}, exception: {}",
          jobInstanceId,
          executionRecordDTO.getDatasetId(),
          executionRecordDTO.getExecutionId(),
          executionRecordDTO.getRecordId(),
          executionRecordDTO.getException()
      );
    }
//    try {
//      Thread.sleep(1000);
//    } catch (InterruptedException e) {
//      Thread.currentThread().interrupt();
//      throw new RuntimeException(e);
//    }
  }

  @Override
  public void onProcessError(@NotNull T item, @NotNull Exception e) {
    LOGGER.error(" onProcessError");
  }


}
