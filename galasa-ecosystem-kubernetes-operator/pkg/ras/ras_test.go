package ras

import (
	"strconv"
	"testing"

	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/apis/galasa/v1alpha1"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

var replicas int32 = 1
var scName = "test-storage"
var instance = &galasav1alpha1.GalasaEcosystem{
	ObjectMeta: v1.ObjectMeta{
		Name:      "test-ecosystem",
		Namespace: "test-namespace",
	},
	Spec: galasav1alpha1.GalasaEcosystemSpec{
		StorageClassName: &scName,
		RasSpec: galasav1alpha1.RasSpec{
			Replicas:     &replicas,
			Storage:      "200Mi",
			NodeSelector: map[string]string{"Node": "NodeName"},
		},
	},
}

func TestNewGrafana(t *testing.T) {
	ras := New(instance)
	if ras.ExposedService == nil {
		t.Error("ExposedService not created")
	}
	if ras.StatefulSet == nil {
		t.Error("Deployment not created")
	}
	if ras.InternalService == nil {
		t.Error("InternalService not created")
	}
}

func TestExposedServiceForm(t *testing.T) {
	service := generateExposedService(instance)
	if service.Name != "test-ecosystem-ras-external-service" {
		t.Error("Service name not generated correctly: " + service.Name)
	}
	if service.Namespace != "test-namespace" {
		t.Error("Service namespace incorrect:" + service.Namespace)
	}
	if len(service.Spec.Ports) != 1 {
		t.Error("Not enought ports: " + strconv.Itoa(len(service.Spec.Ports)))
	}
	if service.Spec.Selector["app"] != "test-ecosystem-ras" {
		t.Error("App selector incorrect: app=" + service.Spec.Selector["app"])
	}
}
func TestInternalServiceForm(t *testing.T) {
	service := generateInternalService(instance)
	if service.Name != "test-ecosystem-ras-internal-service" {
		t.Error("Service name not generated correctly: " + service.Name)
	}
	if service.Namespace != "test-namespace" {
		t.Error("Service namespace incorrect:" + service.Namespace)
	}
	if len(service.Spec.Ports) != 1 {
		t.Error("Not enought ports: " + strconv.Itoa(len(service.Spec.Ports)))
	}
	if service.Spec.Selector["app"] != "test-ecosystem-ras" {
		t.Error("App selector incorrect: app=" + service.Spec.Selector["app"])
	}
}

func TestStatfulSetForm(t *testing.T) {
	ras := generateStatefulSet(instance)
	if ras.Name != "test-ecosystem-ras" {
		t.Error("Cluster name not generated correctly:" + ras.Name)
	}
	if ras.Namespace != "test-namespace" {
		t.Error("Cluster namespace incorrect:" + ras.Namespace)
	}
	if ras.Labels["app"] != "test-ecosystem-ras" {
		t.Error("Labels for the ras incorrect:" + ras.Namespace)
	}
	if *ras.Spec.Replicas != 1 {
		t.Error("Cluster size incorrect:" + string(*ras.Spec.Replicas))
	}
}
