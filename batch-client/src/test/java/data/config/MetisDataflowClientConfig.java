package data.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import data.config.properties.BatchConfigurationProperties;
import data.serialization.CustomLocalDateTimeDeserializer;
import data.serialization.Jackson2DataflowModuleUpdated;
import java.net.URI;
import java.time.LocalDateTime;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.config.DataFlowClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
@EnableConfigurationProperties({
    DataFlowClientProperties.class, BatchConfigurationProperties.class})
public class MetisDataflowClientConfig {

  @Bean
  public DataFlowOperations dataFlowOperations(DataFlowClientProperties dataFlowClientProperties, ObjectMapper objectMapper) {
    DataFlowTemplate dataFlowTemplate = new DataFlowTemplate(URI.create(dataFlowClientProperties.getServerUri()), objectMapper);

    for (HttpMessageConverter<?> converter : dataFlowTemplate.getRestTemplate().getMessageConverters()) {
      if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
        //replace object mapper with the custom-made
        jacksonConverter.setObjectMapper(objectMapper);
      }
    }
    return dataFlowTemplate;
  }

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new Jdk8Module());
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer());
    objectMapper.registerModule(javaTimeModule);
    objectMapper.registerModules(new Jackson2HalModule(), new Jackson2DataflowModuleUpdated());
    return objectMapper;
  }
}
