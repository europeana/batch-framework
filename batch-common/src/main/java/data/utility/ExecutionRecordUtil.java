package data.utility;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import data.entity.ExecutionRecordExceptionLog;

public class ExecutionRecordUtil {

  private ExecutionRecordUtil() {
  }

  public static ExecutionRecordDTO converterToExecutionRecordDTO(ExecutionRecord executionRecord){
    final ExecutionRecordDTO executionRecordDTO = new ExecutionRecordDTO();
    executionRecordDTO.setDatasetId(executionRecord.getDatasetId());
    executionRecordDTO.setExecutionId(executionRecord.getExecutionId());
    executionRecordDTO.setRecordId(executionRecord.getRecordId());
    executionRecordDTO.setExecutionName(executionRecord.getExecutionName());
    executionRecordDTO.setRecordData(executionRecord.getRecordData());
    return executionRecordDTO;
  }

  public static ExecutionRecord converterToExecutionRecord(ExecutionRecordDTO executionRecordDTO){
    final ExecutionRecord executionRecord = new ExecutionRecord();
    executionRecord.setDatasetId(executionRecordDTO.getDatasetId());
    executionRecord.setExecutionId(executionRecordDTO.getExecutionId());
    executionRecord.setRecordId(executionRecordDTO.getRecordId());
    executionRecord.setExecutionName(executionRecordDTO.getExecutionName());
    executionRecord.setRecordData(executionRecordDTO.getRecordData());
    return executionRecord;
  }

  public static ExecutionRecordExceptionLog converterToExecutionRecordExceptionLog(ExecutionRecordDTO executionRecordDTO){
    final ExecutionRecordExceptionLog executionRecordExceptionLog = new ExecutionRecordExceptionLog();
    executionRecordExceptionLog.setDatasetId(executionRecordDTO.getDatasetId());
    executionRecordExceptionLog.setExecutionId(executionRecordDTO.getExecutionId());
    executionRecordExceptionLog.setRecordId(executionRecordDTO.getRecordId());
    executionRecordExceptionLog.setExecutionName(executionRecordDTO.getExecutionName());
    executionRecordExceptionLog.setException(executionRecordDTO.getException());
    return executionRecordExceptionLog;
  }

  public static ExecutionRecordDTO createSuccessExecutionRecordDTO(ExecutionRecordDTO executionRecordDTO, String updatedRecordString,
      String executionName, String executionId){
    final ExecutionRecordDTO resultExecutionRecordDTO = new ExecutionRecordDTO();
    resultExecutionRecordDTO.setDatasetId(executionRecordDTO.getDatasetId());
    resultExecutionRecordDTO.setExecutionId(executionId);
    resultExecutionRecordDTO.setRecordId(executionRecordDTO.getRecordId());
    resultExecutionRecordDTO.setExecutionName(executionName);
    resultExecutionRecordDTO.setRecordData(updatedRecordString);
    resultExecutionRecordDTO.setException("");
    return resultExecutionRecordDTO;
  }

  public static ExecutionRecordDTO createFailureExecutionRecordDTO(ExecutionRecordDTO executionRecordDTO, String errorMessage,
      String executionName, String executionId){
    final ExecutionRecordDTO resultExecutionRecordDTO = new ExecutionRecordDTO();
    resultExecutionRecordDTO.setDatasetId(executionRecordDTO.getDatasetId());
    resultExecutionRecordDTO.setExecutionId(executionId);
    resultExecutionRecordDTO.setRecordId(executionRecordDTO.getRecordId());
    resultExecutionRecordDTO.setExecutionName(executionName);
    resultExecutionRecordDTO.setRecordData("");
    resultExecutionRecordDTO.setException(errorMessage);
    return resultExecutionRecordDTO;
  }
}
