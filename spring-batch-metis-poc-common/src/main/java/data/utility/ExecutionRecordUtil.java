package data.utility;

import data.entity.ExecutionRecord;
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

}
