# Instrucciones de instalación

## 1. Iniciamos Minikube

```bash
minikube start --kubernetes-version v1.20.0
```

## 2. Instalar Haylard

Haylard es una herramienta para configurar instalar y actualizar Spinnaker. Es válido para diferentes entornos, sólo necesita tener un kubectl instalado y configurado a un cluter kubernetes.

Podríamos instalar la aplicación nativamente en Linux, pero vamos a utilizar una imagen ya disponible de Haylard que cuenta con todas las herramientas necesarias para utilizar `kubectl`.

El siguiente comando arrancará Haylard usando docker y compartiendo todos los volumenes necesarios para trabajar con kubectl:

```bash
docker run --name halyard --rm -d --network host  -v "${HOME}"/.hal:/home/spinnaker/.hal \
    -v "${HOME}"/.kube/:/home/spinnaker/.kube -v ~/.kube/:/root/.kube -v "${HOME}"/.minikube/:/root/.minikube \
    -v "${HOME}"/.minikube/:"${HOME}"/.minikube -it ghcr.io/ahmetozer/halyard-container &&
    docker logs -f halyard
```

Cuando el contenedor muestre `Halyard started` estaremos listos para comenzar la instalación.

Cuando el contenedor `haylard` esté arrancado tendremos un entorno preparado para la instalación. Ejecutamos una sesión de bash dentro:

```bash
docker exec -it halyard bash
```

Una vez dentro:

```bash
# Configuramos el contexto (minikube)
CONTEXT=$(kubectl config current-context)

# Creamos un service account para Spinnaker
kubectl apply --context $CONTEXT \
    -f https://spinnaker.io/downloads/kubernetes/service-account.yml

# Guardamos el secreto/token para configurarlo en kubectl
TOKEN=$(kubectl get secret --context $CONTEXT \
   $(kubectl get serviceaccount spinnaker-service-account \
       --context $CONTEXT \
       -n spinnaker \
       -o jsonpath='{.secrets[0].name}') \
   -n spinnaker \
   -o jsonpath='{.data.token}' | base64 --decode)

# Configuramos kubectl
kubectl config set-credentials ${CONTEXT}-token-user --token $TOKEN
kubectl config set-context $CONTEXT --user ${CONTEXT}-token-user
```

Configuramos Haylard para trabajar con el cluster

```bash
# Creamos cuenta y configuramos como provider "kubernetes
hal config provider kubernetes enable

ACCOUNT="my-k8s-account"

hal config provider kubernetes account add ${ACCOUNT} \
    --context ${CONTEXT}

# Configuramos el modo de despliegue distribuido (necesario para kubernetes)
hal config deploy edit --type distributed --account-name $ACCOUNT
```

## 2. Configuramos persistencia para Spinnaker

> TODO: Ver si puede ser opcional con minikube

Spinnaker necesita para persistir la información un almacenamiento externo. Esto se suele hacer en proveedores externos, como AWS S3. Desplegaremos Minio por comodidad para poder usar un servicio similar a S3 para hacer persistencia. Lo desplegaremos localmente como ejemplo. Utilizaremos minio para este ejemplo, pero otros proveedores como S3 pueden ser utilizados.

En una sesión fuera del contenedor de haylard ejecutamos lo siguiente:

```bash
# Configuramos el servicio. Para esta demo pondremos un usuario de prueba
# Utilizamos estos datos aleatorios porque MINIO es muy exigente con su
# configuración
MINIO_ROOT_USER=$(< /dev/urandom tr -dc a-z | head -c${1:-4})
MINIO_ROOT_PASSWORD=$(< /dev/urandom tr -dc _A-Z-a-z-0-9 | head -c${1:-8})
MINIO_PORT="9010"

# Ejecutamos
docker run -it -d --rm -v ~/.minio-data/:/data --name minio-4-spinnaker -p ${MINIO_PORT}:${MINIO_PORT} \
    -e MINIO_ROOT_USER=${MINIO_ROOT_USER} -e  MINIO_ROOT_PASSWORD=${MINIO_ROOT_PASSWORD} \
    minio/minio  server /data --address :${MINIO_PORT}
```

La siguiente información la utilizaremos más adelante:

```bash
echo "
MINIO_ROOT_USER=${MINIO_ROOT_USER}
MINIO_ROOT_PASSWORD=${MINIO_ROOT_PASSWORD}
ENDPOINT=http://$(docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' minio-4-spinnaker):${MINIO_PORT}
"
```

### 3. Configuramos en Haylard la persistencia

Le decimos a Haylard la persistencia que vamos a usar, para que cuando vaya a desplegar el servicio,
utilice el Minio recién desplegado. Entramos de nuevo al container `haylard`:

```bash
docker exec -it halyard bash
```

Y ejecutamos los siguientes comands
```bash
DEPLOYMENT="default"
mkdir -p ~/.hal/$DEPLOYMENT/profiles/
echo spinnaker.s3.versioning: false > ~/.hal/$DEPLOYMENT/profiles/front50-local.yml

# Configuramos aquí las variables de MINIO anteriores
echo ${MINIO_ROOT_PASSWORD} | hal config storage s3 edit --endpoint $ENDPOINT \
    --access-key-id ${MINIO_ROOT_USER} \
    --secret-access-key

# Habilitamos S3
hal config storage edit --type s3
hal config storage s3 edit --path-style-access=true
```

### 4. Desplegamos Spinakker en minikube

Desplegamos:

```
hal deploy apply
```

Y nos conectamos. Este comando redirije las peticiones al cluster para que podamos acceder con localhost...

```bash
hal deploy connect
```

```bash
Forwarding from 127.0.0.1:8084 -> 8084
Forwarding from [::1]:8084 -> 8084
Forwarding from 127.0.0.1:9000 -> 9000
Forwarding from [::1]:9000 -> 9000
```
