package com.whiletrue.clustertasks.tasks;

public class RetryPolicy {
    private int maxRetries;
    private int retryDelay;
    private float retryBackoffFactor;
    private boolean tryToRunRetryOnDifferentNode;

    public RetryPolicy(int maxRetries, int retryDelay, float retryBackoffFactor, boolean forceRunOnDifferentNode) {
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
        this.retryBackoffFactor = retryBackoffFactor;
        this.tryToRunRetryOnDifferentNode = forceRunOnDifferentNode;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getRetryDelay() {
        return retryDelay;
    }

    public float getRetryBackoffFactor() {
        return retryBackoffFactor;
    }

    public boolean isTryToRunRetryOnDifferentNode() {
        return tryToRunRetryOnDifferentNode;
    }
}
