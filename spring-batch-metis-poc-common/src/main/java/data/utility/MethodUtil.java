package data.utility;

import data.entity.ExecutionRecordDTO;
import java.util.function.Function;

public class MethodUtil<O> {

  public ExecutionRecordDTO  executeCapturing(
      ExecutionRecordDTO executionRecordDTO, Function<ExecutionRecordDTO, O> function, Function<O, String> getRecordString,
      BatchJobType batchJobType, String executionId) {
    ExecutionRecordDTO resultExecutionRecordDTO;
    try{
      final O result = function.apply(executionRecordDTO);
      resultExecutionRecordDTO = ExecutionRecordUtil.createSuccess(executionRecordDTO, getRecordString.apply(result), batchJobType, executionId);
    }
    catch (Exception exception){
      resultExecutionRecordDTO = ExecutionRecordUtil.createFailure(executionRecordDTO, exception.getMessage(), batchJobType, executionId);
    }
    return resultExecutionRecordDTO;
  }

}
