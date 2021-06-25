# ejem-4-canary-zerodowntime

# Deploy V1

1. Start Minikube

```
minikube start --profile canary-istio --kubernetes-version v1.20.0
```

2. Enable istio-provisioner and istio

```
minikube --profile canary-istio addons enable istio-provisioner
minikube --profile canary-istio addons enable istio
```

3. Deploy database deployment:

```
kubectl apply -f db-deployment.yaml
```

3. Deploy application deployment:

```
kubectl apply -f deployment.yaml
```

4. Create istio gateway

```
kubectl apply -f zerodowntime-gateway.yaml
```

5. Create destination rules we will use in this example:

```
kubectl apply -f zerodowntime-destinationrule.yaml
```

6. Create the virtual service:

```
kubectl apply -f zerodowntime-virtual-service-v1.yaml
```

**V1 is deployed and ready for zerodowntime version updates**

# V1 to V2

1. Deploy new version v2 deployment

```
kubectl apply -f deployment-v2.yaml
```

2. Distribute application 90% to v1 and 10% to v2

```
kubectl apply -f zerodowntime-virtual-service-v1-to-v2.yaml
```

# V2

1. Execute the virtual service to only serve to v2:

```
kubectl apply -f zerodowntime-virtual-service-v2.yaml
```

2. Delete deployment V1

```
kubectl delete deployment zerodowntime-v1
```

# V2 to V3

1. Deploy new version v3 deployment

```
kubectl apply -f deployment-v3.yaml
```

2. Distribute application 90% to v2 and 10% to v3

```
kubectl apply -f zerodowntime-virtual-service-v2-to-v3.yaml
```

# V3

1. Execute the virtual service to only serve to v3:

```
kubectl apply -f zerodowntime-virtual-service-v3.yaml
```

2. Delete deployment v2

```
kubectl delete deployment zerodowntime-v2
```

# V3 to V4

1. Deploy new version v4 deployment

```
kubectl apply -f deployment-v4.yaml
```

2. Distribute application 90% to v3 and 10% to v4

```
kubectl apply -f zerodowntime-virtual-service-v3-to-v4.yaml
```

# V4

1. Execute the virtual service to only serve to v4:

```
kubectl apply -f zerodowntime-virtual-service-v4.yaml
```

2. Delete deployment v3

```
kubectl delete deployment zerodowntime-v3
```

