package data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class ExecutionRecordExceptionLogKey implements Serializable {
  @Column(length = 20)
  private String datasetId;
  @Column(length = 50)
  private String executionId;
  @Column(length = 300)
  private String recordId;
}
