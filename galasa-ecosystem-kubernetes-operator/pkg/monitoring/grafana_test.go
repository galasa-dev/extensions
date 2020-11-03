package monitoring

import (
	"strconv"
	"testing"

	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/apis/galasa/v1alpha1"
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
		Monitoring: galasav1alpha1.Monitoring{
			GrafanaReplicas:    &replicas,
			MetricsReplicas:    &replicas,
			PrometheusReplicas: &replicas,
			GrafanaStorage:     "100m",
			PrometheusStorage:  "100m",
		},
		IngressClass:     "test-nginx",
		IngressHostname:  "https://test.ingress.galasa.dev",
		StorageClassName: &scName,
	},
}

func TestNewGrafana(t *testing.T) {
	grafana := NewGrafana(instance)
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
	if grafana.ProvisioningConfigMap == nil {
		t.Error("ProvisioningConfigMap not created")
	}
	if grafana.AutoDashboardConfigMap == nil {
		t.Error("AutoDashboardConfigMap not created")
	}
	if grafana.DashboardConfigMap == nil {
		t.Error("DashboardConfigMap not created")
	}
	if grafana.Ingress == nil {
		t.Error("Ingress not created")
	}
}

func TestGrafanaIngressForm(t *testing.T) {
	ingress := generateGrafanaIngress(instance)
	if ingress.Name != "test-ecosystem-grafana-ingress" {
		t.Error("Service name not generated correctly: " + ingress.Name)
	}
	if ingress.Namespace != "test-namespace" {
		t.Error("Service namespace incorrect:" + ingress.Namespace)
	}
	if ingress.Annotations["kubernetes.io/ingress.class"] != "test-nginx" {
		t.Error("Ingress Class name incorrect: " + *ingress.Spec.IngressClassName)
	}
	if ingress.Spec.Rules[0].IngressRuleValue.HTTP.Paths[0].Path != "/test-ecosystem-grafana" {
		t.Error("Ingress Class path incorrectly formed: " + ingress.Spec.Rules[0].IngressRuleValue.HTTP.Paths[0].Path)
	}
	if ingress.Spec.Rules[0].IngressRuleValue.HTTP.Paths[0].Backend.ServiceName != "test-ecosystem-grafana-external-service" {
		t.Error("Ingress Class Service name incorrect: " + ingress.Spec.Rules[0].IngressRuleValue.HTTP.Paths[0].Backend.ServiceName)
	}
	if ingress.Spec.Rules[0].IngressRuleValue.HTTP.Paths[0].Backend.ServicePort != intstr.FromInt(3000) {
		t.Error("Ingress Class Service port incorrect")
	}
}

func TestGrafanaAutoDashboardConfigMapForm(t *testing.T) {
	autoDashConf := generateAutoDashboardConfigMap(instance)
	if autoDashConf.Name != "grafana-auto-dashboard" {
		t.Error("Incorrect auto dashboard conf name")
	}
	if autoDashConf.Labels["app"] != "test-ecosystem-grafana" {
		t.Error("Incorrect labels")
	}
	if autoDashConf.Data["dashboard.json"] == "" {
		t.Error("Dashboatf.json not populated")
	}
}
func TestGrafanaDashboardConfigMapForm(t *testing.T) {
	dashboardConf := generateDashboardConfigMap(instance)
	if dashboardConf.Name != "grafana-dashboard" {
		t.Error("Incorrect dashboards conf name")
	}
	if dashboardConf.Labels["app"] != "test-ecosystem-grafana" {
		t.Error("Incorrect labels")
	}
	if dashboardConf.Data["dashboards.yaml"] == "" {
		t.Error("Dashboards.yaml not populated")
	}
}
func TestGrafanaProvisioningConfigMapForm(t *testing.T) {
	provisionConf := generateProvisioningConfigMap(instance)
	if provisionConf.Name != "grafana-provisioning" {
		t.Error("Incorrect proviosing conf name")
	}
	if provisionConf.Labels["app"] != "test-ecosystem-grafana" {
		t.Error("Incorrect labels")
	}
	if provisionConf.Data["prometheus.yaml"] == "" {
		t.Error("prometheus.yaml not populated")
	}
}

