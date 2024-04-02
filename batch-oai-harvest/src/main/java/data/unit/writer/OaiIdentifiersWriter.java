package data.unit.writer;

import data.entity.ExecutionRecordExternalIdentifier;
import data.repositories.ExecutionRecordExternalIdentifierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

@Component
public class OaiIdentifiersWriter implements ItemWriter<ExecutionRecordExternalIdentifier> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ExecutionRecordExternalIdentifierRepository repository;

    public OaiIdentifiersWriter(ExecutionRecordExternalIdentifierRepository repository) {
        this.repository = repository;
    }

    @Override
    public void write(Chunk<? extends ExecutionRecordExternalIdentifier> chunk) throws Exception {
        LOGGER.info("Writing chunk of {} oai identifiers to DB", chunk.size());
        repository.saveAll(chunk);
        LOGGER.info("Chunk of {} oai identifiers written to DB", chunk.size());
    }
}
