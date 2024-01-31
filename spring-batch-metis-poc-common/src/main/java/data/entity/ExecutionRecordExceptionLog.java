package data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ExecutionRecordExceptionLog {
  @EmbeddedId
  private ExecutionRecordKey executionRecordKey;
  private String executionName;
  @Column(name = "exception", length = 100000)
  private String exception;
}
