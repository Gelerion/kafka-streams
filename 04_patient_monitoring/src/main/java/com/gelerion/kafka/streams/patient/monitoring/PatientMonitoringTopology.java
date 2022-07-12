package com.gelerion.kafka.streams.patient.monitoring;

import com.gelerion.kafka.streams.patient.monitoring.models.BodyTemp;
import com.gelerion.kafka.streams.patient.monitoring.models.Pulse;
import com.gelerion.kafka.streams.patient.monitoring.serialization.json.JsonSerdes;
import com.gelerion.kafka.streams.patient.monitoring.times.extractors.VitalTimestampExtractor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.kstream.Suppressed.BufferConfig;

import java.time.Duration;

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

        // The pulse-events topic receives data whenever a patient’s heartbeat is recorded. However, for our purposes,
        // we’re interested in the patient’s heart rate, which is measured by the number of beats per minute (bpm).
        // We know that the count operator can be used to count the number of heartbeats, but we need some way of
        // only counting the records that fall within each 60-second window. This is where windowed aggregations come
        // into play.

        TimeWindows tumblingWindow = TimeWindows.of(Duration.ofSeconds(60));
        KTable<Windowed<String>, Long> pulseCounts = pulseEvents
                // 2
                // Grouping records is a prerequisite for performing an aggregation
                .groupByKey()
                // 3.1 - windowed aggregation
                // Window the stream using a 60-second tumbling window. This will allow us to turn the raw pulse
                // events into a heart rate
                .windowedBy(tumblingWindow)
                // 3.2
                // Materialize the heart rate for interactive queries
                .count(Materialized.as("pulse-counts"))
                // 4
                .suppress(Suppressed.untilWindowCloses(BufferConfig.unbounded().shutDownWhenFull()));

        //Output
        //[pulse-counts]: [1@1605171720000/1605171780000], 1
        //[pulse-counts]: [1@1605171720000/1605171780000], 2
        //[pulse-counts]: [1@1605171720000/1605171780000], 3

        // Apart from the key transformation logic, the preceding output highlights a peculiar behavior of Kafka Streams.
        // The heart rate count, which we compute in our windowed aggregation, gets updated each time a new heartbeat
        // is recorded. We can see this in Example 5-3, since the first heart rate that is emitted is 1,
        // followed by 2, 3, etc. Therefore, downstream operators will see not just the final results of a window
        // (the beats per minute), but also the intermediate results of the window (the number of heartbeats in
        // this 60-second window so far)

        // Suppression
        /*
        In order to use the suppress operator, we need to decide three things:
         - Which suppression strategy should be used for suppressing intermediate window computations
         - How much memory should be used for buffering the suppressed events (this is set using a Buffer Config)
         - What to do when this memory limit is exceeded (this is controlled using a Buffer Full Strategy)
         */

        pulseCounts
                // for debugging purposes only
                .toStream()
                // One interesting thing to highlight in this code example is that the key of the KTable changed from
                // String to Windowed<String>. This is because the windowedBy operator converts KTables into windowed
                // KTables, which have multidimensional keys that contain not only the original record key, but also
                // the time range of the window
                // [<old_key>@<window_start_ms>/<window_end_ms>]
                .print(Printed.<Windowed<String>, Long>toSysOut().withLabel("pulse-counts"));

        return builder.build();
    }

}
