package data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ExecutionRecord {
  @EmbeddedId
  private ExecutionRecordKey executionRecordKey;
  @Column(length = 50)
  private String executionName;
  @Column(columnDefinition = "TEXT")
  private String recordData;
}
