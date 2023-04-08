package org.capps;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.capps.models.PodList;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;

@Path("/k8s")
public class k8sDeploymentResource {
        @Inject
        KubernetesClient kubernetesClient;

        @POST
        @Produces(MediaType.APPLICATION_JSON)
        @Path("/deploy")
        public Map<String, Object> updateDeployment(DeploymentParams params) {
                MixedOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deployments = kubernetesClient
                                .apps().deployments();
                Deployment deployment = deployments.inNamespace(params.getNamespace())
                                .withName(params.getDeploymentName())
                                .get();
                if (deployment == null) {
                        deployment = createNewDeployment(params.getDeploymentName(),
                                        params.getImage(),
                                        params.getPort(),
                                        Optional.ofNullable(params.getEnv()));
                        var resource = kubernetesClient.resource(deployment).inNamespace(params.getNamespace());
                        resource.create();
                        // deployments.inNamespace(namespace).create(deployment)
                        Service service = kubernetesClient.services()
                                        .inNamespace(params.getNamespace())
                                        .withName(params.getDeploymentName())
                                        .get();
                        if (service == null) {
                                service = createNewService(params.getDeploymentName(), params.getPort(), deployment);
                                service = kubernetesClient.resource(service).inNamespace(params.getNamespace())
                                                .create();
                        }
                        HashMap<String, Object> resp = new HashMap<>();
                        resp.put("status", "success");
                        resp.put("message", "Successfully created Deployment " + params.getDeploymentName());
                        resp.put("serviceStatus", service.getStatus().toString());
                        return resp;
                }
                deployment.getSpec().getTemplate().getSpec().getContainers().get(0)
                                .setImage(params.getImage());
                if (params.getEnv() != null) {
                        var envVars = deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
                        envVars.addAll(params.getEnv().entrySet().stream().map(item -> new EnvVarBuilder()
                                        .withName(item.getKey()).withValue(item.getValue()).build())
                                        .collect(Collectors.toList()));
                        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setEnv(envVars);
                }
                var resource = kubernetesClient.resource(deployment).inNamespace(params.getNamespace());
                resource.patch();
                HashMap<String, Object> resp = new HashMap<>();
                resp.put("status", "success");
                resp.put("message", "Successfully updated Deployment with image" + params.getImage());
                resp.put("deploymentStatus", new Integer[] { deployment.getStatus().getUpdatedReplicas(),
                                deployment.getStatus().getReplicas() });
                return resp;
        }

        @POST
        @Produces(MediaType.APPLICATION_JSON)
        @Path("/runjob")
        public Map<String, Object> runJob(JobParams params) {
                Job job = createNewJob(params.getName(), params.getImage(), params.getCmdArgs(),
                                Optional.ofNullable(params.getEnv()));
                kubernetesClient.resource(job).inNamespace(params.getNamespace()).create();
                HashMap<String, Object> resp = new HashMap<>();
                resp.put("status", "success");
                resp.put("message", "Successfully created job");
                return resp;
        }

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("/{namespace}/pods")
        public List<PodList> getPodList(@PathParam("namespace") String namespace) {
                return kubernetesClient.pods().inNamespace(namespace).list().getItems().stream()
                                .map(item -> new PodList(item.getMetadata().getName(), item.getStatus().getPhase(),
                                                item.getStatus().getStartTime()))
                                .collect(Collectors.toList());
        }

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("/{namespace}/jobs")
        public Object getJobList(@PathParam("namespace") String namespace) {
                return kubernetesClient.batch().v1().jobs().inNamespace(namespace).list().getItems()
                                .stream().map(item -> {
                                        HashMap<String, Object> map = new HashMap<>();
                                        map.put("name", item.getMetadata().getName());
                                        map.put("succeeded", item.getStatus().getSucceeded());
                                        map.put("active", item.getStatus().getActive());
                                        map.put("failed", item.getStatus().getFailed());
                                        return map;
                                }).collect(Collectors.toList());
        }

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("/{namespace}/job/{job}")
        public Object getJobList(@PathParam("namespace") String namespace, @PathParam("job") String job) {
                return kubernetesClient.batch().v1().jobs().inNamespace(namespace).withName(job).get();
        }

        private static Job createNewJob(String name, String image, List<String> cmdArgs,
                        Optional<Map<String, String>> envMap) {
                List<EnvVar> envVarlist = new java.util.ArrayList<EnvVar>();
                envMap.ifPresent(env -> {
                        envVarlist.addAll(env.entrySet().stream().map(item -> new EnvVarBuilder()
                                        .withName(item.getKey()).withValue(item.getValue()).build())
                                        .collect(Collectors.toList()));
                });

                Container container = new ContainerBuilder()
                                .withName(name)
                                .withImage(image)
                                .withCommand(cmdArgs)
                                .withEnv(envVarlist)
                                .build();

                return new JobBuilder()
                                .withNewMetadata().withName(name).endMetadata()
                                .withNewSpec()
                                .withTtlSecondsAfterFinished(300)
                                .withNewTemplate()
                                .withNewSpec()
                                .withContainers(container)
                                .withRestartPolicy("Never")
                                .endSpec()
                                .endTemplate()
                                .endSpec()
                                .build();

        }

        private static Deployment createNewDeployment(String name, String imageName, int port,
                        Optional<Map<String, String>> envMap) {
                List<EnvVar> envVarlist = new java.util.ArrayList<EnvVar>();
                envMap.ifPresent(env -> {
                        envVarlist.addAll(env.entrySet().stream().map(item -> new EnvVarBuilder()
                                        .withName(item.getKey()).withValue(item.getValue()).build())
                                        .collect(Collectors.toList()));
                });
                return new DeploymentBuilder()
                                .withNewMetadata()
                                .withName(name)
                                .endMetadata()
                                .withNewSpec()
                                .withReplicas(2)
                                .withMinReadySeconds(2)
                                .withNewSelector()
                                .addToMatchLabels("app", name)
                                .endSelector()
                                .withNewTemplate()
                                .withNewMetadata()
                                .addToLabels("app", name)
                                .endMetadata()
                                .withNewSpec()
                                .addNewContainer()
                                .withImage(imageName)
                                .withImagePullPolicy("Always")
                                .withName(name)
                                .addNewPort()
                                .withContainerPort(port)
                                .endPort()
                                .addAllToEnv(envVarlist)
                                .endContainer()
                                .endSpec()
                                .endTemplate()
                                .withNewStrategy()
                                .withType("RollingUpdate")
                                .withNewRollingUpdate()
                                .withMaxUnavailable(new IntOrString(1))
                                .withMaxSurge(new IntOrString(2))
                                .endRollingUpdate()
                                .endStrategy()
                                .endSpec()
                                .build();
        }

        private static Service createNewService(String serviceName, int port, Deployment deployment) {
                return new ServiceBuilder()
                                .withNewMetadata()
                                .withName(serviceName)
                                .endMetadata()
                                .withNewSpec()
                                .withSelector(deployment.getSpec().getSelector().getMatchLabels())
                                .addNewPort()
                                .withName("http")
                                .withPort(port)
                                .withTargetPort(new IntOrString(port))
                                .endPort()
                                .endSpec()
                                .build();

        }
}
