package data.repositories;

import data.entity.ExecutionRecordExceptionLog;
import data.entity.ExecutionRecordExceptionLogKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionRecordExceptionLogRepository extends JpaRepository<ExecutionRecordExceptionLog, ExecutionRecordExceptionLogKey> {
//  Page<ExecutionRecordExceptionLog> findByExecutionRecordExceptionLogKeyDatasetIdAndExecutionRecordKeyExecutionId(String datasetId, String executionId, Pageable pageable);
//  long countByExecutionRecordExceptionLogKeyDatasetIdAndExecutionRecordKeyExecutionId(String datasetId, String executionId);
}
