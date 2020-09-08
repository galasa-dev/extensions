package engines

import (
	"strconv"
	"testing"
)

func TestResmonInternalServiceForm(t *testing.T) {
	service := generateResmonInternalService(instance)
	if service.Name != "test-ecosystem-resource-monitor-internal-service" {
		t.Error("Service name not generated correctly: " + service.Name)
	}
	if service.Namespace != "test-namespace" {
		t.Error("Service namespace incorrect:" + service.Namespace)
	}
	if len(service.Spec.Ports) != 2 {
		t.Error("Not enought ports" + strconv.Itoa(len(service.Spec.Ports)))
	}
	if service.Spec.Selector["app"] != "test-ecosystem-resource-monitor" {
		t.Error("App selector incorrect: app=" + service.Spec.Selector["app"])
	}
}

func TestResmonExposedServiceForm(t *testing.T) {
	exposedService := generateResmonExposedService(instance)
	if exposedService.Name != "test-ecosystem-resource-monitor-external-service" {
		t.Error("Service name not generated correctly: " + exposedService.Name)
	}
	if exposedService.Namespace != "test-namespace" {
		t.Error("Service namespace incorrect:" + exposedService.Namespace)
	}
	if len(exposedService.Spec.Ports) != 2 {
		t.Error("Not enought ports" + strconv.Itoa(len(exposedService.Spec.Ports)))
	}
	if exposedService.Spec.Selector["app"] != "test-ecosystem-resource-monitor" {
		t.Error("App selector incorrect: app=" + exposedService.Spec.Selector["app"])
	}

}

func TestResmonDeploymentForm(t *testing.T) {
	deployment := generateResmonDeployment(instance)
	if deployment.Spec.Template.Name != "test-ecosystem-resource-monitor" {
		t.Error("Pod Name incorrect: " + deployment.Spec.Template.Name)
	}
	if deployment.Spec.Template.Labels["app"] != "test-ecosystem-resource-monitor" {
		t.Error("Pod Labels incorrect: " + deployment.Spec.Template.Labels["app"])
	}
	containers := deployment.Spec.Template.Spec.Containers
	for i, arg := range containers[0].Args {
		if arg == "--resourcemanagement" {
			break
		}
		if i == len(containers[0].Args)-1 {
			t.Error("Controller should have the flag --resourcemanagement")
		}
	}
	if containers[0].Env[0].Name != "BOOTSTRAP_URI" {
		t.Error("Container requires the ENV BOOTSTRAP_URI to be set")
	}
	if containers[0].Env[1].Name != "NAMESPACE" {
		t.Error("Container requires the ENV NAMESPACE to be set")
	}

	if len(containers[0].Ports) != 2 {
		t.Error("Not enough ports: " + strconv.Itoa(len(containers[0].Ports)))
	}

}

func TestNewResmon(t *testing.T) {
	resmon := NewResmon(instance)
	if resmon.ExposedService == nil {
		t.Error("ExposedService not created")
	}
	if resmon.Deployment == nil {
		t.Error("Deployment not created")
	}
	if resmon.InternalService == nil {
		t.Error("InternalService not created")
	}
}
