package data.utility;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import data.entity.ExecutionRecordExceptionLog;
import java.util.function.Function;

public class MethodUtil<T> {

  public ExecutionRecordDTO  executeCapturing(
      ExecutionRecord executionRecord, Function<ExecutionRecord, T> function, Function<T, String> getRecordString,
      BatchJobType batchJobType, String executionId) {
    final ExecutionRecordDTO executionRecordDTO = new ExecutionRecordDTO();
    try{
      final T result = function.apply(executionRecord);
      final ExecutionRecord resultExecutionRecord = ExecutionRecordUtil.prepareResultExecutionRecord(executionRecord,
          getRecordString.apply(result), batchJobType.name(), executionId);
      executionRecordDTO.setExecutionRecord(resultExecutionRecord);
    }
    catch (Exception exception){
      final ExecutionRecordExceptionLog executionRecordExceptionLog = ExecutionRecordUtil.prepareResultExecutionRecordExceptionLog(
          executionRecord, exception.getMessage(), batchJobType.name(), executionId);
      executionRecordDTO.setExecutionRecordExceptionLog(executionRecordExceptionLog);
    }
    return executionRecordDTO;
  }

}
