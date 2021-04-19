package monitoring

import (
	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/apis/galasa/v1alpha1"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
)

type Metrics struct {
	InternalService *corev1.Service
	ExposedService  *corev1.Service
	Deployment      *appsv1.Deployment
}

func NewMetrics(cr *galasav1alpha1.GalasaEcosystem) *Metrics {
	return &Metrics{
		InternalService: generateMetricsInternalService(cr),
		ExposedService:  generateMetricsExposedService(cr),
		Deployment:      generateMetricsDeployment(cr),
	}
}

func generateMetricsInternalService(cr *galasav1alpha1.GalasaEcosystem) *corev1.Service {
	return &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-metrics-internal-service",
			Namespace: cr.Namespace,
			Labels: map[string]string{
				"app": cr.Name + "-metrics",
			},
		},
		Spec: corev1.ServiceSpec{
			Ports: []corev1.ServicePort{
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
				"app": cr.Name + "-metrics",
			},
		},
	}
}

func generateMetricsExposedService(cr *galasav1alpha1.GalasaEcosystem) *corev1.Service {
	return &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-metrics-external-service",
			Namespace: cr.Namespace,
		},
		Spec: corev1.ServiceSpec{
			Type: corev1.ServiceType(corev1.ServiceTypeNodePort),
			Ports: []corev1.ServicePort{
				{
					Name:       "metrics",
					TargetPort: intstr.FromInt(9010),
					Port:       9010,
				},
			},
			Selector: map[string]string{
				"app": cr.Name + "-metrics",
			},
		},
	}
}

func generateMetricsDeployment(cr *galasav1alpha1.GalasaEcosystem) *appsv1.Deployment {
	version := cr.Spec.GalasaVersion
	if cr.Spec.Monitoring.MetricsImageVersion != "" {
		version = cr.Spec.Monitoring.MetricsImageVersion
	}
	return &appsv1.Deployment{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-metrics",
			Namespace: cr.Namespace,
			Labels: map[string]string{
				"app": cr.Name + "-metrics",
			},
		},
		Spec: appsv1.DeploymentSpec{
			Replicas: cr.Spec.EngineResmon.Replicas,
			Selector: &metav1.LabelSelector{
				MatchLabels: map[string]string{
					"app": cr.Name + "-metrics",
				},
			},
			Template: corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: cr.Name + "-metrics",
					Labels: map[string]string{
						"app": cr.Name + "-metrics",
					},
				},
				Spec: corev1.PodSpec{
					NodeSelector: cr.Spec.Monitoring.NodeSelector,
					Containers: []corev1.Container{
						{
							Name:            "metrics",
							Image:           cr.Spec.Monitoring.MetricsImageName + ":" + version,
							ImagePullPolicy: corev1.PullPolicy(cr.Spec.ImagePullPolicy),
							Command: []string{
								"java",
							},
							Args: []string{
								"-jar",
								"boot.jar",
								"--obr",
								"file:galasa.obr",
								"--trace",
								"--metricserver",
								"--bootstrap",
								"$(BOOTSTRAP_URI)",
								"--trace",
							},
							Env: []corev1.EnvVar{
								{
									Name: "BOOTSTRAP_URI",
									ValueFrom: &corev1.EnvVarSource{
										ConfigMapKeyRef: &corev1.ConfigMapKeySelector{
											LocalObjectReference: corev1.LocalObjectReference{
												Name: "config",
											},
											Key: "bootstrap",
										},
									},
								},
								{
									Name: "NAMESPACE",
									ValueFrom: &corev1.EnvVarSource{
										FieldRef: &corev1.ObjectFieldSelector{
											FieldPath: "metadata.namespace",
										},
									},
								},
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
							},
							LivenessProbe: &corev1.Probe{
								InitialDelaySeconds: 60,
								PeriodSeconds:       60,
								Handler: corev1.Handler{
									HTTPGet: &corev1.HTTPGetAction{
										Path: "/",
										Port: intstr.FromInt(9011),
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
