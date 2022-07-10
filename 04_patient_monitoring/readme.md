## Patient Monitoring Application
We will try to detect the presence of a medical condition called systemic inflammatory response syndrome, or SIRS. 
According to Bridgette Kadri, a physician’s assistant at the Medical University of South Carolina, there are several 
vital signs, including body temperature, blood pressure, and heart rate, that can be used as indicators of SIRS. 
In this tutorial, we will look at two of these measurements: body temperature and heart rate. When both of these 
vitals reach predefined thresholds (heart rate >= 100 beats per minute, body temperature >= 100.4°F), we will send 
a record to alerts topic to notify the appropriate medical personne

## Topology
![Screenshot](images/patient_monitoring_topology.png)
1. Our Kafka cluster contains two topics that capture patient vitals measurements: 
   1. The `pulse-events` topic is populated by a heartbeat sensor. Every time the sensor picks up a patient’s heartbeat, it appends a record to this topic. Records are keyed by patient ID.
   2. The `body-temp-events` topic is populated by a wireless body temperature sensor. Every time the patient’s core body temperature is taken, a record is appended to this topic. These records are also keyed by patient ID.
2. In order to detect elevated heart rates, we need to convert the raw pulse events into a heart rate (measured using beats per minute, or bpm). As we learned in the previous chapter, we must first group the records to satisfy Kafka Streams’ prerequisite for performing aggregations.
3. We will use a windowed aggregation to convert the pulse events into a heart rate. Since our unit of measurement is beats per minute, our window size will be 60 seconds.
4. We will use the suppress operator to only emit the final computation of the bpm window.
5. In order to detect an infection, we will filter all vitals measurements that breach a set of predefined thresholds (heart rate >= 100 beats per minute, body temperature >= 100.4°F).
6. Windowed aggregations change the record key. Therefore, we’ll need to rekey the heart rate records by patient ID to meet the co-partitioning requirements for joining records.
7. We will perform a windowed join to combine the two vitals streams. Since we are performing the join after filtering for elevated bpm and body temperature measures, each joined record will indicate an alerting condition for SIRS.
8. Finally, we will expose the results of our heart rate aggregation via interactive queries. We will also write the output of our joined stream to a topic called alerts.

## Running Locally
Once Docker Compose is installed, you can start the local Kafka cluster using the following command:

```sh
$ docker-compose up
```

## Producing Test Data
Once your application is running, you can produce some test data to see it in action. Since our patient monitoring 
application reads from multiple topics (`pulse-events`, `body-temp-events`), we have saved example records for each 
topic in the `data/` directory. To produce data into each of these topics, open a new tab in your shell and run the following commands.  
```sh
# log into the broker, which is where the kafka console scripts live
$ docker-compose exec kafka bash

# produce test data to pulse-events topic
$ kafka-console-producer \
  --bootstrap-server kafka:9092 \
  --topic pulse-events \
  --property 'parse.key=true' \
  --property 'key.separator=|' < pulse-events.json

# produce test data to body-temp-events topic
$ kafka-console-producer \
  --bootstrap-server kafka:9092 \
  --topic body-temp-events \
  --property 'parse.key=true' \
  --property 'key.separator=|' < body-temp-events.json
```

