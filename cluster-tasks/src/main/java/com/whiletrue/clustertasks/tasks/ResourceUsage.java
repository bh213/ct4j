package com.whiletrue.clustertasks.tasks;

import java.util.Objects;

public class ResourceUsage {
    private float cpuCoreUsage; // 1.0 per full core. e.g. 4.0 for 4 core machine. 0.5 for 50% on single core.
    private float maximumMemoryUsageInMb;
    private float customResource1;
    private float customResource2;
    private String customResource1Name = "<custom resource 1>";
    private String customResource2Name = "<custom resource 2>";
    ;


    public ResourceUsage(float cpuCoreUsage, float maximumMemoryUsageInMb) {
        this.cpuCoreUsage = cpuCoreUsage;
        this.maximumMemoryUsageInMb = maximumMemoryUsageInMb;
    }

    public ResourceUsage(float cpuCoreUsage, float maximumMemoryUsageInMb, String customResource1Name, float customResource1) {
        this(cpuCoreUsage, maximumMemoryUsageInMb);
        this.customResource1 = customResource1;
        this.customResource1Name = Objects.requireNonNull(customResource1Name);
    }

    public ResourceUsage(float cpuCoreUsage, float maximumMemoryUsageInMb, String customResource1Name, float customResource1, String customResource2Name, float customResource2) {
        this(cpuCoreUsage, maximumMemoryUsageInMb, customResource1Name, customResource1);
        this.customResource2 = customResource2;
        this.customResource2Name = Objects.requireNonNull(customResource2Name);
    }

    public ResourceUsage(ResourceUsage currentResourceUsage) {
        this.customResource2 = currentResourceUsage.customResource2;
        this.customResource1 = currentResourceUsage.customResource1;
        this.customResource1Name = currentResourceUsage.customResource1Name;
        this.customResource2Name = currentResourceUsage.customResource2Name;
        this.cpuCoreUsage = currentResourceUsage.cpuCoreUsage;
        this.maximumMemoryUsageInMb = currentResourceUsage.maximumMemoryUsageInMb;
    }

    public float getCpuCoreUsage() {
        return cpuCoreUsage;
    }

    public float getMaximumMemoryUsageInMb() {
        return maximumMemoryUsageInMb;
    }

    public float getCustomResource1() {
        return customResource1;
    }

    public float getCustomResource2() {
        return customResource2;
    }

    public boolean addIfResourcesAreAvailable(ResourceUsage resourcesToBeUsed){
        if (resourcesToBeUsed == null) return true;
        if (this.cpuCoreUsage >= resourcesToBeUsed.cpuCoreUsage &&
        this.maximumMemoryUsageInMb >= resourcesToBeUsed.maximumMemoryUsageInMb &&
        this.customResource1 >= resourcesToBeUsed.customResource1 &&
        this.customResource2 >= resourcesToBeUsed.customResource2) {
            subtract(resourcesToBeUsed);
            return true;
        } else return false;
    }

    public void add(ResourceUsage additionalResource) {
        if (additionalResource == null) return;
        this.cpuCoreUsage += additionalResource.cpuCoreUsage;
        this.maximumMemoryUsageInMb += additionalResource.maximumMemoryUsageInMb;
        this.customResource1 += additionalResource.customResource1;
        this.customResource2 += additionalResource.customResource2;
    }

    public void subtract(ResourceUsage additionalResource) {
        if (additionalResource == null) return;
        this.cpuCoreUsage -= additionalResource.cpuCoreUsage;
        this.maximumMemoryUsageInMb -= additionalResource.maximumMemoryUsageInMb;
        this.customResource1 -= additionalResource.customResource1;
        this.customResource2 -= additionalResource.customResource2;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ResourceUsage{");
        sb.append("[cpu:").append(cpuCoreUsage);
        sb.append("  mem:").append(maximumMemoryUsageInMb);
        sb.append("MB, ")
                .append(customResource1Name)
                .append(":")
                .append(customResource1)
                .append(" ")
                .append(customResource2Name)
                .append(":")
                .append(customResource2);
        sb.append(']');
        return sb.toString();
    }
}
