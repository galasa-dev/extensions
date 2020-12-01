package engines

import (
	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/apis/galasa/v1alpha1"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
)

type Controller struct {
	InternalService *corev1.Service
	Deployment      *appsv1.Deployment
	ConfigMap       *corev1.ConfigMap
}

func NewController(cr *galasav1alpha1.GalasaEcosystem, apiService *corev1.Service) *Controller {
	return &Controller{
		InternalService: generateControllerInternalService(cr),
		Deployment:      generateControllerDeployment(cr),
		ConfigMap:       generateControllerConfigMap(cr, apiService),
	}
}

func generateControllerConfigMap(cr *galasav1alpha1.GalasaEcosystem, apiService *corev1.Service) *corev1.ConfigMap {
	user := cr.Spec.Config
	config := map[string]string{
		"bootstrap":    "http://" + apiService.Name + ":8080/bootstrap",
		"max_engines":  "10",
		"engine_label": "k8s-standard-engine",
		"engine_image": cr.Spec.DockerRegistry + "/galasa-boot-embedded-amd64:" + cr.Spec.GalasaVersion,
	}
	for k, v := range user {
		config[k] = v
	}

	return &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "config",
			Namespace: cr.Namespace,
		},
		Data: config,
	}
}

func generateControllerDeployment(cr *galasav1alpha1.GalasaEcosystem) *appsv1.Deployment {
	return &appsv1.Deployment{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-engine-controller",
			Namespace: cr.Namespace,
			Labels: map[string]string{
				"app": cr.Name + "-engine-controller",
			},
		},
		Spec: appsv1.DeploymentSpec{
			Replicas: cr.Spec.EngineController.Replicas,
			Selector: &metav1.LabelSelector{
				MatchLabels: map[string]string{
					"app": cr.Name + "-engine-controller",
				},
			},
			Template: corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: cr.Name + "-engine-controller",
					Labels: map[string]string{
						"app": cr.Name + "-engine-controller",
					},
				},
				Spec: corev1.PodSpec{
					NodeSelector: cr.Spec.EngineController.NodeSelector,
					Containers: []corev1.Container{
						{
							Name:            cr.Name + "-engine-controller",
							Image:           cr.Spec.DockerRegistry + "/galasa-boot-embedded-amd64:" + cr.Spec.GalasaVersion,
							ImagePullPolicy: corev1.PullPolicy(cr.Spec.ImagePullPolicy),
							Command: []string{
								"java",
							},
							Args: []string{
								"-jar",
								"boot.jar",
								"--obr",
								"file:galasa.obr",
								"--k8scontroller",
								"--bootstrap",
								"$(BOOTSTRAP_URI)",
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
					ServiceAccountName: "galasa-ecosystem-kubernetes-operator",
				},
			},
		},
	}
}

func generateControllerInternalService(cr *galasav1alpha1.GalasaEcosystem) *corev1.Service {
	return &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-engine-controller-internal-service",
			Namespace: cr.Namespace,
			Labels: map[string]string{
				"app": cr.Name + "-engine-controller",
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
				"app": cr.Name + "-engine-controller",
			},
		},
	}
}
