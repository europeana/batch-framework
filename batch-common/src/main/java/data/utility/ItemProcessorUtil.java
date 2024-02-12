package data.utility;

import static data.utility.ExecutionRecordUtil.createFailureExecutionRecordDTO;
import static data.utility.ExecutionRecordUtil.createSuccessExecutionRecordDTO;

import data.entity.ExecutionRecordDTO;
import data.job.BatchJobSubType;
import data.job.BatchJobType;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.function.ThrowingFunction;

@AllArgsConstructor
public class ItemProcessorUtil<O> {

  private final ThrowingFunction<ExecutionRecordDTO, O> function;
  private final Function<O, String> getRecordString;

  public ExecutionRecordDTO processCapturingException(ExecutionRecordDTO executionRecordDTO, BatchJobType batchJobType,
      BatchJobSubType batchJobSubType, String executionId) {
    final String executionName = batchJobType.name() + "-" + batchJobSubType.getName();
    return getExecutionRecordDTO(executionRecordDTO, executionId, executionName);
  }

  public ExecutionRecordDTO processCapturingException(ExecutionRecordDTO executionRecordDTO, BatchJobType batchJobType, String executionId) {
    final String executionName = batchJobType.name();
    return getExecutionRecordDTO(executionRecordDTO, executionId, executionName);
  }

  @NotNull
  public ExecutionRecordDTO getExecutionRecordDTO(ExecutionRecordDTO executionRecordDTO, String executionId,
      String executionName) {
    ExecutionRecordDTO resultExecutionRecordDTO;
    try {
      final O result = function.apply(executionRecordDTO);
      resultExecutionRecordDTO =
          createSuccessExecutionRecordDTO(executionRecordDTO, getRecordString.apply(result), executionName, executionId);
    } catch (Exception exception) {
      resultExecutionRecordDTO =
          createFailureExecutionRecordDTO(executionRecordDTO, exception.getMessage(), executionName, executionId);
    }
    return resultExecutionRecordDTO;
  }

}
