

 [![](https://jitpack.io/v/bh213/ct4j.svg)](https://jitpack.io/#bh213/ct4j) 
 [![](https://img.shields.io/travis/bh213/ct4j.svg)](https://travis-ci.org/bh213/ct4j) 
 ![https://github.com/bh213/ct4j/blob/master/LICENSE](https://img.shields.io/github/license/mashape/apistatus.svg)
[![Maven Central](https://img.shields.io/maven-central/v/com.whiletrue/ct4j.svg)](https://mvnrepository.com/artifact/com.whiletrue/ct4j)








cluster tasks for java (CT4j)  
======================================

## Features

##### Cluster aware 

Each task runs at exactly one node at the time. Retries can run at the same or different node. Tasks on failed node will be run by different node.

##### Single node and in-memory support

For testing or smaller services running on single node or with in-memory data store are also supported.


##### Retrying and error handling

Task can declare retry behavior or implement logic to decide whether to retry or fail.   


##### Task workflows

Task can spawn next task in workflow on failure or success.

##### Recurring tasks

Tasks can be configured to run periodically (e.g. every 10 seconds) with guaranteed execution on single cluster node only.
 

##### Spring boot support

Uses JPA for database access and cluster support

Tasks can be beans and can participate in dependency injection (with `@Autowired`)   

##### Designed for millions of tasks

This library can process very short or long lived tasks


##### Resource based scheduling

Task can declare required resources, such as CPU and memory usage and scheduler will take this into account. 
This enables running long resource intensive tasks together with very short tasks without having to worry about situation where all resource intensive tasks are started at the same node.


##### Priority support

Tasks are by specified priority, making sure important tasks such as notifications are run before long-running processing tasks.


##### Ready to run

Includes liquibase database migration and docker commands to get you started quickly. See  [samples](samples/README.md). 


## Example task

```java
@ClusterTask(name = "example task", maxRetries = 5, retryDelay = 1000, retryBackoffFactor = 1.5f)
public class ExampleTask extends Task<String> {
    private static Logger log = LoggerFactory.getLogger(ExampleTask.class);

    @Override
    public void run(String input, TaskExecutionContext taskExecutionContext) throws Exception {
        log.info("Task {} called with input {}", taskExecutionContext.getTaskId(), input);
    }
}
```
## Running example task

```java
    final String taskId = taskManager.queueTask(ExampleTask.class, "example input");
```


## Spring boot task

```java
public class AutowiredTask extends Task<String> {
    private static Logger log = LoggerFactory.getLogger(AutowiredTask.class);

    private final DummyAutowiredClass autowiredObject;

    @Autowired
    public AutowiredTask(DummyAutowiredClass autowiredObject) {
        this.autowiredObject = autowiredObject;
    }

    @Override
    public void run(String input, TaskExecutionContext taskExecutionContext) throws Exception {
        log.info("Input was {} and autowired was {}", input, autowiredObject);
    }

    public DummyAutowiredClass getAutowiredObject() {
        return autowiredObject;
    }
}

```


# Configuration


| Property | Description | Default | Options |
|:-------  |:----------- |:------- |:------  |
| `ct4j.task-factory` | Task creation factory | `spring` | `spring` - spring boot factory with `@Autowired` support  <br/> `constructor` - default constructor factory|
| `ct4j.persistence`| Task persistence mode | `memory` | `memory` - in-memory database <br/> `jpa` - JPA based persistence |
| `ct4j.time-provider`| Time provider | `local` | `local` - uses local date provider |


# Samples

[List of samples](samples) 



