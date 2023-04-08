package org.capps.models;

public class PodList {
    public String name;
    public String status;
    public String startTime;

    public PodList(String name, String status, String startTime) {
        this.name = name;
        this.status = status;
        this.startTime = startTime;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStartTime() {
        return this.startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
}
