package com.whiletrue.clustertasks.tasks;

public enum ScheduledTaskAction {
    AlwaysAdd(false, false),
    SingletonTaskReplace(false, true),
    SingletonTaskKeepExisting(false, false),
    TaskPerInputReplace(true, true),
    TaskPerInputKeepExisting(true, false),
    ;

    private boolean isPerInput;
    private boolean isReplaceTasks;

    public boolean isReplaceTasks() {
        return isReplaceTasks;
    }

    ScheduledTaskAction(boolean isPerInput, boolean isReplaceTasks) {
        this.isPerInput = isPerInput;
        this.isReplaceTasks = isReplaceTasks;
    }

    public boolean isPerInput() {
        return isPerInput;
    }
}

