package data.repositories;

import data.entity.ExecutionRecordExternalIdentifier;
import data.entity.ExecutionRecordKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionRecordExternalIdentifierRepository extends JpaRepository<ExecutionRecordExternalIdentifier, ExecutionRecordKey> {

    Page<ExecutionRecordExternalIdentifier> findAllByExecutionRecordKey_ExecutionId(String executionId, PageRequest pageable);
    long countByExecutionRecordKeyDatasetIdAndExecutionRecordKeyExecutionId(String datasetId, String executionId);

}
