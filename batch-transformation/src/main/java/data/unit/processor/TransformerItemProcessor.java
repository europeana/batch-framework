package data.unit.processor;

import static data.job.BatchJobType.TRANSFORMATION;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import data.unit.processor.listener.MetisItemProcessor;
import data.job.BatchJobType;
import data.utility.ExecutionRecordUtil;
import data.utility.ItemProcessorUtil;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
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
public class TransformerItemProcessor implements MetisItemProcessor<ExecutionRecord, ExecutionRecordDTO, String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final BatchJobType batchJobType = TRANSFORMATION;

  @Value("#{jobParameters['datasetId']}")
  private String datasetId;
  @Value("#{jobParameters['datasetName']}")
  private String datasetName;
  @Value("#{jobParameters['datasetCountry']}")
  private String datasetCountry;
  @Value("#{jobParameters['datasetLanguage']}")
  private String datasetLanguage;
  @Value("#{jobParameters['xsltUrl']}")
  private String xsltUrl;
  @Value("#{stepExecution.jobExecution.jobInstance.id}")
  private Long jobInstanceId;

  private final ItemProcessorUtil<String> itemProcessorUtil;

  public TransformerItemProcessor() {
    itemProcessorUtil = new ItemProcessorUtil<>(getFunction(), Function.identity());
  }

  @Override
  public ThrowingFunction<ExecutionRecordDTO, String> getFunction() {
    return executionRecordDTO -> {
      final XsltTransformer xsltTransformer;
      xsltTransformer = prepareXsltTransformer();
      final byte[] contentBytes = executionRecordDTO.getRecordData().getBytes(StandardCharsets.UTF_8);
      final String resultString;
      try (StringWriter writer = xsltTransformer.transform(contentBytes, prepareEuropeanaGeneratedIdsMap(contentBytes))) {
        resultString = writer.toString();
      }
      return resultString;
    };
  }

  @Override
  public ExecutionRecordDTO process(@NonNull ExecutionRecord executionRecord) {
    final ExecutionRecordDTO executionRecordDTO = ExecutionRecordUtil.converterToExecutionRecordDTO(executionRecord);
    return itemProcessorUtil.processCapturingException(executionRecordDTO, batchJobType, jobInstanceId.toString());
  }

  private XsltTransformer prepareXsltTransformer()
      throws TransformationException {
    return new XsltTransformer(xsltUrl, datasetName, datasetCountry, datasetLanguage);
  }

  private EuropeanaGeneratedIdsMap prepareEuropeanaGeneratedIdsMap(byte[] content)
      throws EuropeanaIdException {
    EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = null;
    if (!StringUtils.isBlank(datasetId)) {
      String fileDataString = new String(content, StandardCharsets.UTF_8);
      EuropeanaIdCreator europeanIdCreator = new EuropeanaIdCreator();
      europeanaGeneratedIdsMap = europeanIdCreator.constructEuropeanaId(fileDataString, datasetId);
    }
    return europeanaGeneratedIdsMap;
  }

}
