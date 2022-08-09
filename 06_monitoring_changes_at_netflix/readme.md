## Monitoring changes at Netflix
The goal of this application is simple. We need to consume a stream of production changes, filter and transform 
the data for processing, enrich and aggregate the data for reporting purposes, and ultimately make the processed 
data available to downstream systems. It sounds like a lot of work, but with `ksqlDB`, the implementation will 
be very straightforward.

The type of change we’ll be focusing on will be changes to a show’s season length 
(for example, Stranger Things, Season 4 may originally be slated for 12 episodes, but could be reworked into 
an 8-episode season, causing a ripple effect in various systems, including talent scheduling, cash projection, etc.). 
This example was chosen because it not only models a real-world problem, but also touches on the most common tasks 
you will tackle in your own ksqlDB applications.

## Topology
![Screenshot](images/monitoring_chanes_at_netflix_topology.png)
1. Our application will read from two topics:
   1. The `titles` topic is a compacted topic containing metadata (title name, release date, etc.) for films and television shows that are hosted on the Netflix service.
   2. The `production_changes` topic is written to whenever there is a talent scheduling, budgetary, release date, or season length change to a title that is currently in production.
2. After consuming the data from our source topics, we need to perform some basic preprocessing (e.g., filtering and transformation) in order to prepare the `production_changes` data for enrichment. The preprocessed stream, which will contain only changes to a title’s episode count/season length after it has been filtered, will be written to a Kafka topic named `season_length_changes`
3. We will then perform some data enrichment on the preprocessed data. Specifically, we will join the `season_length_changes` stream with the `titles` data to create a combined record from multiple sources and dimensions
4. Next, we will perform some windowed and non-windowed aggregations to count the number of changes in a five-minute period. The resulting table will be materialized, and made available for lookup-style pull queries
5. Finally, we will make the enriched and aggregated data available to two different types of clients. The first client will receive continuous updates via push queries, using a long-lived connection to ksqlDB. The second client will perform point lookups using short-lived pull queries that are more akin to traditional database lookups

## Running Locally
Once Docker Compose is installed, you can start the local Kafka cluster using the following command:

```sh
$ docker-compose up
```
Once the services are running, open another tab and log into the ksqlDB CLI using the following command:
```sh
$ docker-compose exec ksqldb-cli  ksql http://ksqldb-server:8088
```
Now, you can run each of the queries from the CLI:
- files/sql/all.sql
  
If you'd like to run all the queries in the above file, simply execute the following statement from the CLI:
```sh
ksql> RUN SCRIPT '/etc/sql/all.sql';
```