package data.unit.processor;

import data.entity.ExecutionRecord;
import data.entity.ExecutionRecordDTO;
import data.unit.processor.listener.MetisItemProcessor;
import data.utility.BatchJobType;
import data.utility.MethodUtil;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import java.io.IOException;
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

@Component
@StepScope
@Setter
public class TransformerItemProcessor implements MetisItemProcessor<ExecutionRecord, ExecutionRecordDTO, String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

  private static final BatchJobType batchJobType = BatchJobType.TRANSFORMATION;
  private MethodUtil<String> methodUtil = new MethodUtil<>();
  private final Function<ExecutionRecord, String> function = getFunction();

  @Override
  public Function<ExecutionRecord, String> getFunction() {
    return executionRecord -> {
      final XsltTransformer xsltTransformer;
      try {
        xsltTransformer = prepareXsltTransformer();
      } catch (TransformationException e) {
        throw new RuntimeException(e);
      }

      final byte[] contentBytes = executionRecord.getRecordData().getBytes(StandardCharsets.UTF_8);
      final String resultString;
      try (StringWriter writer = xsltTransformer.transform(contentBytes, prepareEuropeanaGeneratedIdsMap(contentBytes))) {
        resultString = writer.toString();
      } catch (EuropeanaIdException | TransformationException | IOException e) {
        throw new RuntimeException(e);
      }
      return resultString;
    };
  }

  @Override
  public ExecutionRecordDTO process(@NonNull ExecutionRecord executionRecord) throws Exception {
    return methodUtil.executeCapturing(executionRecord, function, Function.identity(), batchJobType, jobInstanceId.toString());
//    final Function<ExecutionRecord, String> function = executionRecord1 -> {
//      final XsltTransformer xsltTransformer;
//      try {
//        xsltTransformer = prepareXsltTransformer();
//      } catch (TransformationException e) {
//        throw new RuntimeException(e);
//      }
//
//      final byte[] contentBytes = executionRecord1.getRecordData().getBytes(StandardCharsets.UTF_8);
//      final String resultString;
//      try (StringWriter writer = xsltTransformer.transform(contentBytes, prepareEuropeanaGeneratedIdsMap(contentBytes))) {
//        resultString = writer.toString();
//      } catch (EuropeanaIdException | TransformationException | IOException e) {
//        throw new RuntimeException(e);
//      }
//      return resultString;
//    };


//    final ExecutionRecordDTO executionRecordDTO = new ExecutionRecordDTO();
//    int number = new Random().nextInt(10 - 1 + 1) + 1;
//    if (number % 10 == 0) {
//      LOGGER.info("NUMBER {}: ", number);
//      final TransformationException transformationException = new TransformationException(
//          new RuntimeException("On random%10 == 0 exception"));
//      final ExecutionRecordExceptionLog executionRecordExceptionLog = ExecutionRecordUtil.prepareResultExecutionRecordExceptionLog(
//          executionRecord, transformationException.getMessage(), BatchJobType.TRANSFORMATION.name(), jobInstanceId.toString());
//      executionRecordDTO.setExecutionRecordExceptionLog(executionRecordExceptionLog);
//      return executionRecordDTO;
//    }
//
//    final XsltTransformer xsltTransformer;
//    try {
//      xsltTransformer = prepareXsltTransformer();
//    } catch (TransformationException e) {
//      throw new RuntimeException(e);
//    }
//
//    final byte[] contentBytes = executionRecord.getRecordData().getBytes(StandardCharsets.UTF_8);
//    final String resultString;
//    try (StringWriter writer = xsltTransformer.transform(contentBytes, prepareEuropeanaGeneratedIdsMap(contentBytes))) {
//      resultString = writer.toString();
//    } catch (EuropeanaIdException | TransformationException | IOException e) {
//      throw new RuntimeException(e);
//    }
//    final ExecutionRecord resultExecutionRecord = ExecutionRecordUtil.prepareResultExecutionRecord(executionRecord, resultString,
//        BatchJobType.TRANSFORMATION.name(), jobInstanceId.toString());
//    executionRecordDTO.setExecutionRecord(resultExecutionRecord);
//    return executionRecordDTO;
  }

  private XsltTransformer prepareXsltTransformer()
      throws TransformationException {
    return new XsltTransformer(xsltUrl, datasetName, datasetCountry, datasetLanguage);
  }

  private EuropeanaGeneratedIdsMap prepareEuropeanaGeneratedIdsMap(byte[] content)
      throws EuropeanaIdException {
    //Prepare europeana identifiers
    EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = null;
    if (!StringUtils.isBlank(datasetId)) {
      String fileDataString = new String(content, StandardCharsets.UTF_8);
      EuropeanaIdCreator europeanIdCreator = new EuropeanaIdCreator();
      europeanaGeneratedIdsMap = europeanIdCreator.constructEuropeanaId(fileDataString, datasetId);
    }
    return europeanaGeneratedIdsMap;
  }

}
