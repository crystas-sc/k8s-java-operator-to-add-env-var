package org.capps;

import java.util.List;
import java.util.Map;

public class JobParams {
    public String name;
    public String image;
    public String namespace;
    public List<String> cmdArgs;
    public Map<String,String> env;

    public Map<String,String> getEnv() {
        return this.env;
    }

    public void setEnv(Map<String,String> env) {
        this.env = env;
    }


    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return this.image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public List<String> getCmdArgs() {
        return this.cmdArgs;
    }

    public void setCmdArgs(List<String> cmdArgs) {
        this.cmdArgs = cmdArgs;
    }
}
