package data.unit.writer;

import data.entity.ExecutionRecordDTO;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.stereotype.Component;

@Component
public class ExecutionRecordDTOAsyncItemWriter extends AsyncItemWriter<ExecutionRecordDTO> {

  public ExecutionRecordDTOAsyncItemWriter(ExecutionRecordDTOItemWriter executionRecordDTOItemWriter) {
    super();
    setDelegate(executionRecordDTOItemWriter);
  }
}
