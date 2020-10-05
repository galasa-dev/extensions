package simbank

import (
	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-operator/pkg/apis/galasa/v1alpha1"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
)

type Simbank struct {
	ExposedService *corev1.Service
	Deployment     *appsv1.Deployment
}

func New(cr *galasav1alpha1.GalasaEcosystem) *Simbank {
	return &Simbank{
		ExposedService: generateExposedService(cr),
		Deployment:     generateDeployment(cr),
	}
}

func generateDeployment(cr *galasav1alpha1.GalasaEcosystem) *appsv1.Deployment {
	return &appsv1.Deployment{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-simbank",
			Namespace: cr.Namespace,
			Labels: map[string]string{
				"app": cr.Name + "-simbank",
			},
		},
		Spec: appsv1.DeploymentSpec{
			Replicas: cr.Spec.Simbank.Replicas,
			Selector: &metav1.LabelSelector{
				MatchLabels: map[string]string{
					"app": cr.Name + "-simbank",
				},
			},
			Template: corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: cr.Name + "-simbank",
					Labels: map[string]string{
						"app": cr.Name + "-simbank",
					},
				},
				Spec: corev1.PodSpec{
					NodeSelector: cr.Spec.Simbank.NodeSelector,
					Containers: []corev1.Container{
						{
							Name:            cr.Name + "-simbank",
							Image:           cr.Spec.DockerRegistry + "/galasa-boot-embedded-amd64:" + cr.Spec.GalasaVersion,
							ImagePullPolicy: corev1.PullAlways,
							Command: []string{
								"java",
							},
							Args: []string{
								"-jar",
								"simplatform.jar",
							},
							Ports: []corev1.ContainerPort{
								{
									Name:          "telnet",
									ContainerPort: 2023,
								},
								{
									Name:          "webservice",
									ContainerPort: 2080,
								},
								{
									Name:          "database",
									ContainerPort: 2027,
								},
								{
									Name:          "mf",
									ContainerPort: 2040,
								},
							},
						},
					},
				},
			},
		},
	}
}

func generateExposedService(cr *galasav1alpha1.GalasaEcosystem) *corev1.Service {
	return &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-simbank-external-service",
			Namespace: cr.Namespace,
		},
		Spec: corev1.ServiceSpec{
			Type: corev1.ServiceType(corev1.ServiceTypeNodePort),
			Ports: []corev1.ServicePort{
				{
					Name:       "simbank-telnet",
					TargetPort: intstr.FromInt(2023),
					Port:       2023,
				},
				{
					Name:       "simbank-webservice",
					TargetPort: intstr.FromInt(2080),
					Port:       2080,
				},
				{
					Name:       "simbank-database",
					TargetPort: intstr.FromInt(2027),
					Port:       2027,
				},
				{
					Name:       "simbank-mf",
					TargetPort: intstr.FromInt(2040),
					Port:       2040,
				},
			},
			Selector: map[string]string{
				"app": cr.Name + "-simbank",
			},
		},
	}
}
