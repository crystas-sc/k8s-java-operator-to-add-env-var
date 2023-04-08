# Demonstartion of Kubernetes operator for adding Pod environment variables
This project demostrates the feasibility of adding Environment variables queried from Ingress output
## Requirements
* Docker
* Kind
* kubectl
* Maven
* Java 11

## Kind Installation
```
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.14.0/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind
```

### Kind verification
```
sudo service docker start
kind create cluster
kubectl cluster-info --context kind-kind
```

## Kubectl Installation
```
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
curl -LO "https://dl.k8s.io/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl.sha256"
echo "$(cat kubectl.sha256)  kubectl" | sha256sum --check
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

```

### Kubectl Installation verification
```
kubectl version --client
kubectl get nodes
kubectl cluster-info --context kind-kind
```

## delete Kind Cluster
```
kind delete cluster --name kind
```

## Seting up Nginx Ingress controller 
```
kind create cluster --config dev-kind-ingress.yaml
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
kubectl apply -f nginx.yaml
curl localhost
``` 

## Operator deployment
```
sudo service docker start
mvn clean package -DskipTests
docker build -f src/main/docker/Dockerfile.jvm -t capps/firstk8soperator .
kind load docker-image capps/firstk8soperator:latest --name=dev-kind
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml


kubectl apply -f deploy-operator/clusterrole.yaml
kubectl apply -f deploy-operator/serviceaccount.yaml
kubectl apply -f deploy-operator/clusterrolebinding.yaml
kubectl apply -f deploy-operator/deployment.yaml
kubectl delete deploy operator-example
kubectl delete -A ValidatingWebhookConfiguration ingress-nginx-admission


```
## Deployment REST API Test
```
curl -X POST http://localhost:8080/k8s/deploy  -H 'Content-Type: application/json'  -d '{"deploymentName":"nginx","namespace":"default","image":"nginx:stable-alpine3.17-slim","port":"80","env":{"env1":"val1","env2":"val3"}}'

curl -X POST http://localhost:8080/k8s/runjob  -H 'Content-Type: application/json'  -d '{"name":"hello","namespace":"default","image":"busybox:1.28","cmdArgs":["echo","\"Hello World!! I am $(name)\""],"env":{"name":"Roger"}}'


curl  http://localhost:8080/k8s/default/pods
curl  http://localhost:8080/k8s/default/jobs
curl  http://localhost:8080/k8s/default/job/hello
```

## Reference
https://www.baeldung.com/ops/kubernetes-kind
https://kubernetes.io/docs/tasks/tools/install-kubectl-linux/
https://kind.sigs.k8s.io/docs/user/quick-start/