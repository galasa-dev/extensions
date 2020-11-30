package apiserver

import (
	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/apis/galasa/v1alpha1"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	v1beta1 "k8s.io/api/networking/v1beta1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
)

type APIServer struct {
	InternalService *corev1.Service
	ExposedService  *corev1.Service
	BootstrapConf   *corev1.ConfigMap
	TestCatalog     *corev1.ConfigMap
	PersistentVol   *corev1.PersistentVolumeClaim
	Deployment      *appsv1.Deployment
	Ingress         *v1beta1.Ingress
}

func New(cr *galasav1alpha1.GalasaEcosystem, cps *corev1.Service) *APIServer {
	ports := cps.Spec.Ports
	var nodePort int32
	for _, p := range ports {
		nodePort = p.NodePort
	}
	cpsURI := cr.Spec.ExternalHostname + ":" + String(nodePort)
	return &APIServer{
		InternalService: generateInternalService(cr),
		ExposedService:  generateExposedService(cr),
		BootstrapConf:   generateBootstrapConfigMap(cr, cpsURI),
		TestCatalog:     generateTestCatalogConfigMap(cr),
		PersistentVol:   generatePersistentVolumeClaim(cr),
		Deployment:      generateDeployment(cr),
		Ingress:         generateIngress(cr),
	}
}
func generateIngress(cr *galasav1alpha1.GalasaEcosystem) *v1beta1.Ingress {
	return &v1beta1.Ingress{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-apiserver-ingress",
			Namespace: cr.Namespace,
			Labels: map[string]string{
				"app": cr.Name + "-apiserver",
			},
			Annotations: map[string]string{
				"kubernetes.io/ingress.class": cr.Spec.IngressClass,
			},
		},
		Spec: v1beta1.IngressSpec{
			Rules: []v1beta1.IngressRule{
				{
					IngressRuleValue: v1beta1.IngressRuleValue{
						HTTP: &v1beta1.HTTPIngressRuleValue{
							Paths: []v1beta1.HTTPIngressPath{
								{
									Path: "/bootstrap",
									Backend: v1beta1.IngressBackend{
										ServiceName: cr.Name + "-api-external-service",
										ServicePort: intstr.FromInt(8080),
									},
								},
							},
						},
					},
				},
			},
		},
	}
}

func generateDeployment(cr *galasav1alpha1.GalasaEcosystem) *appsv1.Deployment {
	return &appsv1.Deployment{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-apiserver",
			Namespace: cr.Namespace,
			Labels: map[string]string{
				"app": cr.Name + "-apiserver",
			},
		},
		Spec: appsv1.DeploymentSpec{
			Replicas: cr.Spec.APIServer.Replicas,
			Selector: &metav1.LabelSelector{
				MatchLabels: map[string]string{
					"app": cr.Name + "-apiserver",
				},
			},
			Template: corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: cr.Name + "-apiserver",
					Labels: map[string]string{
						"app": cr.Name + "-apiserver",
					},
				},
				Spec: corev1.PodSpec{
					NodeSelector: cr.Spec.APIServer.NodeSelector,
					InitContainers: []corev1.Container{
						{
							Name:            "init-chown-data",
							Image:           "busybox:latest",
							ImagePullPolicy: corev1.PullPolicy(cr.Spec.ImagePullPolicy),
							Command: []string{
								"chown", "-R", "1000", "/data",
							},
							VolumeMounts: []corev1.VolumeMount{
								corev1.VolumeMount{
									Name:      "data",
									MountPath: "/data",
									SubPath:   "",
								},
							},
						},
					},
					Containers: []corev1.Container{
						{
							Name:            cr.Name + "-resource-monitor",
							Image:           cr.Spec.DockerRegistry + "/galasa-boot-embedded-amd64:" + cr.Spec.GalasaVersion,
							ImagePullPolicy: corev1.PullPolicy(cr.Spec.ImagePullPolicy),
							Command:         []string{"java"},
							Args: []string{
								"-jar",
								"boot.jar",
								"--obr",
								"file:galasa.obr",
								"--trace",
								"--api",
								"--bootstrap",
								"file:/bootstrap.properties",
							},
							Ports: []corev1.ContainerPort{
								{
									Name:          "metrics",
									ContainerPort: 9010,
								},
								{
									Name:          "health",
									ContainerPort: 9011,
								},
								{
									Name:          "http",
									ContainerPort: 8080,
								},
							},
							LivenessProbe: &corev1.Probe{
								InitialDelaySeconds: 60,
								PeriodSeconds:       60,
								Handler: corev1.Handler{
									HTTPGet: &corev1.HTTPGetAction{
										Path: "/health",
										Port: intstr.FromInt(8080),
									},
								},
							},
							ReadinessProbe: &corev1.Probe{
								InitialDelaySeconds: 3,
								PeriodSeconds:       1,
								Handler: corev1.Handler{
									HTTPGet: &corev1.HTTPGetAction{
										Path: "health",
										Port: intstr.FromInt(8080),
									},
								},
							},
							VolumeMounts: []corev1.VolumeMount{
								{
									Name:      "bootstrap",
									MountPath: "/bootstrap.properties",
									SubPath:   "bootstrap.properties",
								},
								{
									Name:      "testcatalog",
									MountPath: "/galasa/load/dev.galasa.testcatalog.cfg",
									SubPath:   "dev.galasa.testcatalog.cfg",
								},
								{
									Name:      "data",
									MountPath: "/galasa/testcatalog",
								},
							},
						},
					},
					Volumes: []corev1.Volume{
						{
							Name: "bootstrap",
							VolumeSource: corev1.VolumeSource{
								ConfigMap: &corev1.ConfigMapVolumeSource{
									LocalObjectReference: corev1.LocalObjectReference{
										Name: "bootstrap-file",
									},
								},
							},
						},
						{
							Name: "testcatalog",
							VolumeSource: corev1.VolumeSource{
								ConfigMap: &corev1.ConfigMapVolumeSource{
									LocalObjectReference: corev1.LocalObjectReference{
										Name: "testcatalog-file",
									},
								},
							},
						},
						{
							Name: "data",
							VolumeSource: corev1.VolumeSource{
								PersistentVolumeClaim: &corev1.PersistentVolumeClaimVolumeSource{
									ClaimName: cr.Name + "-apiserver-pvc",
								},
							},
						},
					},
				},
			},
		},
	}
}

