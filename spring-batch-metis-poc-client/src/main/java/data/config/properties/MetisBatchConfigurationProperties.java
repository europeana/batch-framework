package data.config.properties;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Setter
@Getter
@ConfigurationProperties(prefix = "metis-batch")
public class MetisBatchConfigurationProperties {

  @NestedConfigurationProperty
  private RegisterConfigurationProperties registerProperties;
  @NestedConfigurationProperty
  private JobConfigurationProperties jobProperties;
  private Map<String, String> deploymentProperties;
}
