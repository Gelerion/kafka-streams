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