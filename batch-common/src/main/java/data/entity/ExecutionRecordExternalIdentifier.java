package data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(schema = "batch-framework")
public class ExecutionRecordExternalIdentifier extends ExecutionRecordIdentifier{

    private boolean isDeleted;
}
