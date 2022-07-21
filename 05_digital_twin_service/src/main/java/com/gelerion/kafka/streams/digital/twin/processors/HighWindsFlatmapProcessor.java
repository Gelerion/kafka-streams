package com.gelerion.kafka.streams.digital.twin.processors;

import com.gelerion.kafka.streams.digital.twin.models.TurbineState;
import com.gelerion.kafka.streams.digital.twin.models.TurbineState.Power;
import com.gelerion.kafka.streams.digital.twin.models.TurbineState.Type;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HighWindsFlatmapProcessor implements Processor<String, TurbineState, String, TurbineState> {
    private static final Logger log = LoggerFactory.getLogger(HighWindsFlatmapProcessor.class);

    private ProcessorContext<String, TurbineState> context;

    // The init method is called when the Processor is first instantiated. If your processor needs to perform any
    // initialization tasks, you can specify the initialization logic in this method.
    @Override
    public void init(ProcessorContext<String, TurbineState> context) {
        this.context = context;
    }

    @Override
    public void process(Record<String, TurbineState> record) {
        TurbineState reported = record.value();

        // Whenever you want to send a record to downstream processors, you can call the forward method
        // on the ProcessorContext instance. This method accepts the record that you would like to forward.
        // In our processor implementation, we always want to forward the reported state records, which is why we
        // call context.forward using an unmodified record in this line.
        context.forward(record);

        if (reported.getWindSpeedMph() > 65 && reported.getPower() == Power.ON) {
            log.info("high winds detected. sending shutdown signal");
            // If the previous conditions are met, generate a new record containing a desired power state of OFF.
            // Since we have already sent the original reported state record downstream, and we are now generating
            // a desired state record, this is effectively a type of flatMap operation (our processor has created two
            // output records from one input record)
            TurbineState desired = TurbineState.clone(reported);
            desired.setPower(Power.OFF);
            desired.setType(Type.DESIRED);

            Record<String, TurbineState> newRecord = new Record<>(record.key(), desired, record.timestamp());
            context.forward(newRecord);

            // if you wanted to forward to a specific downstream processor, e.g. a processor named "some-child-node",
            // you could use the following code instead
            // context.forward(newRecord, "some-child-node");
        }
    }
}
