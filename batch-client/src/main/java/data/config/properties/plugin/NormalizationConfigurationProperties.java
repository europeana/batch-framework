package data.config.properties.plugin;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NormalizationConfigurationProperties {

  private String chunkSize;
  private String parallelizationSize;
}
