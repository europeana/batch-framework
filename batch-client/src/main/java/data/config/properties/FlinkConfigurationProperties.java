package data.config.properties;

import java.util.stream.IntStream;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "flink")
public class FlinkConfigurationProperties {
  String jobManagerUrl;
  String jobManagerUser;
  String jobManagerPassword;
  String jarId;
  int readerParallelism;
  int operatorParallelism;
  int sinkParallelism;

  public int getMaxParallelism() {
    return IntStream.of(readerParallelism,operatorParallelism,sinkParallelism).max().getAsInt();
  }
}
