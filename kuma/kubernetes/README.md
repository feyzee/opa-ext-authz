# Kuma with OPA on Kubernetes

An example application deployment using the [OPA-Envoy Plugin](https://www.openpolicyagent.org/docs/latest/envoy-introduction/) with [Kuma](https://kuma.io/) for external authorization in an Envoy-based service mesh.

## Prerequisites

A Kubernetes cluster with Kuma [installed](https://kuma.io/docs/1.3.0/installation/kubernetes/)

_This tutorial has been tested with Kuma 1.3.0_

## Steps

### 1. Start Minikube

[Install](https://kuma.io/docs/1.3.0/installation/kubernetes/) the Kuma Control Plane (if not already installed)

### 2. Create the `kuma-opa-demo` namespace with kuma sidecar-injection enabled

```sh
kubectl apply -f namespace.yaml
```

### 3. Create the Kuma `ProxyTemplate`

The `ProxyTemplate` configuration defines an external authorization filter `envoy.ext_authz` for a gRPC authorization server provided by the OPA sidecar.

```sh
kubectl apply -f proxy-template.yaml
```

### 4. Define the OPA policy

```sh
kubectl create secret generic opa-policy --from-file policy.rego -n kuma-opa-demo
```

### 5. Create App Deployment with OPA sidecar

The `example-app.yaml` includes both a `Deployment` with the `example-app` and `opa` sidecar, as well as the `example-app-service` `Service`
* _The `kuma-dp` sidecar will be automatically injected_
* In order to add an `httpFilter` via the [`ProxyTemplate`](https://kuma.io/docs/1.3.0/policies/proxy-template/#http-filter) (_via **Step 3.** above_), the `Service` must be annotated with `<port>.service.kuma.io/protocol: http` as described in [Protocol support in Kuma](https://kuma.io/docs/1.3.0/policies/protocol-support-in-kuma/)

```sh
kubectl apply -f example-app.yaml
```

Set the `SERVICE_URL` environment variable to the service’s IP/port.

**minikube:**
```sh
export SERVICE_PORT=$(kubectl -n kuma-opa-demo get service example-app-service -o jsonpath='{.spec.ports[?(@.port==8080)].nodePort}')
export SERVICE_HOST=$(minikube ip)
export SERVICE_URL=$SERVICE_HOST:$SERVICE_PORT
echo $SERVICE_URL
```

### 6. Exercise the OPA policy

#### Check that a `GET` request to the `/get` endpoint is **Allowed** (`200 OK`).

```sh
curl -X GET $SERVICE_URL/get -i
```

#### Check that a `POST` request to the `/post` endpoint is **Denied** (`403 Forbidden`).

```sh
curl -X POST $SERVICE_URL/post -i
```
