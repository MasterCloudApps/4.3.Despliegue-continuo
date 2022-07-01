# Ejemplo 1 - GitHub Actions

Este proyecto consta de un servidor REST sencillo para la gestión de items.

## Construir la aplicación (en local)

Para construir el JAR del proyecto (y lanzar los test):

```
    ./mvnw clean package
```

## Lanzar la aplicación en local (en local)

Para lanzar la aplicación el local:

```
    java -jar target/items-0.0.1-SNAPSHOT.jar 
```
## Integración con flux

El contenido de esta carpeta debe incluirse en un repositorio items-app. Este repositorio está referenciado desde fleet-infra/clusters/my-cluster/items-app-source.yaml, y en items-kustomization.yaml se especifica que la carpeta dentro del repositorio de items que contiene los manifiestos kubernetes es k8s.
