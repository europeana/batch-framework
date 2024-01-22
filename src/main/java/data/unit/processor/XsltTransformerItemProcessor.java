package data.unit.processor;

import data.entity.ExecutionRecord;
import data.utility.ExecutionRecordUtil;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Setter
public class XsltTransformerItemProcessor implements ItemProcessor<ExecutionRecord, ExecutionRecord> {

  private static final Logger LOGGER = LoggerFactory.getLogger(XsltTransformerItemProcessor.class);

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

  @Override
  public ExecutionRecord process(@NonNull ExecutionRecord executionRecord) throws InterruptedException {
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
    return ExecutionRecordUtil.prepareResultExecutionRecord(executionRecord, resultString, "TRANSFORMATION", jobInstanceId.toString());
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
