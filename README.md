cluster tasks for java (CT4j)  
======================================

## Features

* Cluster aware (tasks runs at single cluster node)
* retrying and error handling
* task workflows
* Spring boot support
* can handle millions of tasks

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
// TODO
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




### TODO:
v1:

- [ ] tests
    - [x] scheduler
    - [ ] task runner
    - [ ] cluster/network Instance stuff

- [ ] cleanup/fix/refactor performance stats
- [ ] recurring cluster tasks support
- [ ] detect crashed instances
- [ ] detect too long running tasks
- [x] custom task factory
- [ ] configuration support
    - [ ] validate tasks (serialize & deserialize input & task on queue)
- [ ] docs
- [ ] samples
- rename (cluster tasks 4 java, ct4j)



next:
- [ ] hazelcast runner
- [ ] lambda runner
- [ ] db based time provider
- [ ] jdbc persistence
- [ ] enable tryToRunRetryOnDifferentNode
- [ ] enable maximumNumberOfConcurrentTasksOfThisType
- [ ] enable max running time
- [ ] tasks shortcut
