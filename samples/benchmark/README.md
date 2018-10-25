Benchmark with postgres and JPA support
============================

Clustered: Yes 

Persistent: Yes 

## Description

This application is split into two parts. 
`generator` keeps creating a configured number of tasks per second and `node`s executes them.

The following tasks are available:

| Name | Description | 
|--- | --- |
| Empty Task| Tests ct4j performance and does nothing|
| Failing Task| Task that always fails, tests errors  and retrying and failure handling|
| Get Url Task| Does GET to specified URL. Tests I/O and handkles either success or failure depending on url configured|
| Single Full Cpu Task| Fully uses a CPU core for a specified number of milliseconds|
    


## Configuration

Check [application-generator.properties](src\main\resources\application-generator.properties) for generator properties.


### Database migration by liquibase 
Migration script is provided by `com.whiletrue:cluster-tasks-spring-boot` and is stored in `classpath:ct4j-liquibase-migration.xml`)

### Running this sample

Create database and edit [application.properties](/src/main/resources/application.properties) connection properties.


Alternatively, use docker. This sample is configured for docker by default.
```bash
docker run --rm -p 5432:5432 --platform=linux -e POSTGRES_USER=ct4j -e POSTGRES_DB=ct4j postgres:9.6-alpine
```


### Run


### Generator
One generator is needed to generate tasks

```bash
SPRING_PROFILES_ACTIVE=generator ./gradlew bootRun
```



### Generator
At least one node is required to run benchmark but you can run as many as you want

```bash
SPRING_PROFILES_ACTIVE=node ./gradlew bootRun
```
