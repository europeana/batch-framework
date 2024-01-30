package data.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class ExecutionRecordExceptionLogKey implements Serializable {
  private String datasetId;
  private String executionId;
  private String recordId;
}
