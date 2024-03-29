## IoT Digital Twin Service
We will use the Processor API to build a digital twin service for an offshore wind farm. 
Digital twins (sometimes called device shadows) are popular in both IoT (Internet of Things) and IIoT (industrial IoT) 
use cases, in which the state of a physical object is mirrored in a digital copy.
  
To give you a quick example of what a digital twin is, consider the following. We have a wind farm with 40 wind turbines. 
Whenever one of the turbines reports its current state (wind speed, temperature, power status, etc.), we save 
that information in a key-value store. Now, if we want to interact with a particular wind turbine, we don’t do so 
directly. IoT devices can and do frequently go offline, so we can achieve higher availability and reduce errors if 
we instead only interact with the digital copy (twin) of a physical device.
  
For example, if we want to set the power state from `ON` to `OFF`, instead of sending that signal to the turbine 
directly, we would set the so-called desired state on the digital copy. The physical turbine would subsequently 
synchronize its state (i.e., disable power to the blades) whenever it comes online, and usually at set intervals, 
thereafter. Therefore, a digital twin record will include both a reported and desired state, and we will create 
and expose digital twin records.

With this in mind, our application needs to ingest a stream of sensor data from a set of wind turbines, perform some 
minor processing on the data, and maintain the latest state of each wind turbine in a persistent key-value state store. 
We will then expose the data via Kafka Streams’ interactive queries feature.

## Topology
![Screenshot](images/digital_twin_topology.png)
1. Our Kafka cluster contains two topics, and therefore we need to learn how to add source processors using the Processor API.
   1. Each wind turbine (edge node) is outfitted with a set of environmental sensors, and this data (e.g., wind speed), along with some metadata about the turbine itself (e.g., power state), is sent to the `reported-state-events` topic periodically.
   2. The `desired-state-events` topic is written to whenever a user or process wants to change the power state of a turbine (i.e., turn it off or on)
2. Since the environmental sensor data is reported in the `reported-state-events topic`, we will add a stream processor that determines whether the reported wind speed for a given turbine exceeds safe operating levels,6 and if it does, we will automatically generate a shutdown signal.
3. The third step is broken into two parts:
   1. First, both types of events (reported and desired) will be combined into a so-called digital twin record. These records will be processed and then written to a persistent key-value store called `digital-twin-store`
   2. The second part of this step involves scheduling a periodic function, called a punctuator, to clean out old digital twin records that haven’t seen an update in more than seven days.
4. Each digital twin record will be written to an output topic called digital-twins for analytical purposes.
5. We will expose the digital twin records via Kafka Streams’ interactive queries feature. Every few seconds, the microcontroller on the wind turbine will attempt to synchronize its own state with the desired state exposed by Kafka Streams

## Running Locally
Once Docker Compose is installed, you can start the local Kafka cluster using the following command:

```sh
$ docker-compose up
```

## Producing Test Data
Once your application is running, you can produce some test data to see it in action. 
Since our digital twin application reads from multiple topics (`reported-state-events`, `desired-state-events`), 
we have saved example records for each topic in the `data/` directory. First, produce some data to 
the `reported-state-events` topic using the following command:

```sh
$ docker-compose exec kafka bash -c "
  kafka-console-producer \
  --bootstrap-server kafka:9092 \
  --topic reported-state-events \
  --property 'parse.key=true' \
  --property 'key.separator=|' < reported-state-events.json"
```
One of the example records included a windspeed of 68mph, which exceeds our threshold for safe operating conditions (our threshold is 65mph):
```json
{
  "timestamp": "2020-11-23T09:02:01.000Z",
  "wind_speed_mph": 68,
  "power": "ON",
  "type": "REPORTED"
}
```
Therefore, we should be able to query the Kafka Streams-backed service to retrieve the device shadow for this device 
(which has an ID of 1, as indicated by the record key), and the desired state should show as `OFF` since our 
Kafka Streams application generated a shutdown signal. (`HighWindsFlatmapProcessor`)
```sh
$ curl localhost:7000/devices/1

# output
{
"desired": {
  "timestamp": "2020-11-23T09:02:01.000Z",
  "windSpeedMph": 68,
  "power": "OFF",
  "type": "DESIRED"
},
"reported": {
  "timestamp": "2020-11-23T09:02:01.000Z",
  "windSpeedMph": 68,
  "power": "ON",
  "type": "REPORTED"
}
```

We can also manually update the state of this device (i.e. by turning it on) by producing the following record 
to the `desired-state-events` topic:
```sh
$ docker-compose exec kafka bash -c "
  kafka-console-producer \
  --bootstrap-server kafka:9092 \
  --topic desired-state-events \
  --property 'parse.key=true' \
  --property 'key.separator=|' < desired-state-events.json"
```
  
## When to use the Processor API
In general, you may want to utilize the Processor API if you need to take advantage of the following:
- Access to record metadata (topic, partition, offset information, record headers, and so on)
- Ability to schedule periodic functions
- More fine-grained control over when records get forwarded to downstream processors
- More granular access to state stores
- Ability to circumvent any limitations you come across in the DSL

### Punctuator
Depending on your use case, you may need to perform some periodic task in your Kafka Streams application. This is one 
area where the Processor API really shines, since it allows you to easily schedule a task using 
the `ProcessorContext#schedule` method.  
When we think about when a periodic function will execute in Kafka Streams, we are reminded of this complexity. 
There are two punctuation types (i.e., timing strategies) that you can select from:
  
* `Stream time` Stream time is the highest timestamp observed for a particular topic-partition. It is initially unknown and can only increase or stay the same. It advances only when new data is seen, so if you use this punctuation type, then your function will not execute unless data arrives on a continuous basis.
* `Wall clock time` The local system time, which is advanced during each iteration of the consumer poll method. The upper bound for how often this gets updated is defined by the `StreamsConfig.POLL_MS_CONFIG` configuration, which is the maximum amount of time (in milliseconds) the underlying poll method will block as it waits for new data. This means periodic functions will continue to execute regardless of whether or not new messages arrive.