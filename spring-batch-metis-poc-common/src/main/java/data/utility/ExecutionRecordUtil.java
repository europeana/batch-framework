package data.utility;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordExceptionLog;
import data.entity.ExecutionRecordExceptionLogKey;
import data.entity.ExecutionRecordKey;

public class ExecutionRecordUtil {

  private ExecutionRecordUtil() {
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
