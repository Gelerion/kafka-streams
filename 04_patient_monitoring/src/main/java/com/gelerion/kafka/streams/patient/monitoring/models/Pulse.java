package com.gelerion.kafka.streams.patient.monitoring.models;

public class Pulse implements Vital {
    private String timestamp;

    public String getTimestamp() {
        return this.timestamp;
    }
}
