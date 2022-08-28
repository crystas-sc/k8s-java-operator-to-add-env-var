package org.capps;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

@Singleton
public class KubernetesClientProducer {

    @Produces
    @Singleton
    @Named("namespace")
    private String findNamespace() throws IOException {
      return new String(Files.readAllBytes(Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/namespace")));
    }

    @Produces
    @Singleton
    KubernetesClient newClient(@Named("namespace") String namespace) {
        System.out.println("newClient namespace: "+namespace);
        return new DefaultKubernetesClient().inNamespace(namespace);
    }
}