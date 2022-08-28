package org.capps;

import java.util.List;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.SystemException;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.quarkus.runtime.StartupEvent;

public class PodWatcher {

    @Inject
    KubernetesClient kubernetesClient;

    void editDeployments(String ingressHostname, Deployment resource) {

        System.out.println("Editing deployment");
        Deployment deployment = new DeploymentBuilder(resource)
                .editOrNewSpec().editOrNewTemplate().editOrNewSpec().editContainer(0)
                .addToEnv(new EnvVar("ingressHostname", ingressHostname, null))
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

        kubernetesClient.apps().deployments().createOrReplace(deployment);

    }

    void startup(@Observes StartupEvent event) {
        // System.out.println("Ingress size: "
        // +
        // kubernetesClient.network().v1().ingresses().inAnyNamespace().list().getItems().size());
        final String ingressHostname = kubernetesClient.network().v1().ingresses().withName("nginx-ingress").get()
                .getStatus().getLoadBalancer().getIngress().get(0).getHostname();

        kubernetesClient.apps().deployments().withLabel("addIngressHostValToEnv", "yes").list().getItems()
                .forEach(deploy -> {
                    System.out.println("Found  deployment at startup: "+ deploy.getMetadata().getName());
                    editDeployments(ingressHostname, deploy);
                    // Deployment deployment = new DeploymentBuilder(deploy)
                    // .editOrNewSpec().editOrNewTemplate().editOrNewSpec().editContainer(0)
                    // .addToEnv(new EnvVar("ingressHostname", ingressHostname, null))
                    // .endContainer()
                    // .endSpec()
                    // .endTemplate()
                    // .endSpec()
                    // .build();

                    // kubernetesClient.apps().deployments().createOrReplace(deployment);
                });

        // kubernetesClient.network().v1().ingresses().inAnyNamespace().list().getItems().forEach(res
        // -> {
        // String ingressHostname =
        // res.getStatus().getLoadBalancer().getIngress().get(0).getHostname();
        // System.out.println("Found ingress with name:"+res.getMetadata().getName()+",
        // ingressHostname" + ingressHostname);
        // System.out.println(
        // "Found ingress with name:" +
        // res.getStatus().getLoadBalancer().getIngress().get(0).getIp());
        // System.out.println("Found ingress with name:"
        // +
        // res.getStatus().getLoadBalancer().getIngress().get(0).getAdditionalProperties());
        // System.out.println("Found ingress with name:" +
        // res.getAdditionalProperties());
        // System.out.println("Found ingress with name:" + res.getMetadata());
        // System.out.println("Found ingress with name:" + res.getMetadata().getName());

        // List<EnvVar> envVars =
        // kubernetesClient.apps().deployments().withName("nginx").get().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
        // envVars.add(new EnvVar("ingressHostname", ingressHostname, null));

        // kubernetesClient.apps().deployments().withName("nginx").edit(d -> new
        // DeploymentBuilder(d)
        // .editOrNewSpec().editOrNewTemplate().editOrNewSpec().editContainer(0)
        // .addToEnv(new EnvVar("ingressHostname", ingressHostname, null))
        // .endContainer()
        // .endSpec()
        // .endTemplate()
        // .endSpec()
        // .build());

        // List<EnvVar> envVars=
        // kubernetesClient.pods().withName("nginx").get().getSpec().getContainers().get(0).getEnv();
        // envVars.add(new EnvVar("ingressHostname", ingressHostname, null));
        // System.out.println("envVars: "+envVars.toString());
        // kubernetesClient.pods().withName("nginx").edit( p -> new
        // PodBuilder(p).editOrNewSpec()
        // .editContainer(0).addAllToEnv(envVars).endContainer().endSpec()
        // .editOrNewMetadata().addToLabels("ingressHostname","yes").endMetadata().build());
        // });

        // List<Pod> pods = kubernetesClient.pods().list().getItems();
        // for (Pod pod : pods) {
        // System.out.println("Found resource with name: " + pod.getMetadata().getName()
        // + " version"
        // + pod.getMetadata().getResourceVersion());

        // }

        kubernetesClient.apps().deployments().watch(new Watcher<Deployment>() {

            @Override
            public void eventReceived(Action action, Deployment resource) {
                System.out.println("Received deployment action" + action + " for resource with name"
                        + resource.getMetadata().getName());
                if (resource.getMetadata().getLabels().containsKey("addIngressHostValToEnv") &&
                        resource.getMetadata().getLabels().get("addIngressHostValToEnv").equals("yes") && action == Action.ADDED)
                    editDeployments(ingressHostname, resource);
            }

            @Override
            public void onClose(WatcherException cause) {
                if (cause != null) {
                    cause.printStackTrace();
                    System.exit(-1);
                }

            }

        });

        kubernetesClient.pods().watch(new Watcher<Pod>() {

            @Override
            public void eventReceived(Action action, Pod resource) {
                System.out.println("Recevied action:" + action + " for resource with name: "
                        + resource.getMetadata().getName() + " version"
                        + resource.getMetadata().getResourceVersion());

            }

            @Override
            public void onClose(WatcherException cause) {
                if (cause != null) {
                    cause.printStackTrace();
                    System.exit(-1);
                }

            }
        });
    }

}
