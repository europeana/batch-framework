package data.config.properties;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegisterConfigurationProperties {

  private String oaiHarvestName;
  private String oaiHarvestUri;
  private String validationName;
  private String validationUri;
  private String transformationName;
  private String transformationUri;
  private String normalizationName;
  private String normalizationUri;
  private String enrichmentName;
  private String enrichmentUri;
  private String mediaName;
  private String mediaUri;
  private String indexingName;
  private String indexingUri;

}
