package org.capps;

import java.util.Map;
import java.util.Optional;

public class DeploymentParams {
    public String deploymentName;
    public String namespace;
    public String image;
    public int port;
    public Map<String,String> env;

    public Map<String,String> getEnv() {
        return this.env;
    }

    public void setEnv(Map<String,String> env) {
        this.env = env;
    }

    

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getImage() {
        return this.image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public DeploymentParams() {
    }

    public String getDeploymentName() {
        return this.deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
    
    
}
