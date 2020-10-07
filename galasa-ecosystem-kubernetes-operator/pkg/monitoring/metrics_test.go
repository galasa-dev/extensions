package monitoring

import (
	"strconv"
	"testing"
)

func TestNewMetrics(t *testing.T) {
	metrics := NewMetrics(instance)
	if metrics.ExposedService == nil {
		t.Error("ExposedService not created")
	}
	if metrics.Deployment == nil {
		t.Error("Deployment not created")
	}
	if metrics.InternalService == nil {
		t.Error("InternalService not created")
	}
}

func TestMetricsExposedServiceForm(t *testing.T) {
	service := generateMetricsInternalService(instance)
	if service.Name != "test-ecosystem-metrics-internal-service" {
		t.Error("Service name not generated correctly: " + service.Name)
	}
	if service.Namespace != "test-namespace" {
		t.Error("Service namespace incorrect:" + service.Namespace)
	}
	if len(service.Spec.Ports) != 2 {
		t.Error("Not enough ports: " + strconv.Itoa(len(service.Spec.Ports)))
	}
	if service.Spec.Selector["app"] != "test-ecosystem-metrics" {
		t.Error("App selector incorrect: app=" + service.Spec.Selector["app"])
	}
}
func TestMetricsInternalServiceForm(t *testing.T) {
	service := generateMetricsExposedService(instance)
	if service.Name != "test-ecosystem-metrics-external-service" {
		t.Error("Service name not generated correctly: " + service.Name)
	}
	if service.Namespace != "test-namespace" {
		t.Error("Service namespace incorrect:" + service.Namespace)
	}
	if len(service.Spec.Ports) != 1 {
		t.Error("Not enought ports: " + strconv.Itoa(len(service.Spec.Ports)))
	}
	if service.Spec.Selector["app"] != "test-ecosystem-metrics" {
		t.Error("App selector incorrect: app=" + service.Spec.Selector["app"])
	}
}

func TestMetricsDeploymentForm(t *testing.T) {
	deployment := generateMetricsDeployment(instance)
	if deployment.Spec.Template.Name != "test-ecosystem-metrics" {
		t.Error("Pod Name incorrect: " + deployment.Spec.Template.Name)
	}
	if deployment.Spec.Template.Labels["app"] != "test-ecosystem-metrics" {
		t.Error("Pod Labels incorrect: " + deployment.Spec.Template.Labels["app"])
	}
	containers := deployment.Spec.Template.Spec.Containers
	for i, arg := range containers[0].Args {
		if arg == "--metricserver" {
			break
		}
		if i == len(containers[0].Args)-1 {
			t.Error("Controller should have the flag --metricserver")
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
