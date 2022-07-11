package com.gelerion.kafka.streams.patient.monitoring;

import com.gelerion.kafka.streams.patient.monitoring.models.BodyTemp;
import com.gelerion.kafka.streams.patient.monitoring.models.Pulse;
import com.gelerion.kafka.streams.patient.monitoring.serialization.json.JsonSerdes;
import com.gelerion.kafka.streams.patient.monitoring.times.extractors.VitalTimestampExtractor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;

public class PatientMonitoringTopology {

    public static Topology build() {
        StreamsBuilder builder = new StreamsBuilder();
        // The following topology steps are numbered.
        // These numbers correlate with the topology design

        // 1.1
        Consumed<String, Pulse> pulseConsumerOptions = Consumed.with(Serdes.String(), JsonSerdes.Pulse())
                //use our custom extractor
                .withTimestampExtractor(new VitalTimestampExtractor());

        KStream<String, Pulse> pulseEvents =
                builder.stream("pulse-events", pulseConsumerOptions);

        // 1.2
        Consumed<String, BodyTemp> bodyTempConsumerOptions =
                Consumed.with(Serdes.String(), JsonSerdes.BodyTemp())
                        .withTimestampExtractor(new VitalTimestampExtractor());

        KStream<String, BodyTemp> tempEvents =
                builder.stream("body-temp-events", bodyTempConsumerOptions);

        return builder.build();
    }

}
