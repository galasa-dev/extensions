package ras

import (
	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/apis/galasa/v1alpha1"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
)

type RAS struct {
	InternalService *corev1.Service
	ExposedService  *corev1.Service
	StatefulSet     *appsv1.StatefulSet
	// Ingress         *v1beta1.Ingress
}

func New(cr *galasav1alpha1.GalasaEcosystem) *RAS {
	return &RAS{
		InternalService: generateInternalService(cr),
		ExposedService:  generateExposedService(cr),
		StatefulSet:     generateStatefulSet(cr),
	}
}

func generateInternalService(cr *galasav1alpha1.GalasaEcosystem) *corev1.Service {
	return &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-ras-internal-service",
			Namespace: cr.Namespace,
			Labels: map[string]string{
				"app": cr.Name + "-ras",
			},
		},
		Spec: corev1.ServiceSpec{
			Ports: []corev1.ServicePort{
				{
					Name:       "couchdbport",
					TargetPort: intstr.FromInt(5984),
					Port:       5984,
				},
			},
			Selector: map[string]string{
				"app": cr.Name + "-ras",
			},
		},
	}
}

func generateExposedService(cr *galasav1alpha1.GalasaEcosystem) *corev1.Service {
	return &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-ras-external-service",
			Namespace: cr.Namespace,
		},
		Spec: corev1.ServiceSpec{
			Type: corev1.ServiceType(corev1.ServiceTypeNodePort),
			Ports: []corev1.ServicePort{
				{
					Name:       "couchdbport",
					TargetPort: intstr.FromInt(5984),
					Port:       5984,
				},
			},
			Selector: map[string]string{
				"app": cr.Name + "-ras",
			},
		},
	}
}

func generateStatefulSet(cr *galasav1alpha1.GalasaEcosystem) *appsv1.StatefulSet {
	trueBool := true
	return &appsv1.StatefulSet{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-ras",
			Namespace: cr.Namespace,
			Labels: map[string]string{
				"app": cr.Name + "-ras",
			},
		},
		Spec: appsv1.StatefulSetSpec{
			ServiceName: cr.Name + "-ras-internal-service",
			Replicas:    cr.Spec.RasSpec.Replicas,
			Selector: &metav1.LabelSelector{
				MatchLabels: map[string]string{
					"app": cr.Name + "-ras",
				},
			},
			Template: corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: cr.Name + "-ras",
					Labels: map[string]string{
						"app": cr.Name + "-ras",
					},
				},
				Spec: corev1.PodSpec{
					NodeSelector: cr.Spec.RasSpec.NodeSelector,
					Containers: []corev1.Container{
						{
							Name:            "couchdb",
							Image:           "couchdb:2.3.1",
							ImagePullPolicy: corev1.PullPolicy(cr.Spec.ImagePullPolicy),
							Ports: []corev1.ContainerPort{
								{
									Name:          "couchdbport",
									ContainerPort: 5984,
								},
							},
							LivenessProbe: &corev1.Probe{
								InitialDelaySeconds: 60,
								PeriodSeconds:       60,
								Handler: corev1.Handler{
									HTTPGet: &corev1.HTTPGetAction{
										Path: "/",
										Port: intstr.FromInt(5984),
									},
								},
							},
							VolumeMounts: []corev1.VolumeMount{
								{
									MountPath: "/opt/couchdb/data",
									Name:      "data-disk",
								},
							},
						},
					},
					Volumes: []corev1.Volume{
						corev1.Volume{
							Name: "data-disk",
							VolumeSource: corev1.VolumeSource{
								PersistentVolumeClaim: &corev1.PersistentVolumeClaimVolumeSource{
									ClaimName: cr.Name + "-ras-pvc",
								},
							},
						},
					},
				},
			},
			VolumeClaimTemplates: []corev1.PersistentVolumeClaim{
				{
					ObjectMeta: metav1.ObjectMeta{
						Name: "data-disk",
						OwnerReferences: []metav1.OwnerReference{
							{
								APIVersion:         cr.APIVersion,
								Kind:               cr.Kind,
								Name:               cr.Name,
								Controller:         &trueBool,
								BlockOwnerDeletion: &trueBool,
								UID:                cr.UID,
							},
						},
					},
					Spec: corev1.PersistentVolumeClaimSpec{
						AccessModes: []corev1.PersistentVolumeAccessMode{
							corev1.PersistentVolumeAccessMode("ReadWriteOnce"),
						},
						StorageClassName: cr.Spec.StorageClassName,
						Resources: corev1.ResourceRequirements{
							Requests: corev1.ResourceList{
								corev1.ResourceName(corev1.ResourceStorage): resource.MustParse(cr.Spec.RasSpec.Storage),
							},
						},
					},
				},
			},
		},
	}
}
