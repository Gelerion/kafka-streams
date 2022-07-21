package com.gelerion.kafka.streams.digital.twin.serialization.json;

import com.gelerion.kafka.streams.digital.twin.models.DigitalTwin;
import com.gelerion.kafka.streams.digital.twin.models.TurbineState;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;

public class JsonSerdes {

    public static Serde<DigitalTwin> DigitalTwin() {
        JsonSerializer<DigitalTwin> serializer = new JsonSerializer<>();
        JsonDeserializer<DigitalTwin> deserializer = new JsonDeserializer<>(DigitalTwin.class);
        return Serdes.serdeFrom(serializer, deserializer);
    }

    public static Serde<TurbineState> TurbineState() {
        JsonSerializer<TurbineState> serializer = new JsonSerializer<>();
        JsonDeserializer<TurbineState> deserializer = new JsonDeserializer<>(TurbineState.class);
        return Serdes.serdeFrom(serializer, deserializer);
    }

}