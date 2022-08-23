-- Custom types (similar to composite types in PostgreSQL) allow us to specify a group of field names and their
-- associated data types, and then later reference the same collection of fields using a name of our choosing.

-- For example, our application needs to capture changes to a season length, which could have a before and
-- after state that are structurally identical. See - production_change_example.json
CREATE TYPE season_length AS STRUCT<season_id INT, episode_count INT> ;

-- Our titles topic, which contains metadata about films and television shows, will be modeled as a table since we
-- only care about the current metadata associated with each title. We can create our table using the following statement:
CREATE TABLE titles (
  -- Remember that tables have mutable, update-style semantics, so if multiple records are seen with the same primary
  -- key, then only the latest will be stored in the table. The exception to this is if the record key is set but
  -- the value is NULL.
  id INT PRIMARY KEY,
  title VARCHAR
) WITH (
      -- ksqlDB will create the underlying topic for us if it does not already exist
      KAFKA_TOPIC='titles',
      VALUE_FORMAT='AVRO',
      PARTITIONS=4
      );


-- We’ll model the production_changes topic as a stream. This is the ideal collection type for this topic since we don’t
-- need to track the latest state of each change event; we simply need to consume and process each value
CREATE STREAM production_changes (
    -- Streams have immutable, insert-style semantics, so uniquely identifying records is not possible. However,
    -- the KEY identifier can be used to alias the record key column (which corresponds to the Kafka record key)
    rowkey VARCHAR KEY,
    uuid INT,
    title_id INT,
    change_type VARCHAR,
    -- our custom data type
    before season_length,
    after season_length,
    created_at VARCHAR
) WITH (
    KAFKA_TOPIC='production_changes',
    PARTITIONS='4',
    VALUE_FORMAT='JSON',
    -- This tells ksqlDB that the created_at column contains the timestamp that ksqlDB should use for time-based
    -- operations, including windowed aggregations and joins
    TIMESTAMP='created_at',
    TIMESTAMP_FORMAT='yyyy-MM-dd HH:mm:ss'
);

-- Persistent Queries
CREATE STREAM season_length_changes
WITH (
    KAFKA_TOPIC = 'season_length_changes',
    VALUE_FORMAT = 'AVRO',
    PARTITIONS = 4,
    REPLICAS = 1
) AS SELECT
     ROWKEY,
     title_id,
     IFNULL(after->season_id, before->season_id) AS season_id,
     before->episode_count AS old_episode_count,
     after->episode_count AS new_episode_count,
     created_at
FROM production_changes
WHERE change_type = 'season_length'
EMIT CHANGES ;

-- non-windowed stream-table join
CREATE STREAM season_length_changes_enriched
WITH (
    KAFKA_TOPIC = 'season_length_changes_enriched',
    VALUE_FORMAT = 'AVRO',
    PARTITIONS = 4,
    TIMESTAMP='created_at',
    TIMESTAMP_FORMAT='yyyy-MM-dd HH:mm:ss'
) AS
SELECT
    s.title_id,
    t.title,
    s.season_id,
    s.old_episode_count,
    s.new_episode_count,
    s.created_at
FROM season_length_changes s
INNER JOIN titles t ON s.title_id = t.id
EMIT CHANGES;

-- Aggregations
-- create materialized view (could only be computed from aggregate queries)
CREATE TABLE season_length_change_counts
WITH (
    KAFKA_TOPIC = 'season_length_change_counts',
    VALUE_FORMAT = 'AVRO',
    PARTITIONS = 1
) AS
SELECT
    title_id,
    season_id,
    COUNT(*) AS change_count,
    LATEST_BY_OFFSET(new_episode_count) AS episode_count
FROM season_length_changes_enriched
WINDOW TUMBLING (
    SIZE 1 HOUR,
    -- keep state stores small
    RETENTION 2 DAYS,
    -- wait for late events up to 10 minutes
    GRACE PERIOD 10 MINUTES
)
GROUP BY title_id, season_id
EMIT CHANGES;