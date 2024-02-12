package data.utility;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import data.entity.ExecutionRecordExceptionLog;
import data.entity.ExecutionRecordKey;

public class ExecutionRecordUtil {

  private ExecutionRecordUtil() {
  }

  public static ExecutionRecordDTO converterToExecutionRecordDTO(ExecutionRecord executionRecord){
    final ExecutionRecordDTO executionRecordDTO = new ExecutionRecordDTO();
    executionRecordDTO.setDatasetId(executionRecord.getExecutionRecordKey().getDatasetId());
    executionRecordDTO.setExecutionId(executionRecord.getExecutionRecordKey().getExecutionId());
    executionRecordDTO.setRecordId(executionRecord.getExecutionRecordKey().getRecordId());
    executionRecordDTO.setExecutionName(executionRecord.getExecutionName());
    executionRecordDTO.setRecordData(executionRecord.getRecordData());
    return executionRecordDTO;
  }

  public static ExecutionRecord converterToExecutionRecord(ExecutionRecordDTO executionRecordDTO){
    final ExecutionRecord executionRecord = new ExecutionRecord();
    final ExecutionRecordKey executionRecordKey = new ExecutionRecordKey();
    executionRecordKey.setDatasetId(executionRecordDTO.getDatasetId());
    executionRecordKey.setExecutionId(executionRecordDTO.getExecutionId());
    executionRecordKey.setRecordId(executionRecordDTO.getRecordId());
    executionRecord.setExecutionRecordKey(executionRecordKey);
    executionRecord.setExecutionName(executionRecordDTO.getExecutionName());
    executionRecord.setRecordData(executionRecordDTO.getRecordData());
    return executionRecord;
  }

  public static ExecutionRecordExceptionLog converterToExecutionRecordExceptionLog(ExecutionRecordDTO executionRecordDTO){
    final ExecutionRecordExceptionLog executionRecordExceptionLog = new ExecutionRecordExceptionLog();
    final ExecutionRecordKey executionRecordKey = new ExecutionRecordKey();
    executionRecordKey.setDatasetId(executionRecordDTO.getDatasetId());
    executionRecordKey.setExecutionId(executionRecordDTO.getExecutionId());
    executionRecordKey.setRecordId(executionRecordDTO.getRecordId());
    executionRecordExceptionLog.setExecutionRecordKey(executionRecordKey);
    executionRecordExceptionLog.setExecutionName(executionRecordDTO.getExecutionName());
    executionRecordExceptionLog.setException(executionRecordDTO.getException());
    return executionRecordExceptionLog;
  }

  public static ExecutionRecordDTO createSuccessExecutionRecordDTO(ExecutionRecordDTO executionRecordDTO, String updatedRecordString,
      BatchJobType batchJobType, String executionId){
    final ExecutionRecordDTO resultExecutionRecordDTO = new ExecutionRecordDTO();
    resultExecutionRecordDTO.setDatasetId(executionRecordDTO.getDatasetId());
    resultExecutionRecordDTO.setExecutionId(executionId);
    resultExecutionRecordDTO.setRecordId(executionRecordDTO.getRecordId());
    resultExecutionRecordDTO.setExecutionName(batchJobType.name());
    resultExecutionRecordDTO.setRecordData(updatedRecordString);
    resultExecutionRecordDTO.setException("");
    return resultExecutionRecordDTO;
  }

  public static ExecutionRecordDTO createFailureExecutionRecordDTO(ExecutionRecordDTO executionRecordDTO, String errorMessage,
      BatchJobType batchJobType, String executionId){
    final ExecutionRecordDTO resultExecutionRecordDTO = new ExecutionRecordDTO();
    resultExecutionRecordDTO.setDatasetId(executionRecordDTO.getDatasetId());
    resultExecutionRecordDTO.setExecutionId(executionId);
    resultExecutionRecordDTO.setRecordId(executionRecordDTO.getRecordId());
    resultExecutionRecordDTO.setExecutionName(batchJobType.name());
    resultExecutionRecordDTO.setRecordData("");
    resultExecutionRecordDTO.setException(errorMessage);
    return resultExecutionRecordDTO;
  }
}
