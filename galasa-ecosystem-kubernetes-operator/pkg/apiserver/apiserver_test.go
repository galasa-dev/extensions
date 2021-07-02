package apiserver

import (
	"strconv"
	"testing"

	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/apis/galasa/v1alpha1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
)

var replicas int32 = 1
var scName = "test-storage"
var instance = &galasav1alpha1.GalasaEcosystem{
	ObjectMeta: v1.ObjectMeta{
		Name:      "test-ecosystem",
		Namespace: "test-namespace",
	},
	Spec: galasav1alpha1.GalasaEcosystemSpec{
		APIServer: galasav1alpha1.ApiServer{
			Replicas: &replicas,
			Storage:  "100m",
		},
		IngressClass:     "test-nginx",
		IngressHostname:  "https://test.ingress.galasa.dev",
		StorageClassName: &scName,
	},
}

var cpsService = &corev1.Service{
	ObjectMeta: metav1.ObjectMeta{
		Name: "test-cps-service",
	},
}

func TestNewGrafana(t *testing.T) {
	api := New(instance)
	if api.ExposedService == nil {
		t.Error("ExposedService not created")
	}
	if api.Deployment == nil {
		t.Error("Deployment not created")
	}
	if api.InternalService == nil {
		t.Error("InternalService not created")
	}
	if api.Ingress == nil {
		t.Error("Ingress not created")
	}
}

func TestExposedServiceForm(t *testing.T) {
	service := generateInternalService(instance)
	if service.Name != "test-ecosystem-api-internal-service" {
		t.Error("Service name not generated correctly: " + service.Name)
	}
	if service.Namespace != "test-namespace" {
		t.Error("Service namespace incorrect:" + service.Namespace)
	}
	if len(service.Spec.Ports) != 3 {
		t.Error("Not enought ports: " + strconv.Itoa(len(service.Spec.Ports)))
	}
	if service.Spec.Selector["app"] != "test-ecosystem-apiserver" {
		t.Error("App selector incorrect: app=" + service.Spec.Selector["app"])
	}
}
func TestInternalServiceForm(t *testing.T) {
	service := generateExposedService(instance)
	if service.Name != "test-ecosystem-api-external-service" {
		t.Error("Service name not generated correctly: " + service.Name)
	}
	if service.Namespace != "test-namespace" {
		t.Error("Service namespace incorrect:" + service.Namespace)
	}
	if len(service.Spec.Ports) != 1 {
		t.Error("Not enought ports: " + strconv.Itoa(len(service.Spec.Ports)))
	}
	if service.Spec.Selector["app"] != "test-ecosystem-apiserver" {
		t.Error("App selector incorrect: app=" + service.Spec.Selector["app"])
	}
}

func TestDeploymentForm(t *testing.T) {
	deployment := generateDeployment(instance)
	if deployment.Spec.Template.Name != "test-ecosystem-apiserver" {
		t.Error("Pod Name incorrect: " + deployment.Spec.Template.Name)
	}
	if deployment.Spec.Template.Labels["app"] != "test-ecosystem-apiserver" {
		t.Error("Pod Labels incorrect: " + deployment.Spec.Template.Labels["app"])
	}
	containers := deployment.Spec.Template.Spec.Containers
	for i, arg := range containers[0].Args {
		if arg == "--bootstrap" {
			break
		}
		if i == len(containers[0].Args)-1 {
			t.Error("Controller should have the flag --bootstrap")
		}
	}
	if len(containers[0].Ports) != 3 {
		t.Error("Not enough ports: " + strconv.Itoa(len(containers[0].Ports)))
	}

}

func TestBootstrapConfigMapForm(t *testing.T) {
	apiConf := generateBootstrapConfigMap(instance)
	if apiConf.Name != "bootstrap-file" {
		t.Error("Incorrect auto dashboard conf name")
	}
	if apiConf.Data["bootstrap.properties"] == "" {
		t.Error("bootstrap.properties not populated")
	}
}

func TestCatalogConfigMapForm(t *testing.T) {
	testCatalogConf := generateTestCatalogConfigMap(instance)
	if testCatalogConf.Name != "testcatalog-file" {
		t.Error("Incorrect auto dashboard conf name")
	}
	if testCatalogConf.Data["dev.galasa.testcatalog.cfg"] == "" {
		t.Error("dev.galasa.testcatalog.cfg not populated")
	}
}

func TestPersistentVolumeClaimForm(t *testing.T) {
	pvc := generatePersistentVolumeClaim(instance)
	if pvc.Name != "test-ecosystem-apiserver-pvc" {
		t.Error("Non standard PVC name: " + pvc.Name)
	}
	if *pvc.Spec.StorageClassName != scName {
		t.Error("Storage class name incorrect: " + *pvc.Spec.StorageClassName)
	}
}

func TestIngressForm(t *testing.T) {
	ingress := generateIngress(instance)
	if ingress.Name != "test-ecosystem-apiserver-ingress" {
		t.Error("Service name not generated correctly: " + ingress.Name)
	}
	if ingress.Namespace != "test-namespace" {
		t.Error("Service namespace incorrect:" + ingress.Namespace)
	}
	if ingress.Annotations["kubernetes.io/ingress.class"] != "test-nginx" {
		t.Error("Ingress Class name incorrect: " + *ingress.Spec.IngressClassName)
	}
	if ingress.Spec.Rules[0].IngressRuleValue.HTTP.Paths[0].Path != "/bootstrap" {
		t.Error("Ingress Class path incorrectly formed: " + ingress.Spec.Rules[0].IngressRuleValue.HTTP.Paths[0].Path)
	}
	if ingress.Spec.Rules[0].IngressRuleValue.HTTP.Paths[0].Backend.ServiceName != "test-ecosystem-apiserver-external-service" {
		t.Error("Ingress Class Service name incorrect: " + ingress.Spec.Rules[0].IngressRuleValue.HTTP.Paths[0].Backend.ServiceName)
	}
	if ingress.Spec.Rules[0].IngressRuleValue.HTTP.Paths[0].Backend.ServicePort != intstr.FromInt(8080) {
		t.Error("Ingress Class Service port incorrect")
	}
}
