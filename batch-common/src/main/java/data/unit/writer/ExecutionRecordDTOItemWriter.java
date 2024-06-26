package data.unit.writer;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import data.entity.ExecutionRecordExceptionLog;
import data.repositories.ExecutionRecordExceptionLogRepository;
import data.repositories.ExecutionRecordRepository;
import data.utility.ExecutionRecordUtil;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class ExecutionRecordDTOItemWriter implements ItemWriter<ExecutionRecordDTO> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final ExecutionRecordRepository<ExecutionRecord> executionRecordRepository;
  private final ExecutionRecordExceptionLogRepository<ExecutionRecordExceptionLog> executionRecordExceptionLogRepository;

  @Autowired
  public ExecutionRecordDTOItemWriter(ExecutionRecordRepository<ExecutionRecord> executionRecordRepository,
      ExecutionRecordExceptionLogRepository<ExecutionRecordExceptionLog> executionRecordExceptionLogRepository) {
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordExceptionLogRepository = executionRecordExceptionLogRepository;
  }

  @Override
  public void write(Chunk<? extends ExecutionRecordDTO> chunk) {
    LOGGER.info("In writer writing chunk");
    final ArrayList<ExecutionRecord> executionRecords = new ArrayList<>();
    final ArrayList<ExecutionRecordExceptionLog> executionRecordExceptionLogs = new ArrayList<>();
    for (ExecutionRecordDTO executionRecordDTO : chunk) {
      if (StringUtils.isNotBlank(executionRecordDTO.getRecordData())) {
        executionRecords.add(ExecutionRecordUtil.converterToExecutionRecord(executionRecordDTO));
      } else {
        executionRecordExceptionLogs.add(ExecutionRecordUtil.converterToExecutionRecordExceptionLog(executionRecordDTO));
      }
    }
    LOGGER.info("In writer before saveAll");
    executionRecordRepository.saveAll(executionRecords);
    executionRecordExceptionLogRepository.saveAll(executionRecordExceptionLogs);
    LOGGER.info("In writer finished writing chunk");
  }
}

