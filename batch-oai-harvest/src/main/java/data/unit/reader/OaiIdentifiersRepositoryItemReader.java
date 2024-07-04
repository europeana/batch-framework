package data.unit.reader;

import data.entity.ExecutionRecordExternalIdentifier;
import data.repositories.ExecutionRecordExternalIdentifierRepository;
import java.util.HashMap;
import java.util.Map;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@StepScope
public class OaiIdentifiersRepositoryItemReader extends RepositoryItemReader<ExecutionRecordExternalIdentifier> {

    @Value("#{jobParameters['overrideJobId'] ?: stepExecution.jobExecution.jobInstance.id}")
    private Long jobInstanceId;

    private final ExecutionRecordExternalIdentifierRepository<ExecutionRecordExternalIdentifier> executionRecordExternalIdentifierRepository;

    public OaiIdentifiersRepositoryItemReader(ExecutionRecordExternalIdentifierRepository<ExecutionRecordExternalIdentifier> executionRecordExternalIdentifierRepository) {
        this.executionRecordExternalIdentifierRepository = executionRecordExternalIdentifierRepository;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        setRepository(executionRecordExternalIdentifierRepository);
        setSort(Collections.emptyMap());
        setMethodName("findAllByExecutionId");
        setArguments(List.of(jobInstanceId+""));
        Map<String, Direction> sorts = new HashMap<>();
        sorts.put("RecordId", Direction.ASC);
        setSort(sorts);

        super.afterPropertiesSet();
    }
}
