package data.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameter;

public class CustomJobParameterJacksonDeserializer extends JsonDeserializer<JobParameter> {

  private final Logger logger = LoggerFactory.getLogger(CustomJobParameterJacksonDeserializer.class);

  @Override
  public JobParameter deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException, JsonProcessingException {
    ObjectCodec oc = jsonParser.getCodec();
    JsonNode node = oc.readTree(jsonParser);

    final String value = node.get("value").asText();
    final boolean identifying = node.get("identifying").asBoolean();
    final String type = node.get("type").asText();

    final JobParameter jobParameter;

    if (!type.isEmpty() && !type.equalsIgnoreCase("STRING")) {
      if ("DATE".equalsIgnoreCase(type)) {
        jobParameter = new JobParameter(DateTime.parse(value).toDate(), DateTime.class, identifying);
      } else if ("DOUBLE".equalsIgnoreCase(type)) {
        jobParameter = new JobParameter(Double.valueOf(value), Double.class, identifying);
      } else if ("LONG".equalsIgnoreCase(type)) {
        jobParameter = new JobParameter(Long.valueOf(value), Long.class, identifying);
      } else {
        throw new IllegalStateException("Unsupported JobParameter type: " + type);
      }
    } else {

      jobParameter = new JobParameter(value, String.class, identifying);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("jobParameter - value: {} (type: {}, isIdentifying: {})",
          jobParameter.getValue(), jobParameter.getType(), jobParameter.isIdentifying());
    }

    return jobParameter;
  }
}
