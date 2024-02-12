package data.config.properties.plugin;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OaiHarvestConfigurationProperties {

  private String chunkSize;
  private String parallelizationSize;
}
