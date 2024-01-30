package data.unit.writer;

import data.entity.ExecutionRecordDTO;
import data.repositories.ExecutionRecordExceptionLogRepository;
import data.repositories.ExecutionRecordRepository;
import data.utility.ExecutionRecordUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class ExecutionRecordDTOItemWriter implements ItemWriter<ExecutionRecordDTO> {

  private final ExecutionRecordRepository executionRecordRepository;
  private final ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository;

  @Autowired
  public ExecutionRecordDTOItemWriter(ExecutionRecordRepository executionRecordRepository, ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository) {
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordExceptionLogRepository = executionRecordExceptionLogRepository;
  }

  @Override
  public void write(Chunk<? extends ExecutionRecordDTO> chunk) {
    for (ExecutionRecordDTO executionRecordDTO : chunk) {
      if (StringUtils.isNotBlank(executionRecordDTO.getRecordData())){
        executionRecordRepository.save(ExecutionRecordUtil.converter(executionRecordDTO));
      }
      else{
        executionRecordExceptionLogRepository.save(ExecutionRecordUtil.converterExceptionLog(executionRecordDTO));
      }
    }
  }
}
