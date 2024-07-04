package data.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "source")
public class OaiSourceConfigurationProperties {
  String url;
  String setSpec;
  String metadataPrefix;
  int recordCount;

}
