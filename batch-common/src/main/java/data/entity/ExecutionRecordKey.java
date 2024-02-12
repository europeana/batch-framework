package data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class ExecutionRecordKey implements Serializable {
  @Column(length = 20)
  private String datasetId;
  @Column(length = 50)
  private String executionId;
  @Column(length = 300)
  private String recordId;
}
