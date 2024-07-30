package data;

import static data.DbCleaner.JUNIT_DATASET;
import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariConfig;
import data.config.TestsConfig;
import data.config.MetisDataflowClientConfig;
import data.config.properties.BatchConfigurationProperties;
import data.config.properties.OaiSourceConfigurationProperties;
import data.entity.ExecutionRecord;
import data.repositories.ExecutionRecordExceptionLogRepository;
import data.repositories.ExecutionRecordRepository;
import jakarta.annotation.Resource;
import java.lang.invoke.MethodHandles;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {MetisDataflowClientConfig.class, TestsConfig.class})
@EnableAutoConfiguration
@TestMethodOrder(MethodOrderer.MethodName.class)
public abstract class AbstractPerformanceTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  protected HikariConfig dbConfig;


  @Resource
  protected ExecutionRecordRepository<ExecutionRecord> executionRecordRepository;

  @Resource
  protected ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository;

  private static boolean firstTest = true;

  private static boolean cleared;

  protected StopWatch startWatch;

  @Autowired
  private DbCleaner dbCleaner;

  @Autowired
  protected BatchConfigurationProperties batchConfigurationProperties;

  @Autowired
  protected OaiSourceConfigurationProperties sourceProperties;


  protected void enforceDbClear(int stepNumber) {
    if (firstTest) {
      firstTest = false;
      dbCleaner.clearDbFor(stepNumber);
      cleared = true;
    } else {
      if (cleared) {
        LOGGER.info("There is no need to clear DB. It was cleared before first test.");
      } else {
        throw new RuntimeException("The DB could not be cleared before first test.");
      }
    }
  }


  protected void validateResult(int stepNumber) {
    LOGGER.info("Step: {} - task execution time: {}", stepNumber, startWatch.formatTime());
    String datasetId = JUNIT_DATASET;
    String taskId = String.valueOf(stepNumber);
    int expectedRecordCount = stepNumber != 1 ? sourceProperties.getValidRecordCount() : sourceProperties.getRecordCount() ;
    long expectedErrorCount = stepNumber != 2 ? 0: sourceProperties.getRecordCount() - sourceProperties.getValidRecordCount();
    assertThat(executionRecordRepository.countByDatasetIdAndExecutionId(datasetId, taskId)).isEqualTo(expectedRecordCount);
    assertThat(executionRecordExceptionLogRepository.countByDatasetIdAndExecutionId(datasetId, taskId)).isEqualTo(expectedErrorCount);
  }

}
