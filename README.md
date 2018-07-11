cluster tasks for java (CT4j)  
======================================

## Features

##### Cluster aware 

Each task runs at exactly one node at the time. Retries can run at the same or different node. Tasks on failed node will be run by different node.


##### Retrying and error handling

Task can declare retry behavior or implement logic to decide whether to retry or fail.   


##### task workflows

Task can spawn next task in workflow on failure or success.
 

##### Spring boot support

Uses JPA for database access and cluster support

Tasks can be beans and can use `@Autowired`   

##### Designed for millions of tasks

This library can process very short or long lived tasks


##### Resource based scheduling

Task can declare required resources, such as CPU and memory usage and scheduler will take this into account. 
This enables running long resource intensive tasks togather with very short tasks without having to worry about situation where all resource intensive tasks are started at the same node.


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



