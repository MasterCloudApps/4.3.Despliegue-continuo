El contenido de la carpeta fleet-infra debe incorporarse al repositorio del mismo nombre que crea flux durante el bootstraping. 

Incluye:

* Flux
* Istio
* Flagger

La carpeta istio contiene deployments de flagger e istio que se aplican como cualquier otro recurso en Flux: definiendo un GitRepository en la carpeta my-cluster (manifest istio.yaml).

Los ficheros en esta carpeta están sujetos a la licencia incluida en la misma (Apache 2.0), son autoría de @stefanprodan (Stefan Prodan) y están cogidos del repositorio: https://github.com/stefanprodan/gitops-istio. 