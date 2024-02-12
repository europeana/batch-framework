package data.config;//package data.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import data.config.properties.BatchConfigurationProperties;
import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.config.DataFlowClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    DataFlowClientProperties.class, BatchConfigurationProperties.class,
    BatchConfigurationProperties.class})
public class MetisDataflowClientConfig {

  @Bean
  public DataFlowOperations dataFlowOperations(DataFlowClientProperties dataFlowClientProperties) {
    return new DataFlowTemplate(URI.create(dataFlowClientProperties.getServerUri()), new ObjectMapper());
  }
}
