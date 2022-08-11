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