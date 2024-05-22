package data.repositories;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordIdentifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionRecordRepository<T extends ExecutionRecordIdentifier> extends JpaRepository<ExecutionRecord, T> {
  Page<ExecutionRecord> findByDatasetIdAndExecutionId(String datasetId, String executionId, Pageable pageable);
  long countByDatasetIdAndExecutionId(String datasetId, String executionId);
}
