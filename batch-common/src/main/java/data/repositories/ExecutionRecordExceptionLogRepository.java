package data.repositories;

import data.entity.ExecutionRecordExceptionLog;
import data.entity.ExecutionRecordIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionRecordExceptionLogRepository<T extends ExecutionRecordIdentifier> extends JpaRepository<ExecutionRecordExceptionLog, T> {
  long countByDatasetIdAndExecutionId(String datasetId, String executionId);
}
