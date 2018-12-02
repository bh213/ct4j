package com.whiletrue.ct4j.tasks;

public class DummyAutowiredClass {
    private String name;
    private String value;

    public DummyAutowiredClass(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DummyAutowiredClass{");
        sb.append("name='").append(name).append('\'');
        sb.append(", value='").append(value).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
