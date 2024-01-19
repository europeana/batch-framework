package data.repositories;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionRecordRepository extends JpaRepository<ExecutionRecord, ExecutionRecordKey> {
  Page<ExecutionRecord> findByExecutionRecordKeyDatasetIdAndExecutionRecordKeyExecutionId(String datasetId, String executionId, Pageable pageable);
}
