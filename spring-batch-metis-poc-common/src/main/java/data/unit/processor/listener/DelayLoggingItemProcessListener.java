package data.unit.processor.listener;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import data.entity.ExecutionRecordExceptionLog;
import java.lang.invoke.MethodHandles;
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
public class DelayLoggingItemProcessListener<T> implements ItemProcessListener<T, ExecutionRecordDTO> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("#{stepExecution.jobExecution.jobInstance.id}")
  private Long jobInstanceId;

  @Override
  public void beforeProcess(@NotNull T item) {
    LOGGER.debug("beforeProcess");
  }

  @Override
  public void afterProcess(@NotNull T item, ExecutionRecordDTO executionRecord) {
    final ExecutionRecord executionRecord1 = executionRecord.getExecutionRecord();
    final ExecutionRecordExceptionLog executionRecordExceptionLog = executionRecord.getExecutionRecordExceptionLog();

    if (executionRecord1 != null){
      LOGGER.info("AfterProcess LOG_DELAY success jobId {}, datasetId, executionId, recordId: {}, {}, {}",
          jobInstanceId,
          executionRecord1.getExecutionRecordKey().getDatasetId(),
          executionRecord1.getExecutionRecordKey().getExecutionId(),
          executionRecord1.getExecutionRecordKey().getRecordId());
    }
    else{
      LOGGER.info("AfterProcess LOG_DELAY failure jobId {}, datasetId: {}, executionId: {}, recordId: {}, exception: {}",
          jobInstanceId,
          executionRecordExceptionLog.getExecutionRecordKey().getDatasetId(),
          executionRecordExceptionLog.getExecutionRecordKey().getExecutionId(),
          executionRecordExceptionLog.getExecutionRecordKey().getRecordId(),
          executionRecordExceptionLog.getException()
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
