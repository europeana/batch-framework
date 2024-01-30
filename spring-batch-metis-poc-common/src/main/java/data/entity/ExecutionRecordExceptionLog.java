package data.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ExecutionRecordExceptionLog {
  @EmbeddedId
  private ExecutionRecordExceptionLogKey executionRecordKey;
  private String executionName;
  private String exception;
}
