package engines

import (
	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-operator/pkg/apis/galasa/v1alpha1"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
)

type Resmon struct {
	InternalService *corev1.Service
	ExposedService  *corev1.Service
	Deployment      *appsv1.Deployment
}

func NewResmon(cr *galasav1alpha1.GalasaEcosystem) *Resmon {
	return &Resmon{
		ExposedService:  generateResmonExposedService(cr),
		InternalService: generateResmonInternalService(cr),
		Deployment:      generateResmonDeployment(cr),
	}
}

func generateResmonInternalService(cr *galasav1alpha1.GalasaEcosystem) *corev1.Service {
	return &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-resource-monitor-internal-service",
			Namespace: cr.Namespace,
			Labels: map[string]string{
				"app": cr.Name + "-resource-monitor",
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
				"app": cr.Name + "-resource-monitor",
			},
		},
	}
}

func generateResmonExposedService(cr *galasav1alpha1.GalasaEcosystem) *corev1.Service {
	return &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-resource-monitor-external-service",
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
				{
					Name:       "health",
					TargetPort: intstr.FromInt(9011),
					Port:       9011,
				},
			},
			Selector: map[string]string{
				"app": cr.Name + "-resource-monitor",
			},
		},
	}
}

func generateResmonDeployment(cr *galasav1alpha1.GalasaEcosystem) *appsv1.Deployment {
	return &appsv1.Deployment{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-resource-monitor",
			Namespace: cr.Namespace,
			Labels: map[string]string{
				"app": cr.Name + "-resource-monitor",
			},
		},
		Spec: appsv1.DeploymentSpec{
			Replicas: cr.Spec.EngineResmon.Replicas,
			Selector: &metav1.LabelSelector{
				MatchLabels: map[string]string{
					"app": cr.Name + "-resource-monitor",
				},
			},
			Template: corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: cr.Name + "-resource-monitor",
					Labels: map[string]string{
						"app": cr.Name + "-resource-monitor",
					},
				},
				Spec: corev1.PodSpec{
					NodeSelector: cr.Spec.EngineResmon.NodeSelector,
					Containers: []corev1.Container{
						{
							Name:            "resource-monitor",
							Image:           cr.Spec.DockerRegistry + "/galasa-boot-embedded-amd64:" + cr.Spec.GalasaVersion,
							ImagePullPolicy: corev1.PullAlways,
							Command: []string{
								"java",
							},
							Args: []string{
								"-jar",
								"boot.jar",
								"--obr",
								"file:galasa.obr",
								"--trace",
								"--resourcemanagement",
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
