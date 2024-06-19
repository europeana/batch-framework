package data.config;

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
    DataFlowClientProperties.class, BatchConfigurationProperties.class})
public class MetisDataflowClientConfig {

  @Bean
  public DataFlowOperations dataFlowOperations(DataFlowClientProperties dataFlowClientProperties) {
    return new DataFlowTemplate(URI.create(dataFlowClientProperties.getServerUri()), new ObjectMapper());
  }

  //For the jobOperation methods in RestartJobTestIT the below needs to be uncommented and the above commented.

//  @Bean
//  public DataFlowOperations dataFlowOperations(DataFlowClientProperties dataFlowClientProperties, ObjectMapper objectMapper) {
//    DataFlowTemplate dataFlowTemplate = new DataFlowTemplate(URI.create(dataFlowClientProperties.getServerUri()), objectMapper);
//
//    for (HttpMessageConverter<?> converter : dataFlowTemplate.getRestTemplate().getMessageConverters()) {
//      if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
//        //replace object mapper with the custom-made
//        jacksonConverter.setObjectMapper(objectMapper);
//      }
//    }
//    return dataFlowTemplate;
//  }
//
//  @Bean
//  public ObjectMapper objectMapper() {
//    ObjectMapper objectMapper = new ObjectMapper();
//    objectMapper.registerModule(new Jdk8Module());
//    JavaTimeModule javaTimeModule = new JavaTimeModule();
//    javaTimeModule.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer());
//    objectMapper.registerModule(javaTimeModule);
//    objectMapper.registerModules(new Jackson2HalModule(), new Jackson2DataflowModuleUpdated());
//    return objectMapper;
//  }
}
