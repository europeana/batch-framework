package data.utility;

import static data.utility.ExecutionRecordUtil.createFailureExecutionRecordDTO;
import static data.utility.ExecutionRecordUtil.createSuccessExecutionRecordDTO;

import data.entity.ExecutionRecordDTO;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.springframework.util.function.ThrowingFunction;

@AllArgsConstructor
public class ItemProcessorUtil<O> {

  private final ThrowingFunction<ExecutionRecordDTO, O> function;
  private final Function<O, String> getRecordString;

  public ExecutionRecordDTO processCapturingException(ExecutionRecordDTO executionRecordDTO, BatchJobType batchJobType,
      String executionId) {
    ExecutionRecordDTO resultExecutionRecordDTO;
    try {
      final O result = function.apply(executionRecordDTO);
      resultExecutionRecordDTO =
          createSuccessExecutionRecordDTO(executionRecordDTO, getRecordString.apply(result), batchJobType, executionId);
    } catch (Exception exception) {
      resultExecutionRecordDTO =
          createFailureExecutionRecordDTO(executionRecordDTO, exception.getMessage(), batchJobType, executionId);
    }
    return resultExecutionRecordDTO;
  }

}
