package data.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "test")
public class TestConfigurationProperties {

  String datasetId;
  DbCleaningMode dbCleaning;
}
