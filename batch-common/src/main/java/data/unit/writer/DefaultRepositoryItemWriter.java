package data.unit.writer;

import data.entity.ExecutionRecord;
import data.repositories.ExecutionRecordRepository;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultRepositoryItemWriter extends RepositoryItemWriter<ExecutionRecord> {

  @Autowired
  public DefaultRepositoryItemWriter(ExecutionRecordRepository<ExecutionRecord> executionRecordRepository) {
    super();
    setRepository(executionRecordRepository);
    setMethodName("save");
  }
}
