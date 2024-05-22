package data.unit.reader;

import data.entity.ExecutionRecord;
import data.repositories.ExecutionRecordRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort.Direction;

@Setter
public class DefaultRepositoryItemReader extends RepositoryItemReader<ExecutionRecord> {

  @Value("#{jobParameters['datasetId']}")
  private String datasetId;
  @Value("#{jobParameters['executionId']}")
  private String executionId;

  private ExecutionRecordRepository<ExecutionRecord> executionRecordRepository;

  public DefaultRepositoryItemReader(ExecutionRecordRepository<ExecutionRecord> executionRecordRepository) {
    super();
    this.executionRecordRepository = executionRecordRepository;
  }

  @Override
  public void afterPropertiesSet() throws Exception {

    setRepository(executionRecordRepository);
    setMethodName("findByDatasetIdAndExecutionId");

    List<Object> queryMethodArguments = new ArrayList<>();
    queryMethodArguments.add(datasetId);
    queryMethodArguments.add(executionId);

    setArguments(queryMethodArguments);
    setPageSize(1);
    Map<String, Direction> sorts = new HashMap<>();
    sorts.put("RecordId", Direction.ASC);
    setSort(sorts);
  }
}
