package data.serialization;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = CustomJobParameterJacksonDeserializer.class)
public abstract class JobParameterJacksonMixInUpdated {

}
