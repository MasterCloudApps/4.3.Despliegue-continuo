# Ejemplo Flux y Flagger con Istio

En este ejemplo se prepara la infraestructura y se despliega una aplicación de items. Se indican a continuación los pasos necesarios para aplicar el ejemplo.

## Prerequisitos

* Minikube
* Cluster Kubernetes 1.23.0 en minikube
* Flux CLI
* Kubectl CLI
* Un Personal Access Token en nuestra cuenta GitHub para Flux 

## Preparación

Abrir el tunneling de minikube para habilitar LoadBalancer como tipo en servicios:

    minikube tunnel -p <nombre cluster>

Verificar que no salen errores. En caso contrario: Ctrl+C y volver a ejecutar el comando.

Crear un repositorio items-app en la cuenta personal, y commitear el contenido de la carpeta items del ejemplo. El fichero pom.xml debe estar en la raiz del repositorio, de forma que la carpeta k8s esté también en la raiz (es referenciada desde items-app-source.yaml).

Exportar como variables de entorno nuestro usuario GitHub y el Personal Access Token:

    export GITHUB_USER=<gh user>
    export GITHUB_TOKEN=<Personal Access Token>

Instalar Flux:

    flux bootstrap github \   
        --owner=$GITHUB_USER \
        --repository=fleet-infra \
        --branch=main \
        --path=./clusters/my-cluster \
        --personal

Si queremos que el repositorio se llame de cualquier otra manera, podemos cambiar fleet-infra por cualquier otro nombre. Flux creará el repositorio e incluirá unos mínimos manifests para instalar el propio Flux y monitorizar el repositorio.

Ejecutando el siguiente comando:

    flux get kustomizations --watch

Deberíamos obtener algo como esto:

    NAME       	REVISION    	SUSPENDED	READY	MESSAGE                        
    flux-system	main/93310c8	False    	True 	Applied revision: main/93310c8

Flux se ha instalado correctamente. 

## Inclusión de Flagger e Istio

Para poder usar canary releases con Flagger, necesitamos incluir en el repositorio de fleet-infra los manifiestos para que Flux instale istio y flagger. Para ello:

* Incluir en clusters/my-cluster los ficheros istio-version.yaml e istio.yaml
* Incluir en la raiz del repsitorio fleet-infra la carpeta istio

    git add .
    git commit -m "Add Istio and Flagger"
    git push

Flux detectará el cambio en el repositorio de fleet-infra (porque lo está monitorizando) y aplicará los cambios:

    flux get kustomizations --watch
    NAME       	REVISION    	SUSPENDED	READY	MESSAGE                        
    flux-system	main/fd25e73	False    	True 	Applied revision: main/fd25e73	
    flux-system	main/fd25e73d1650250f2c3156a6075c7097c5dba41e	False	Unknown	reconciliation in progress	
    istio-gateway		False	False	waiting to be reconciled	
    istio-gateway		False	False	waiting to be reconciled	
    istio-system		False	False	waiting to be reconciled	
    istio-system		False	False	waiting to be reconciled	
    istio-gateway		False	False	unable to get 'flux-system/istio-system' dependency: Kustomization.kustomize.toolkit.fluxcd.io "istio-system" not found	
    istio-system		False	Unknown	reconciliation in progress	
    flux-system	main/6631e6d	False	True	Applied revision: main/6631e6d	
    istio-system		False	Unknown	running health checks with a timeout of 9m30s	
    istio-gateway		False	False	dependency 'flux-system/istio-system' is not ready	
    istio-system	main/6631e6d	False	True	Applied revision: main/6631e6d	
    istio-gateway		False	Unknown	reconciliation in progress	
    istio-gateway		False	Unknown	running health checks with a timeout of 9m30s	
    istio-gateway	main/6631e6d	False	True	Applied revision: main/6631e6d	
    flux-system	main/6631e6d	False	True	Applied revision: main/6631e6d	

Al final istio-gateway e istio-system deben tener la columna READY a True.

## Desplegar la aplicación

A continuación incluímos en fleet-infra la referencia a nuestra aplicación de items. Esto requiere dos cosas:

### Añadir el GitRepository

Añadimos el fichero items-app-source.yaml en clusters/my-cluster. Este fichero se puede generar automáticamente también con el siguiente comando (después hay que mover el fichero a su localización en my-cluster):

    flux create source git items \
        --url=https://github.com/gortazar/items-app \
        --branch=master \
        --interval=30s \
        --export > ./clusters/my-cluster/items-app-source.yaml

El fichero generado es un CRD de Flux (GitRepository) que contiene información sobre el repositorio que hay que monitorizar para la aplicación items, pero no cómo se despliega. 

    git add .
    git commit -m "Add items repository"
    git push

Flux automáticamente nos indicará que está monitorizando el repositorio. 

### Añadir el Kustomization

El CRD Kustomization le dice a Flux cómo desplegar una aplicación monitorizada a través de un GitRepository. Es el fichero items-kustomization.yaml que hay que copiar en my-cluster, o se puede generar con el siguiente comando:

    flux create kustomization items \
        --target-namespace=items \
        --source=items \
        --path="./k8s" \
        --prune=true \
        --interval=1m \
        --export > ./clusters/my-cluster/items-kustomization.yaml

El source es el nombre del GitRepository donde hay que ir a buscar el path. Es decir, Flux clonará el GitRepository de nombre items usando la url indicada en el GitRepository, se moverá a la carpeta indicada en el Kustomization en el atributo path (k8s) y aplicará los manifiestos encontrados ahí. 

Cuando hagamos push de este fichero Flux intentará desplegar la aplicación. Pero para ello debe tener disponible la imagen docker. Antes de ejecutar este paso vamos a generar la imagen y subirla a minikube:

    mvn spring-boot:build-image -DskipTests -Dspring-boot.build-image.imageName=items:1.0.0
    minikube image load items:1.0.0 -p <nombre cluster>

Ahora empujamos los cambios a fleet-infra y se despliegan automáticamente:

    git add .
    git commit -m "Add Kustomization for items app"
    git push

Flux sincroniza los cambios con el cluster:

    flux get kustomizations --watch
    NAME       	REVISION    	SUSPENDED	READY	MESSAGE
    items	main/2d384fa8c7eb73ac6da35aedc849ed0adef50a6d	False	Unknown	reconciliation in progress	
    items	main/2d384fa	False	True	Applied revision: main/2d384fa

Si vemos los deployments, la aplicación se ha desplegado con Flagger (items-primary presente):

    kubectl -n items get deployments
    NAME            READY   UP-TO-DATE   AVAILABLE   AGE
    items           0/0     1            1           44m
    items-primary   1/1     1            1           44m

### Conclusiones

Para desplegar de forma automática aplicaciones en Flux necesitamos dos cosas:

* El GitRepository en my-cluster que le indica a Flux en qué repo está la aplicación
* El Kustomization en my-cluster que le indica a Flux cómo desplegar una aplicación referenciada a través de un GitRepository

## Actualizar la aplicación

Cuando actualicemos la aplicación:

* Generamos la nueva imagen
* La subimos a Minikube
* Cambiamos la versión en k8s/deployment.yaml
* Empujamos los cambios al repositorio de items
* Flux actualiza la aplicación usando Flagger 