func TestGrafanaConfigMapForm(t *testing.T) {
	grafanaConf := generateGrafanaConfig(instance)
	if grafanaConf.Name != "grafana-config" {
		t.Error("Incorrect auto dashboard conf name")
	}
	if grafanaConf.Labels["app"] != "test-ecosystem-grafana" {
		t.Error("Incorrect labels")
	}
	if grafanaConf.Data["grafana.ini"] == "" {
		t.Error("grafana.ini not populated")
	}
}
func TestGrafanaPersistentVolumeClaimForm(t *testing.T) {
	pvc := generateGrafanaPVC(instance)
	if pvc.Name != "test-ecosystem-grafana-pvc" {
		t.Error("Non standard PVC name: " + pvc.Name)
	}
	if *pvc.Spec.StorageClassName != scName {
		t.Error("Storage class name incorrect: " + *pvc.Spec.StorageClassName)
	}
}
func TestGrafanaDeploymentForm(t *testing.T) {
	deployment := generateGrafanaDeployment(instance)
	if deployment.Name != "test-ecosystem-grafana" {
		t.Error("Deployment name incorrect: " + deployment.Name)
	}
	if deployment.Namespace != "test-namespace" {
		t.Error("Deployment namespace incorrect: " + deployment.Namespace)
	}
	if deployment.Spec.Replicas != &replicas {
		t.Error("Number of replicas not the same as requested")
	}
	if deployment.Spec.Template.Name != "test-ecosystem-grafana" {
		t.Error("Pod Template name incorrect: " + deployment.Spec.Template.Name)
	}
	if deployment.Spec.Template.Labels["app"] != "test-ecosystem-grafana" {
		t.Error("Pod labels incorrect: app=" + deployment.Spec.Template.Labels["app"])
	}
	if deployment.Spec.Template.Spec.InitContainers[0].VolumeMounts[0].MountPath != "/var/lib/grafana" {
		t.Error("MountPath incorrect, init container wont chown the correct dir")
	}
	if deployment.Spec.Template.Spec.Containers[0].Ports[0].ContainerPort != 3000 {
		t.Error("Wrong port specificed")
	}
	if len(deployment.Spec.Template.Spec.Containers[0].VolumeMounts) != 5 {
		t.Error("Number of mounts incorrect")
	}
	if len(deployment.Spec.Template.Spec.Volumes) != 5 {
		t.Error("Number of volumes incorrect")
	}
}
func TestGrafanaExposedServiceForm(t *testing.T) {
	service := generateGrafanaInternalService(instance)
	if service.Name != "test-ecosystem-grafana-internal-service" {
		t.Error("Service name not generated correctly: " + service.Name)
	}
	if service.Namespace != "test-namespace" {
		t.Error("Service namespace incorrect:" + service.Namespace)
	}
	if len(service.Spec.Ports) != 1 {
		t.Error("Not enought ports: " + strconv.Itoa(len(service.Spec.Ports)))
	}
	if service.Spec.Selector["app"] != "test-ecosystem-grafana" {
		t.Error("App selector incorrect: app=" + service.Spec.Selector["app"])
	}
}
func TestGrafanaInternalServiceForm(t *testing.T) {
	service := generateGrafanaExposedService(instance)
	if service.Name != "test-ecosystem-grafana-external-service" {
		t.Error("Service name not generated correctly: " + service.Name)
	}
	if service.Namespace != "test-namespace" {
		t.Error("Service namespace incorrect:" + service.Namespace)
	}
	if len(service.Spec.Ports) != 1 {
		t.Error("Not enought ports: " + strconv.Itoa(len(service.Spec.Ports)))
	}
	if service.Spec.Selector["app"] != "test-ecosystem-grafana" {
		t.Error("App selector incorrect: app=" + service.Spec.Selector["app"])
	}
}
