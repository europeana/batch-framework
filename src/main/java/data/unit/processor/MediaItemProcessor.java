package data.unit.processor;

import static java.util.Objects.nonNull;

import data.entity.ExecutionRecord;
import data.utility.BatchJobType;
import data.utility.ExecutionRecordUtil;
import eu.europeana.metis.mediaprocessing.MediaExtractor;
import eu.europeana.metis.mediaprocessing.MediaProcessorFactory;
import eu.europeana.metis.mediaprocessing.RdfConverterFactory;
import eu.europeana.metis.mediaprocessing.RdfDeserializer;
import eu.europeana.metis.mediaprocessing.RdfSerializer;
import eu.europeana.metis.mediaprocessing.exception.MediaExtractionException;
import eu.europeana.metis.mediaprocessing.exception.MediaProcessorException;
import eu.europeana.metis.mediaprocessing.exception.RdfDeserializationException;
import eu.europeana.metis.mediaprocessing.exception.RdfSerializationException;
import eu.europeana.metis.mediaprocessing.model.EnrichedRdf;
import eu.europeana.metis.mediaprocessing.model.RdfResourceEntry;
import eu.europeana.metis.mediaprocessing.model.ResourceExtractionResult;
import eu.europeana.metis.mediaprocessing.model.Thumbnail;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Setter
public class MediaItemProcessor implements ItemProcessor<ExecutionRecord, ExecutionRecord> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MediaItemProcessor.class);
  private MediaExtractor mediaExtractor;
  private RdfSerializer rdfSerializer;
  private RdfDeserializer rdfDeserializer;

  @Value("#{stepExecution.jobExecution.jobInstance.id}")
  private Long jobInstanceId;

  @Override
  public ExecutionRecord process(ExecutionRecord executionRecord) throws Exception {
    final byte[] rdfBytes = executionRecord.getRecordData().getBytes(StandardCharsets.UTF_8);
    final EnrichedRdf enrichedRdf = getEnrichedRdf(rdfBytes);

    RdfResourceEntry resourceMainThumbnail = rdfDeserializer.getMainThumbnailResourceForMediaExtraction(rdfBytes);
    boolean hasMainThumbnail = false;
    if (resourceMainThumbnail != null) {
      hasMainThumbnail = processResourceWithoutThumbnail(resourceMainThumbnail, executionRecord.getExecutionRecordKey().getRecordId(), enrichedRdf, mediaExtractor);
    }
    List<RdfResourceEntry> remainingResourcesList = rdfDeserializer.getRemainingResourcesForMediaExtraction(rdfBytes);
    if (hasMainThumbnail) {
      remainingResourcesList.forEach(entry ->
          processResourceWithThumbnail(entry, executionRecord.getExecutionRecordKey().getRecordId(), enrichedRdf, mediaExtractor)
      );
    } else {
      remainingResourcesList.forEach(entry ->
          processResourceWithoutThumbnail(entry, executionRecord.getExecutionRecordKey().getRecordId(), enrichedRdf, mediaExtractor)
      );
    }
    String resultString = new String(getOutputRdf(enrichedRdf), StandardCharsets.UTF_8);
    return ExecutionRecordUtil.prepareResultExecutionRecord(executionRecord, resultString, BatchJobType.MEDIA.name(), jobInstanceId.toString());
  }

  private EnrichedRdf getEnrichedRdf(byte[] rdfBytes) throws RdfDeserializationException {
      return rdfDeserializer.getRdfForResourceEnriching(rdfBytes);
  }

  private byte[] getOutputRdf(EnrichedRdf rdfForEnrichment) throws RdfSerializationException {
      return rdfSerializer.serialize(rdfForEnrichment);
  }

  private boolean processResourceWithThumbnail(RdfResourceEntry resourceToProcess, String recordId,
      EnrichedRdf rdfForEnrichment, MediaExtractor extractor) {
    return processResource(resourceToProcess, recordId, rdfForEnrichment, extractor, true);
  }

  private boolean processResourceWithoutThumbnail(RdfResourceEntry resourceToProcess, String recordId,
      EnrichedRdf rdfForEnrichment, MediaExtractor extractor) {
    return processResource(resourceToProcess, recordId, rdfForEnrichment, extractor, false);
  }

  private boolean processResource(RdfResourceEntry resourceToProcess, String recordId,
      EnrichedRdf rdfForEnrichment, MediaExtractor extractor, boolean gotMainThumbnail) {
    ResourceExtractionResult extraction;
    boolean successful = false;

    try {
      // Perform media extraction
      extraction = extractor.performMediaExtraction(resourceToProcess, gotMainThumbnail);

      // Check if extraction for media was successful
      successful = extraction != null;

      // If successful then store data
      if (successful) {
        rdfForEnrichment.enrichResource(extraction.getMetadata());
        if (!CollectionUtils.isEmpty(extraction.getThumbnails())) {
          storeThumbnails(recordId, extraction.getThumbnails());
        }
      }

    } catch (MediaExtractionException e) {
      LOGGER.warn("Error while extracting media for record {}. ", recordId, e);
    }

    return successful;
  }

  private void storeThumbnails(String recordId, List<Thumbnail> thumbnails) {
    if (nonNull(thumbnails)) {
      LOGGER.info("Fake storing thumbnail");
    }
  }

  @PostConstruct
  private void postConstruct() throws MediaProcessorException {
    final RdfConverterFactory rdfConverterFactory = new RdfConverterFactory();
    rdfDeserializer = rdfConverterFactory.createRdfDeserializer();
    rdfSerializer = rdfConverterFactory.createRdfSerializer();
    final MediaProcessorFactory mediaProcessorFactory = new MediaProcessorFactory();
    mediaExtractor = mediaProcessorFactory.createMediaExtractor();
  }

}
