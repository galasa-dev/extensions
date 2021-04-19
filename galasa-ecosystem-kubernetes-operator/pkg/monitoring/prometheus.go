package monitoring

import (
	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/apis/galasa/v1alpha1"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
)

type Prometheus struct {
	ConfigMap             *corev1.ConfigMap
	PersistentVolumeClaim *corev1.PersistentVolumeClaim
	ExposedService        *corev1.Service
	InternalService       *corev1.Service
	Deployment            *appsv1.Deployment
}

func NewPrometheus(cr *galasav1alpha1.GalasaEcosystem) *Prometheus {
	return &Prometheus{
		ConfigMap:             generatePrometheusConfigMap(cr),
		PersistentVolumeClaim: generatePrometheusPVC(cr),
		ExposedService:        generatePrometheusExposedService(cr),
		InternalService:       generatePrometheusInternalService(cr),
		Deployment:            generatePrometheusDeployment(cr),
	}
}

func generatePrometheusDeployment(cr *galasav1alpha1.GalasaEcosystem) *appsv1.Deployment {
	defaultMode := int32(420)
	return &appsv1.Deployment{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-prometheus",
			Namespace: cr.Namespace,
			Labels: map[string]string{
				"app": cr.Name + "-prometheus",
			},
		},
		Spec: appsv1.DeploymentSpec{
			Replicas: cr.Spec.Monitoring.PrometheusReplicas,
			Selector: &metav1.LabelSelector{
				MatchLabels: map[string]string{
					"app": cr.Name + "-prometheus",
				},
			},
			Template: corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: "prometheus",
					Labels: map[string]string{
						"app": cr.Name + "-prometheus",
					},
				},
				Spec: corev1.PodSpec{
					NodeSelector: cr.Spec.Monitoring.NodeSelector,
					InitContainers: []corev1.Container{
						{
							Name:            "init-chown-data",
							Image:           cr.Spec.BusyBoxImageName + ":" + cr.Spec.BusyBoxImageVersion,
							ImagePullPolicy: corev1.PullPolicy(cr.Spec.ImagePullPolicy),
							Command: []string{
								"chown",
								"-R",
								"65534:65534",
								"/data",
							},
							VolumeMounts: []corev1.VolumeMount{
								{
									Name:      "datadir",
									MountPath: "/data",
									SubPath:   "",
								},
							},
						},
					},
					Containers: []corev1.Container{
						{
							Name:            "prometheus",
							Image:           cr.Spec.Monitoring.PrometheusImageName + ":" + cr.Spec.Monitoring.PrometheusImageVersion,
							ImagePullPolicy: corev1.PullPolicy(cr.Spec.ImagePullPolicy),
							Args: []string{
								"--config.file=/etc/prometheus/prometheus.yml",
								"--storage.tsdb.path=/prometheus",
								"--web.external-url=" + cr.Spec.ExternalHostname + ":9090/galasa-prometheus",
								"--web.route-prefix=/",
							},
							Ports: []corev1.ContainerPort{
								{
									Name:          "prometheus-port",
									ContainerPort: 9090,
								},
							},
							VolumeMounts: []corev1.VolumeMount{
								{
									Name:      "prometheus-config",
									MountPath: "/etc/prometheus",
								},
								{
									Name:      "datadir",
									MountPath: "/prometheus",
								},
							},
						},
					},
					Volumes: []corev1.Volume{
						{
							Name: "prometheus-config",
							VolumeSource: corev1.VolumeSource{
								ConfigMap: &corev1.ConfigMapVolumeSource{
									DefaultMode: &defaultMode,
									LocalObjectReference: corev1.LocalObjectReference{
										Name: "prometheus-config",
									},
								},
							},
						},
						{
							Name: "datadir",
							VolumeSource: corev1.VolumeSource{
								PersistentVolumeClaim: &corev1.PersistentVolumeClaimVolumeSource{
									ClaimName: cr.Name + "-prometheus-pvc",
								},
							},
						},
					},
				},
			},
		},
	}
}

func generatePrometheusConfigMap(cr *galasav1alpha1.GalasaEcosystem) *corev1.ConfigMap {
	return &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "prometheus-config",
			Namespace: cr.Namespace,
			Labels: map[string]string{
				"app": cr.Name + "-prometheus",
			},
		},
		Data: map[string]string{
			"prometheus.yml": `
global:
  scrape_interval:     15s
  evaluation_interval: 15s 

scrape_configs:
  - job_name: 'resource-monitor'
    scrape_interval: 5s
    static_configs:
      - targets: ['` + cr.Name + `-resource-monitor-internal-service:9010']
        labels:
          groups: 'test'
  - job_name: 'engine-controller'
    scrape_interval: 5s
    static_configs:
      - targets: ['` + cr.Name + `-engine-controller-internal-service:9010']
        labels:
          groups: 'test'
  - job_name: 'metrics'
    scrape_interval: 5s
    static_configs:
      - targets: ['` + cr.Name + `-metrics-internal-service:9010']
        labels:
          groups: 'test'
`,
		},
	}

}

func generatePrometheusPVC(cr *galasav1alpha1.GalasaEcosystem) *corev1.PersistentVolumeClaim {
	return &corev1.PersistentVolumeClaim{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-prometheus-pvc",
			Namespace: cr.Namespace,
			Labels: map[string]string{
				"app": cr.Name + "-prometheus",
			},
		},
		Spec: corev1.PersistentVolumeClaimSpec{
			AccessModes: []corev1.PersistentVolumeAccessMode{
				corev1.ReadWriteOnce,
			},
			StorageClassName: cr.Spec.StorageClassName,
			Resources: corev1.ResourceRequirements{
				Requests: corev1.ResourceList{
					corev1.ResourceName(corev1.ResourceStorage): resource.MustParse(cr.Spec.Monitoring.PrometheusStorage),
				},
			},
		},
	}
}

func generatePrometheusInternalService(cr *galasav1alpha1.GalasaEcosystem) *corev1.Service {
	return &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-prometheus-internal-service",
			Namespace: cr.Namespace,
			Labels: map[string]string{
				"app": cr.Name + "-prometheus",
			},
		},
		Spec: corev1.ServiceSpec{
			Ports: []corev1.ServicePort{
				{
					Name:       "prometheus",
					TargetPort: intstr.FromInt(9090),
					Port:       9090,
				},
			},
			Selector: map[string]string{
				"app": cr.Name + "-prometheus",
			},
		},
	}
}

func generatePrometheusExposedService(cr *galasav1alpha1.GalasaEcosystem) *corev1.Service {
	return &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-prometheus-external-service",
			Namespace: cr.Namespace,
		},
		Spec: corev1.ServiceSpec{
			Type: corev1.ServiceType(corev1.ServiceTypeNodePort),
			Ports: []corev1.ServicePort{
				{
					Name:       "prometheus-external",
					TargetPort: intstr.FromInt(9090),
					Port:       9090,
				},
			},
			Selector: map[string]string{
				"app": cr.Name + "-prometheus",
			},
		},
	}
}
