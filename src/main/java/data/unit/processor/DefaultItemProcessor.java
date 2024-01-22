package data.unit.processor;

import static data.utility.ExecutionRecordUtil.prepareResultExecutionRecord;

import data.entity.ExecutionRecord;
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
public class DefaultItemProcessor implements ItemProcessor<ExecutionRecord, ExecutionRecord> {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultItemProcessor.class);

  @Value("#{stepExecution.jobExecution.jobInstance.id}")
  private Long jobInstanceId;

  @Override
  public ExecutionRecord process(ExecutionRecord executionRecord) throws Exception {
    LOGGER.info("DefaultItemProcessor thread: {}", Thread.currentThread());
    return prepareResultExecutionRecord(executionRecord, executionRecord.getRecordData(), "DEFAULT", jobInstanceId.toString());
  }
}
