Minimal sample with postgres JPA support
============================

Clustered: Yes 

Persistent: Yes 

### Database migration by liquibase 
Migration script is provided by `com.whiletrue:ct4j-spring-boot` and is stored in `classpath:ct4j-liquibase-migration.xml`)

### Running this sample

Create database and edit [application.properties](/src/main/resources/application.properties) connection properties.


Alternatively, use docker (sample is preconfigured for these options):
```bash
docker run -p 5432:5432 -e POSTGRES_USER=ct4j -e POSTGRES_DB=ct4j postgres:9.6-alpine
```


### Run

```bash
./gradlew bootRun
```




