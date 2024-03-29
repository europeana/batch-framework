package data.config.properties.plugin;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EnrichmentConfigurationProperties {

  private String chunkSize;
  private String parallelizationSize;

  private String dereferenceUrl;
  private String entityManagementUrl;
  private String entityApiUrl;
  private String entityApiKey;
}
