package com.gelerion.kafka.streams.digital.twin;

import com.gelerion.kafka.streams.digital.twin.processors.HighWindsFlatmapProcessor;
import com.gelerion.kafka.streams.digital.twin.serialization.json.JsonSerdes;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Topology;

public class DigitalTwinTopology {

    @SuppressWarnings("resource")
    public static Topology build() {
        Topology topology = new Topology();

        // There are many overloaded versions of this method, including variations that support offset reset strategies,
        // topic patterns, and more
        topology.addSource(
                "Desired State Events",
                Serdes.String().deserializer(),
                JsonSerdes.TurbineState().deserializer(),
                "desired-state-events"
        );

        topology.addSource(
                "Reported State Events",
                Serdes.String().deserializer(),
                JsonSerdes.TurbineState().deserializer(),
                "reported-state-events"
        );

        // One thing to note about the preceding example is that you will see no mention of a stream or table.
        // These abstractions do not exist in the Processor API. However, conceptually speaking, both source processors
        // we have added in the preceding code represent a stream.

        // Adding stateless stream processors
        // This will create an extra desired state event, with power == OFF (i.e. a shutdown signal) if the
        // wind speed reaches dangerous thresholds for operation
        topology.addProcessor(
                "High Winds Flatmap Processor",
                HighWindsFlatmapProcessor::new,
                "Reported State Events" //parent
        );

        // Creating stateful processors


        return topology;
    }

}