func generatePersistentVolumeClaim(cr *galasav1alpha1.GalasaEcosystem) *corev1.PersistentVolumeClaim {
	return &corev1.PersistentVolumeClaim{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-apiserver-pvc",
			Namespace: cr.Namespace,
		},
		Spec: corev1.PersistentVolumeClaimSpec{
			AccessModes: []corev1.PersistentVolumeAccessMode{
				"ReadWriteOnce",
			},
			StorageClassName: cr.Spec.StorageClassName,
			Resources: corev1.ResourceRequirements{
				Requests: corev1.ResourceList{
					corev1.ResourceName(corev1.ResourceStorage): resource.MustParse(cr.Spec.APIServer.Storage),
				},
			},
		},
	}
}

func generateBootstrapConfigMap(cr *galasav1alpha1.GalasaEcosystem, cpsURI string) *corev1.ConfigMap {

	return &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "bootstrap-file",
			Namespace: cr.Namespace,
		},
		Data: map[string]string{
			"bootstrap.properties": `framework.config.store=etcd:` + cpsURI + `
framework.extra.bundles=dev.galasa.cps.etcd,dev.galasa.ras.couchdb
			`,
		},
	}
}

func generateTestCatalogConfigMap(cr *galasav1alpha1.GalasaEcosystem) *corev1.ConfigMap {
	return &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "testcatalog-file",
			Namespace: cr.Namespace,
		},
		Data: map[string]string{
			"dev.galasa.testcatalog.cfg": "framework.testcatalog.directory=file:/galasa/testcatalog",
		},
	}
}

func generateInternalService(cr *galasav1alpha1.GalasaEcosystem) *corev1.Service {
	return &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Annotations: map[string]string{
				"service.alpha.kubernetes.io/tolerate-unready-endpoints": "true",
			},
			Name:      cr.Name + "-api-internal-service",
			Namespace: cr.Namespace,
			Labels: map[string]string{
				"app": cr.Name + "-apiserver",
			},
		},
		Spec: corev1.ServiceSpec{
			Ports: []corev1.ServicePort{
				{
					Name:       "http",
					TargetPort: intstr.FromInt(8080),
					Port:       8080,
				},
				{
					Name:       "metrics",
					TargetPort: intstr.FromInt(9010),
					Port:       9010,
				},
				{
					Name:       "health",
					TargetPort: intstr.FromInt(9011),
					Port:       9011,
				},
			},
			Selector: map[string]string{
				"app": cr.Name + "-apiserver",
			},
		},
	}
}

func generateExposedService(cr *galasav1alpha1.GalasaEcosystem) *corev1.Service {
	return &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-api-external-service",
			Namespace: cr.Namespace,
		},
		Spec: corev1.ServiceSpec{
			Type: corev1.ServiceType(corev1.ServiceTypeNodePort),
			Ports: []corev1.ServicePort{
				{
					Name:       "http",
					TargetPort: intstr.FromInt(8080),
					Port:       8080,
				},
			},
			Selector: map[string]string{
				"app": cr.Name + "-apiserver",
			},
		},
	}
}

// Fast String int32 to string
func String(n int32) string {
	buf := [11]byte{}
	pos := len(buf)
	i := int64(n)
	signed := i < 0
	if signed {
		i = -i
	}
	for {
		pos--
		buf[pos], i = '0'+byte(i%10), i/10
		if i == 0 {
			if signed {
				pos--
				buf[pos] = '-'
			}
			return string(buf[pos:])
		}
	}
}
