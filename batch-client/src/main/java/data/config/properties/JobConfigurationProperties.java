package data.config.properties;

import data.config.properties.plugin.EnrichmentConfigurationProperties;
import data.config.properties.plugin.IndexingConfigurationProperties;
import data.config.properties.plugin.MediaConfigurationProperties;
import data.config.properties.plugin.NormalizationConfigurationProperties;
import data.config.properties.plugin.OaiHarvestConfigurationProperties;
import data.config.properties.plugin.TransformationConfigurationProperties;
import data.config.properties.plugin.ValidationConfigurationProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Setter
@Getter
public class JobConfigurationProperties {

  @NestedConfigurationProperty
  private OaiHarvestConfigurationProperties oaiHarvest;
  @NestedConfigurationProperty
  private ValidationConfigurationProperties validation;
  @NestedConfigurationProperty
  private TransformationConfigurationProperties transformation;
  @NestedConfigurationProperty
  private NormalizationConfigurationProperties normalization;
  @NestedConfigurationProperty
  private EnrichmentConfigurationProperties enrichment;
  @NestedConfigurationProperty
  private MediaConfigurationProperties media;
  @NestedConfigurationProperty
  private IndexingConfigurationProperties indexing;

}
