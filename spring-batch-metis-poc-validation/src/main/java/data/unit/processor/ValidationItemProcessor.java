package data.unit.processor;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import data.unit.processor.listener.MetisItemProcessor;
import data.utility.BatchJobType;
import data.utility.ExecutionRecordUtil;
import data.utility.ItemProcessorUtil;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import eu.europeana.validation.model.ValidationResult;
import eu.europeana.validation.service.ValidationExecutionService;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.function.Function;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

@Component
@StepScope
@Setter
public class ValidationItemProcessor implements MetisItemProcessor<ExecutionRecord, ExecutionRecordDTO, String> {

  @Value("#{jobParameters['targetJob']}")
  private BatchJobType targetJob;
  @Value("#{stepExecution.jobExecution.jobInstance.id}")
  private Long jobInstanceId;

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String EDM_SORTER_FILE_URL = "http://ftp.eanadev.org/schema_zips/edm_sorter_20230809.xsl";
  private ValidationExecutionService validationService;
  private final Properties properties = new Properties();
  private final ItemProcessorUtil<String> itemProcessorUtil;
  //TODO: 2024-01-31 - Find a better way to do this. SubType??
  private static final BatchJobType batchJobType = BatchJobType.VALIDATION;

  public ValidationItemProcessor() {
    properties.setProperty("predefinedSchemas", "localhost");

    properties.setProperty("predefinedSchemas.edm-internal.url",
        "http://ftp.eanadev.org/schema_zips/europeana_schemas-20220809.zip");
    properties.setProperty("predefinedSchemas.edm-internal.rootLocation", "EDM-INTERNAL.xsd");
    properties.setProperty("predefinedSchemas.edm-internal.schematronLocation", "schematron/schematron-internal.xsl");

    properties.setProperty("predefinedSchemas.edm-external.url",
        "http://ftp.eanadev.org/schema_zips/europeana_schemas-20220809.zip");
    properties.setProperty("predefinedSchemas.edm-external.rootLocation", "EDM.xsd");
    properties.setProperty("predefinedSchemas.edm-external.schematronLocation", "schematron/schematron.xsl");
    validationService = new ValidationExecutionService(properties);
    itemProcessorUtil = new ItemProcessorUtil<>(getFunction(), Function.identity());
  }

  @Override
  public ThrowingFunction<ExecutionRecordDTO, String> getFunction() {
    return executionRecord -> {
      final String reorderedFileContent;
      reorderedFileContent = reorderFileContent(executionRecord.getRecordData());
      String schema;
      String rootFileLocation;
      String schematronFileLocation;
      switch (targetJob) {
        case BatchJobType.VALIDATION_EXTERNAL -> {
          schema = properties.getProperty("predefinedSchemas.edm-external.url");
          rootFileLocation = properties.getProperty("predefinedSchemas.edm-external.rootLocation");
          schematronFileLocation = properties.getProperty("predefinedSchemas.edm-external.schematronLocation");
        }
        case BatchJobType.VALIDATION_INTERNAL -> {
          schema = properties.getProperty("predefinedSchemas.edm-internal.url");
          rootFileLocation = properties.getProperty("predefinedSchemas.edm-internal.rootLocation");
          schematronFileLocation = properties.getProperty("predefinedSchemas.edm-internal.schematronLocation");
        }
        default -> throw new IllegalStateException("Unexpected value: " + targetJob);
      }

      ValidationResult result =
          validationService.singleValidation(schema, rootFileLocation, schematronFileLocation, reorderedFileContent);
      if (result.isSuccess()) {
        LOGGER.info("Validation Success for datasetId {}, recordId {}", executionRecord.getDatasetId(),
            executionRecord.getRecordId());
      } else {
        LOGGER.info("Validation Failure for datasetId {}, recordId {}", executionRecord.getDatasetId(),
            executionRecord.getRecordId());
      }
      return executionRecord.getRecordData();
    };
  }

  @Override
  public ExecutionRecordDTO process(@NonNull ExecutionRecord executionRecord) {
    LOGGER.info("ValidationItemProcessor thread: {}", Thread.currentThread());
    final ExecutionRecordDTO executionRecordDTO = ExecutionRecordUtil.converterToExecutionRecordDTO(executionRecord);
    return itemProcessorUtil.processCapturingException(executionRecordDTO, targetJob, jobInstanceId.toString());
  }

  private String reorderFileContent(String recordData) throws TransformationException {
    LOGGER.debug("Reordering the file");
    StringWriter writer = prepareXsltTransformer().transform(recordData.getBytes(StandardCharsets.UTF_8), null);
    return writer.toString();
  }

  private XsltTransformer prepareXsltTransformer() throws TransformationException {
    return new XsltTransformer(EDM_SORTER_FILE_URL);
  }
}
