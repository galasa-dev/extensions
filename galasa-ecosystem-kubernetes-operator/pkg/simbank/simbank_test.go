package simbank

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
		Simbank: galasav1alpha1.Simbank{
			NodeSelector: map[string]string{},
			Replicas:     &replicas,
		},
		StorageClassName: &scName,
	},
}

func TestNewSimbank(t *testing.T) {
	grafana := New(instance)
	if grafana.ExposedService == nil {
		t.Error("ExposedService not created")
	}
	if grafana.Deployment == nil {
		t.Error("Deployment not created")
	}
}

func TestExposedServiceForm(t *testing.T) {
	service := generateExposedService(instance)
	if service.Name != "test-ecosystem-simbank-external-service" {
		t.Error("Service name not generated correctly: " + service.Name)
	}
	if service.Namespace != "test-namespace" {
		t.Error("Service namespace incorrect:" + service.Namespace)
	}
	if len(service.Spec.Ports) != 4 {
		t.Error("Not enought ports: " + strconv.Itoa(len(service.Spec.Ports)))
	}
	if service.Spec.Selector["app"] != "test-ecosystem-simbank" {
		t.Error("App selector incorrect: app=" + service.Spec.Selector["app"])
	}
}

func TestSimbankDeploymentForm(t *testing.T) {
	deployment := generateDeployment(instance)
	if deployment.Spec.Template.Name != "test-ecosystem-simbank" {
		t.Error("Pod Name incorrect: " + deployment.Spec.Template.Name)
	}
	if deployment.Spec.Template.Labels["app"] != "test-ecosystem-simbank" {
		t.Error("Pod Labels incorrect: " + deployment.Spec.Template.Labels["app"])
	}
	containers := deployment.Spec.Template.Spec.Containers
	if len(containers[0].Ports) != 4 {
		t.Error("Not enough ports: " + strconv.Itoa(len(containers[0].Ports)))
	}

}
