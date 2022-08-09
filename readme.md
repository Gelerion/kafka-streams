### Creating a New Project

```sh
./gradlew init \
 --type java-application \
 --dsl groovy \
 --test-framework junit-jupiter \
 --project-name project_name \
 --package com.gelerion.kafak.streams
```

###
Running task from a subproject

```sh
./gradlew :subproject:task_name --info
```


## Topics
- [01_hello_world](01_hello_world)
  - Processor API and DSL API examples
- [02_crypto_sentiment](02_crypto_sentiment)
  - Branching, filtering and merging streams
  - Avro serialization and schema registry
- [03_video_game_leaderboard](03_video_game_leaderboard)
  - KStream, KTable and GlobalKTable
  - KStream-KStream, KStream-KTable and KTable-GlobalKTable joins
  - Interactive queries
- [04_patient_monitoring](04_patient_monitoring)
  - Window joins
  - Suppression
  - Time semantics
- [05_digital_twin_service](05_digital_twin_service)
  - Processor API
  - Stateless and Stateful stream processing
  - Periodic functions with Punctuate
- [06_monitoring_changes_at_netflix](06_monitoring_changes_at_netflix)
  - ksqlDB
  
#### State Management
- Fault Tolerance
  - Changelog Topics. (state stores are backed by changelog topics)
  - Standby Replicas
- Rebalancing
  - Preventing State Migration
    - Sticky Assignment (to help prevent stateful tasks from being reassigned, Kafka Streams uses a custom partition assignment strategy that attempts to reassign tasks to instances that previously owned the task)
    - Static Membership
  - Incremental Cooperative Rebalancing (>= 2.4)
- Controlling State Size
  - Tombstones
  - Window retention
  - Aggressive topic compaction
  - Fixed-size LRU cache