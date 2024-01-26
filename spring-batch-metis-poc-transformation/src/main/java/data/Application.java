package data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Application {

  public static void main(String[] args) {
    ConfigurableApplicationContext run = new SpringApplicationBuilder(Application.class)
        .web(WebApplicationType.NONE)
        .run(args);
    System.exit(SpringApplication.exit(run));
  }

}
