package data.config;

import data.DbCleaner;
import data.config.properties.FlinkConfigurationProperties;
import data.config.properties.OaiSourceConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({OaiSourceConfigurationProperties.class, FlinkConfigurationProperties.class})
public class TestsConfig {

  @Bean
  public DbCleaner dbCleaner() {
    return new DbCleaner();
  }
}
