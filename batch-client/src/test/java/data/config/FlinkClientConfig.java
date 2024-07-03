package data.config;

import data.DbCleaner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlinkClientConfig {

  @Bean
  public DbCleaner dbCleaner() {
    return new DbCleaner();
  }
}
