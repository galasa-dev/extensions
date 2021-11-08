package engines

import (
	"strconv"
	"testing"

	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/apis/galasa/v1alpha1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

var replicas int32 = 3
var instance = &galasav1alpha1.GalasaEcosystem{
	ObjectMeta: v1.ObjectMeta{
		Name:      "test-ecosystem",
		Namespace: "test-namespace",
	},
	Spec: galasav1alpha1.GalasaEcosystemSpec{
		EngineController: galasav1alpha1.EngineController{
			Replicas:     &replicas,
			NodeSelector: map[string]string{"Test": "TestNode"},
		},
		EngineResmon: galasav1alpha1.ResourceMonitor{
			Replicas:     &replicas,
			NodeSelector: map[string]string{"Test": "TestNode"},
		},
		GalasaVersion: "1.0.0",
	},
	Status: galasav1alpha1.GalasaEcosystemStatus{
		BootstrapURL: "http://test-api-service:8080/bootstrap",
	},
}

var apiService = &corev1.Service{
	ObjectMeta: metav1.ObjectMeta{
		Name: "test-api-service",
	},
}

func TestControllerServiceForm(t *testing.T) {
	service := generateControllerInternalService(instance)
	if service.Name != "test-ecosystem-engine-controller-internal-service" {
		t.Error("Service name not generated correctly:" + service.Name)
	}
	if service.Namespace != "test-namespace" {
		t.Error("Service namespace incorrect:" + service.Namespace)
	}
	if len(service.Spec.Ports) != 3 {
		t.Error("Not enought ports" + strconv.Itoa(len(service.Spec.Ports)))
	}
	if service.Spec.Selector["app"] != "test-ecosystem-engine-controller" {
		t.Error("App selector incorrect: app=" + service.Spec.Selector["app"])
	}
}

func TestControllerConfigMapForm(t *testing.T) {
	confMap := generateControllerConfigMap(instance)
	if confMap.Data["bootstrap"] != "http://test-api-service:8080/bootstrap" {
		t.Error("Config map, malformed API service endpoint:" + confMap.Data["bootstrap"])
	}
	if confMap.Data["engine_image"] != instance.Spec.EngineController.ControllerImageName+":"+instance.Spec.GalasaVersion {
		t.Error("Config map, malformed API service endpoint:" + confMap.Data["bootstrap"])
	}
}

func TestControllerDeploymentForm(t *testing.T) {
	deployment := generateControllerDeployment(instance)
	if deployment.Spec.Template.Name != "test-ecosystem-engine-controller" {
		t.Error("Pod Name incorrect: " + deployment.Spec.Template.Name)
	}
	if deployment.Spec.Template.Labels["app"] != "test-ecosystem-engine-controller" {
		t.Error("Pod Labels incorrect: " + deployment.Spec.Template.Labels["app"])
	}
	containers := deployment.Spec.Template.Spec.Containers
	for i, arg := range containers[0].Args {
		if arg == "--k8scontroller" {
			break
		}
		if i == len(containers[0].Args)-1 {
			t.Error("Controller should have the flag --k8scontroller")
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

func TestNewController(t *testing.T) {
	controller := NewController(instance)
	if controller.ConfigMap == nil {
		t.Error("Config Map not created")
	}
	if controller.Deployment == nil {
		t.Error("Deployment not created")
	}
	if controller.InternalService == nil {
		t.Error("InternalService not created")
	}
}
