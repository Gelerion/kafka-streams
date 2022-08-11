INSERT INTO production_changes (
    uuid,
    title_id,
    change_type,
    before,
    after,
    created_at
) VALUES (
   1,
   1,
   'season_length',
   STRUCT(season_id := 1, episode_count := 12),
   STRUCT(season_id := 1, episode_count := 8),
   '2021-02-08 10:00:00'
 );

INSERT INTO titles VALUES (1, 'Stranger Things');
INSERT INTO titles VALUES (2, 'Black Mirror');
INSERT INTO titles VALUES (3, 'Bojack Horseman');

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