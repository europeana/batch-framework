package data.utility;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import data.entity.ExecutionRecordExceptionLog;
import data.entity.ExecutionRecordExceptionLogKey;
import data.entity.ExecutionRecordKey;

public class ExecutionRecordUtil {

  private ExecutionRecordUtil() {
  }

  public static ExecutionRecordDTO converter(ExecutionRecord executionRecord){
    final ExecutionRecordDTO executionRecordDTO = new ExecutionRecordDTO();
    executionRecordDTO.setDatasetId(executionRecord.getExecutionRecordKey().getDatasetId());
    executionRecordDTO.setExecutionId(executionRecord.getExecutionRecordKey().getExecutionId());
    executionRecordDTO.setRecordId(executionRecord.getExecutionRecordKey().getRecordId());
    executionRecordDTO.setExecutionName(executionRecord.getExecutionName());
    executionRecordDTO.setRecordData(executionRecord.getRecordData());
    return executionRecordDTO;
  }

  public static ExecutionRecord converter(ExecutionRecordDTO executionRecordDTO){
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

  public static ExecutionRecordExceptionLog converterExceptionLog(ExecutionRecordDTO executionRecordDTO){
    final ExecutionRecordExceptionLog executionRecordExceptionLog = new ExecutionRecordExceptionLog();
    final ExecutionRecordExceptionLogKey executionRecordExceptionLogKey = new ExecutionRecordExceptionLogKey();
    executionRecordExceptionLogKey.setDatasetId(executionRecordDTO.getDatasetId());
    executionRecordExceptionLogKey.setExecutionId(executionRecordDTO.getExecutionId());
    executionRecordExceptionLogKey.setRecordId(executionRecordDTO.getRecordId());
    executionRecordExceptionLog.setExecutionRecordKey(executionRecordExceptionLogKey);
    executionRecordExceptionLog.setExecutionName(executionRecordDTO.getExecutionName());
    executionRecordExceptionLog.setException(executionRecordExceptionLog.getException());
    return executionRecordExceptionLog;
  }

  public static ExecutionRecordDTO createSuccess(ExecutionRecordDTO executionRecordDTO, String updatedRecordString,
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

  public static ExecutionRecordDTO createFailure(ExecutionRecordDTO executionRecordDTO, String errorMessage,
      BatchJobType batchJobType, String executionId){
    final ExecutionRecordDTO resultExecutionRecordDTO = new ExecutionRecordDTO();
    executionRecordDTO.setDatasetId(executionRecordDTO.getDatasetId());
    executionRecordDTO.setExecutionId(executionRecordDTO.getExecutionId());
    executionRecordDTO.setRecordId(executionId);
    executionRecordDTO.setExecutionName(batchJobType.name());
    executionRecordDTO.setRecordData("");
    executionRecordDTO.setException(errorMessage);
    return resultExecutionRecordDTO;
  }

  public static ExecutionRecord prepareResultExecutionRecord(
      ExecutionRecord executionRecord, String updatedData, String executionName, String executionId) {
    return prepareResultExecutionRecord(
        executionRecord.getExecutionRecordKey().getDatasetId(),
        executionId,
        executionRecord.getExecutionRecordKey().getRecordId(),
        executionName,
        updatedData
    );
  }

  public static ExecutionRecord prepareResultExecutionRecord(
      String datasetId, String executionId, String recordId, String executionName, String updatedData) {
    final ExecutionRecordKey resultExecutionRecordKey = new ExecutionRecordKey();
    resultExecutionRecordKey.setDatasetId(datasetId);
    resultExecutionRecordKey.setExecutionId(executionId);
    resultExecutionRecordKey.setRecordId(recordId);

    final ExecutionRecord resultExecutionRecord = new ExecutionRecord();
    resultExecutionRecord.setExecutionRecordKey(resultExecutionRecordKey);
    resultExecutionRecord.setExecutionName(executionName);
    resultExecutionRecord.setRecordData(updatedData);
    return resultExecutionRecord;
  }

  public static ExecutionRecordExceptionLog prepareResultExecutionRecordExceptionLog(
      ExecutionRecord executionRecord, String exception, String executionName, String executionId) {
    return prepareResultExecutionRecordExceptionLog(
        executionRecord.getExecutionRecordKey().getDatasetId(),
        executionId,
        executionRecord.getExecutionRecordKey().getRecordId(),
        executionName,
        exception
    );
  }

  public static ExecutionRecordExceptionLog prepareResultExecutionRecordExceptionLog(
      String datasetId, String executionId, String recordId, String executionName, String exception) {
    final ExecutionRecordExceptionLogKey resultExecutionRecordKey = new ExecutionRecordExceptionLogKey();
    resultExecutionRecordKey.setDatasetId(datasetId);
    resultExecutionRecordKey.setExecutionId(executionId);
    resultExecutionRecordKey.setRecordId(recordId);

    final ExecutionRecordExceptionLog resultExecutionRecord = new ExecutionRecordExceptionLog();
    resultExecutionRecord.setExecutionRecordKey(resultExecutionRecordKey);
    resultExecutionRecord.setExecutionName(executionName);
    resultExecutionRecord.setException(exception);
    return resultExecutionRecord;
  }

}
