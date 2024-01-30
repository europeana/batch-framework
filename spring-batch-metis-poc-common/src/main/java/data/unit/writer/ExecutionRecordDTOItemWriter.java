package data.unit.writer;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import data.entity.ExecutionRecordExceptionLog;
import data.repositories.ExecutionRecordExceptionLogRepository;
import data.repositories.ExecutionRecordRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExecutionRecordDTOItemWriter implements ItemWriter<ExecutionRecordDTO> {

  private final ExecutionRecordRepository executionRecordRepository;
  private final ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository;

  @Autowired
  public ExecutionRecordDTOItemWriter(ExecutionRecordRepository executionRecordRepository, ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository) {
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordExceptionLogRepository = executionRecordExceptionLogRepository;
  }

  @Override
  public void write(Chunk<? extends ExecutionRecordDTO> chunk) throws Exception {
    for (ExecutionRecordDTO item : chunk) {
      final ExecutionRecord executionRecord = item.getExecutionRecord();
      final ExecutionRecordExceptionLog executionRecordExceptionLog = item.getExecutionRecordExceptionLog();
      if ( executionRecord != null){
        executionRecordRepository.save(executionRecord);
      }
      else{
        executionRecordExceptionLogRepository.save(executionRecordExceptionLog);
      }
    }
  }
}
