package com.gelerion.kafka.streams.digital.twin.processors;

import com.gelerion.kafka.streams.digital.twin.models.DigitalTwin;
import com.gelerion.kafka.streams.digital.twin.models.TurbineState;
import com.gelerion.kafka.streams.digital.twin.models.TurbineState.Type;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Cancellable;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class DigitalTwinProcessor implements Processor<String, TurbineState, String, DigitalTwin> {
    private static final Logger log = LoggerFactory.getLogger(DigitalTwinProcessor.class);

    private ProcessorContext<String, DigitalTwin> context;
    private KeyValueStore<String, DigitalTwin> kvStore;
    private Cancellable punctuator;

    @Override
    public void init(ProcessorContext<String, DigitalTwin> context) {
        this.context = context;
        this.kvStore = context.getStateStore("digital-twin-store");
        this.punctuator = this.context.schedule(
                Duration.ofMinutes(5),
                PunctuationType.WALL_CLOCK_TIME, this::enforceTtl);
    }

    @Override
    public void process(Record<String, TurbineState> record) {
        String key = record.key();
        TurbineState value = record.value();
        DigitalTwin digitalTwin = kvStore.get(key);

        if (digitalTwin == null) {
            digitalTwin = new DigitalTwin();
        }

        if (value.getType() == Type.DESIRED) {
            digitalTwin.setDesired(value);
        } else if (value.getType() == Type.REPORTED) {
            digitalTwin.setReported(value);
        }

        kvStore.put(key, digitalTwin);
        Record<String, DigitalTwin> newRecord =
                new Record<>(record.key(), digitalTwin, record.timestamp());
        context.forward(newRecord);
    }

    @Override
    public void close() {
        // cancel the punctuator
        punctuator.cancel();
    }

    private void enforceTtl(long timestamp) {
        try (KeyValueIterator<String, DigitalTwin> iter = kvStore.all()) {
            while (iter.hasNext()) {
                KeyValue<String, DigitalTwin> entry = iter.next();
                TurbineState lastReportedState = entry.value.getReported();
                if (lastReportedState == null) {
                    continue;
                }

                Instant lastUpdated = Instant.parse(lastReportedState.getTimestamp());
                long daysSinceLastUpdate = Duration.between(lastUpdated, Instant.now()).toDays();
                if (daysSinceLastUpdate >= 7) {
                    kvStore.delete(entry.key);
                }
            }
        }
    }
}
