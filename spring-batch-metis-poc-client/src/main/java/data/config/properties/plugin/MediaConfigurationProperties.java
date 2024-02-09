package data.config.properties.plugin;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MediaConfigurationProperties {

  private String chunkSize;
  private String parallelizationSize;
}
