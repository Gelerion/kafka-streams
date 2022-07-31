package com.gelerion.kafka.streams.digital.twin.processors;

import com.gelerion.kafka.streams.digital.twin.models.DigitalTwin;
import com.gelerion.kafka.streams.digital.twin.models.TurbineState;
import com.gelerion.kafka.streams.digital.twin.models.TurbineState.Type;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;

public class DigitalTwinProcessor implements Processor<String, TurbineState, String, DigitalTwin> {

    private ProcessorContext<String, DigitalTwin> context;
    private KeyValueStore<String, DigitalTwin> kvStore;

    @Override
    public void init(ProcessorContext<String, DigitalTwin> context) {
        this.context = context;
        this.kvStore = context.getStateStore("digital-twin-store");
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
}
