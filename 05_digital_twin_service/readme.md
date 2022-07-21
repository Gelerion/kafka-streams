## IoT Digital Twin Service
We will use the Processor API to build a digital twin service for an offshore wind farm. 
Digital twins (sometimes called device shadows) are popular in both IoT (Internet of Things) and IIoT (industrial IoT) 
use cases, in which the state of a physical object is mirrored in a digital copy. This is a great use case for 
Kafka Streams, which can easily ingest and process high-volume sensor data, capture the state of a physical object 
using state stores, and subsequently expose this state using interactive queries.