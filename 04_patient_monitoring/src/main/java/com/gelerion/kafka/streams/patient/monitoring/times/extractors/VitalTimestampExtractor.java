package com.gelerion.kafka.streams.patient.monitoring.times.extractors;

import com.gelerion.kafka.streams.patient.monitoring.models.Vital;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

import java.time.Instant;

/**
 * In Kafka Streams, timestamp extractors are responsible for associating a given record with a timestamp, and these
 * timestamps are used in time-dependent operations like windowed joins and windowed aggregations.
 *
 * One thing you should consider when using timestamp extractors is how records without valid timestamps should be handled.
 * The three most common options are to:
 * - Throw an exception and stop processing (giving the developers an opportunity to resolve the bug)
 * - Fallback to the partition time
 * - Return a negative timestamp, which will allow Kafka Streams to skip over the record and continue processing
 */
public class VitalTimestampExtractor implements TimestampExtractor {

    // Kafka Streams keeps track of the most recent timestamp it has seen for each partition it consumes from, and
    // passes this timestamp to the extract method using the partitionTime parameter.
    @Override
    public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
        Vital measurement = (Vital) record.value();
        if (measurement != null && measurement.getTimestamp() != null) {
            String timestamp = measurement.getTimestamp();
            return Instant.parse(timestamp).toEpochMilli();
        }

        // If we cannot extract a timestamp for some reason, we can fall back to the partition time in order
        // to approximate when the event occurred.
        return partitionTime;
    }
}
