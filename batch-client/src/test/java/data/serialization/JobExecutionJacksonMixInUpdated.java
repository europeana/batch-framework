package data.serialization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties({"running", "jobId", "stopping", "jobConfigurationName"})
public abstract class JobExecutionJacksonMixInUpdated {
  JobExecutionJacksonMixInUpdated(@JsonProperty("id") Long id) {
  }
}
