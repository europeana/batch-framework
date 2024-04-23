package data;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class IndexingApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext run = new SpringApplicationBuilder(IndexingApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
        run.close();
    }

}
