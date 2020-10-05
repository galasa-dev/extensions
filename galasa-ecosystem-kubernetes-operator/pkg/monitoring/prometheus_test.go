package monitoring

import (
	"strconv"
	"testing"
)

func TestNewPrometheus(t *testing.T) {
	grafana := NewPrometheus(instance)
	if grafana.ExposedService == nil {
		t.Error("ExposedService not created")
	}
	if grafana.Deployment == nil {
		t.Error("Deployment not created")
	}
	if grafana.InternalService == nil {
		t.Error("InternalService not created")
	}
	if grafana.PersistentVolumeClaim == nil {
		t.Error("PersistentVolumeClaim not created")
	}
	if grafana.ConfigMap == nil {
		t.Error("ConfigMap not created")
	}
}

func TestPrometheusExposedServiceForm(t *testing.T) {
	service := generatePrometheusInternalService(instance)
	if service.Name != "test-ecosystem-prometheus-internal-service" {
		t.Error("Service name not generated correctly: " + service.Name)
	}
	if service.Namespace != "test-namespace" {
		t.Error("Service namespace incorrect:" + service.Namespace)
	}
	if len(service.Spec.Ports) != 1 {
		t.Error("Not enough ports: " + strconv.Itoa(len(service.Spec.Ports)))
	}
	if service.Spec.Selector["app"] != "test-ecosystem-prometheus" {
		t.Error("App selector incorrect: app=" + service.Spec.Selector["app"])
	}
}
func TestPrometheusInternalServiceForm(t *testing.T) {
	service := generatePrometheusExposedService(instance)
	if service.Name != "test-ecosystem-prometheus-external-service" {
		t.Error("Service name not generated correctly: " + service.Name)
	}
	if service.Namespace != "test-namespace" {
		t.Error("Service namespace incorrect:" + service.Namespace)
	}
	if len(service.Spec.Ports) != 1 {
		t.Error("Not enought ports: " + strconv.Itoa(len(service.Spec.Ports)))
	}
	if service.Spec.Selector["app"] != "test-ecosystem-prometheus" {
		t.Error("App selector incorrect: app=" + service.Spec.Selector["app"])
	}
}

func TestPrometheusPersistentVolumeClaimForm(t *testing.T) {
	pvc := generatePrometheusPVC(instance)
	if pvc.Name != "test-ecosystem-prometheus-pvc" {
		t.Error("Non standard PVC name: " + pvc.Name)
	}
	if *pvc.Spec.StorageClassName != scName {
		t.Error("Storage class name incorrect: " + *pvc.Spec.StorageClassName)
	}
}

func TestPrometheusConfigMapForm(t *testing.T) {
	grafanaConf := generatePrometheusConfigMap(instance)
	if grafanaConf.Name != "prometheus-config" {
		t.Error("Incorrect prometheus.yml conf name")
	}
	if grafanaConf.Labels["app"] != "test-ecosystem-prometheus" {
		t.Error("Incorrect labels")
	}
	if grafanaConf.Data["prometheus.yml"] == "" {
		t.Error("prometheus.yml not populated")
	}
}
