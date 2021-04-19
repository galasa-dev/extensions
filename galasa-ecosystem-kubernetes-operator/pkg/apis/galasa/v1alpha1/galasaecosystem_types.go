package v1alpha1

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// GalasaEcosystemSpec defines the desired state of GalasaEcosystem
type GalasaEcosystemSpec struct {
	Config           map[string]string `json:"config,omitempty"`
	StorageClassName *string           `json:"storageClassName,omitempty"`
	GalasaVersion    string            `json:"galasaVersion"`
	// The valid options are "Always", "Never" or "IfNotPresent"
	// +kubebuilder:default="IfNotPresent"
	ImagePullPolicy  string               `json:"imagePullPolicy,omitempty"`
	MavenRepository  string               `json:"mavenRepository"`
	ExternalHostname string               `json:"externalhostname"`
	IngressClass     string               `json:"ingressClass,omitempty"`
	IngressHostname  string               `json:"ingressHostname,omitempty"`
	Propertystore    PropertyStoreCluster `json:"propertystore"`
	APIServer        ApiServer            `json:"apiserver"`
	RasSpec          RasSpec              `json:"rasSpec"`
	EngineController EngineController     `json:"engineController"`
	EngineResmon     ResourceMonitor      `json:"engineResmon"`
	Simbank          Simbank              `json:"simbank"`
	Monitoring       Monitoring           `json:"monitoring"`
}

type Monitoring struct {
	// +kubebuilder:default=1
	MetricsReplicas *int32 `json:"metricsReplicas,omitempty"`
	// +kubebuilder:default="docker.galasa.dev/galasa-boot-embedded-amd64"
	MetricsImageName    string `json:"metricsImageName,omitempty"`
	MetricsImageVersion string `json:"metricsImageVersion,omitempty"`

	// +kubebuilder:default=1
	PrometheusReplicas *int32 `json:"prometheusReplicas,omitempty"`
	// +kubebuilder:default="prom/prometheus"
	PrometheusImageName string `json:"prometheusImageName,omitempty"`
	// +kubebuilder:default="v2.10.0"
	PrometheusImageVersion string `json:"prometheusImageVersion,omitempty"`
	// +kubebuilder:default="200Mi"
	PrometheusStorage string `json:"prometheusStorage"`

	// +kubebuilder:default=1
	GrafanaReplicas *int32 `json:"grafanaReplicas,omitempty"`
	// +kubebuilder:default="grafana/grafana"
	GrafanaImageName string `json:"grafanaImageName,omitempty"`
	// +kubebuilder:default="latest"
	GrafanaImageVersion string `json:"grafanaImageVersion,omitempty"`
	// +kubebuilder:default="200Mi"
	GrafanaStorage string `json:"grafanaStorage"`

	NodeSelector map[string]string `json:"nodeSelector,omitempty"`
}

type Simbank struct {
	Resources corev1.ResourceRequirements `json:"resources,omitempty"`
	// +kubebuilder:default=1
	Replicas *int32 `json:"replicas,omitempty"`
	// +kubebuilder:default="docker.galasa.dev/galasa-boot-embedded-amd64"
	SimbankImageName    string            `json:"simbankImageName,omitempty"`
	SimbankImageVersion string            `json:"simbankImageVersion,omitempty"`
	NodeSelector        map[string]string `json:"nodeSelector,omitempty"`
}

type EngineController struct {
	Resources corev1.ResourceRequirements `json:"resources,omitempty"`
	// +kubebuilder:default=1
	Replicas *int32 `json:"replicas,omitempty"`
	// +kubebuilder:default="docker.galasa.dev/galasa-boot-embedded-amd64"
	ControllerImageName    string            `json:"controllerImageName,omitempty"`
	ControllerImageVersion string            `json:"controllerImageVersion,omitempty"`
	NodeSelector           map[string]string `json:"nodeSelector,omitempty"`
}

type ResourceMonitor struct {
	Resources corev1.ResourceRequirements `json:"resources,omitempty"`
	// +kubebuilder:default=1
	Replicas *int32 `json:"replicas,omitempty"`
	// +kubebuilder:default="docker.galasa.dev/galasa-boot-embedded-amd64"
	ResourceMonitorImageName    string            `json:"resourceMonitorImageName,omitempty"`
	ResourceMonitorImageVersion string            `json:"resourceMonitorImageVersion,omitempty"`
	NodeSelector                map[string]string `json:"nodeSelector,omitempty"`
}

type ApiServer struct {
	Resources corev1.ResourceRequirements `json:"resources,omitempty"`
	// +kubebuilder:default=1
	Replicas *int32 `json:"replicas,omitempty"`
	// +kubebuilder:default="docker.galasa.dev/galasa-boot-embedded-amd64"
	ApiServerImageName    string            `json:"apiServerImageName,omitempty"`
	ApiServerImageVersion string            `json:"apiServerImageVersion,omitempty"`
	NodeSelector          map[string]string `json:"nodeSelector,omitempty"`
	// +kubebuilder:default="200Mi"
	Storage string `json:"storage"`
}

//Config for the RasSpec
type RasSpec struct {
	Resources corev1.ResourceRequirements `json:"resources,omitempty"`
	// +kubebuilder:default=1
	Replicas *int32 `json:"replicas,omitempty"`
	// +kubebuilder:default="couchdb"
	RasImageName string `json:"rasImageName,omitempty"`
	// +kubebuilder:default="2.3.1"
	RasImageVersion string            `json:"rasImageImageVersion,omitempty"`
	NodeSelector    map[string]string `json:"nodeSelector,omitempty"`
	// +kubebuilder:default="1Gi"
	Storage string `json:"storage"`
}

// PropertyStoreCluster spec for the CPS cluster
type PropertyStoreCluster struct {
	Resources corev1.ResourceRequirements `json:"resources,omitempty"`
	// +kubebuilder:default=1
	PropertyClusterSize int32 `json:"clusterSize,omitempty"`
	// +kubebuilder:default="quay.io/coreos/etcd"
	PropertyStoreImageName string `json:"propertyStoreImageName,omitempty"`
	// +kubebuilder:default="v3.4.3"
	PropertyStoreImageVersion string            `json:"propertyStoreImageVersion,omitempty"`
	InitProps                 map[string]string `json:"InitProps,omitempty"`
	NodeSelector              map[string]string `json:"nodeSelector,omitempty"`
	// +kubebuilder:default="1Gi"
	Storage string `json:"storage"`
}

// GalasaEcosystemStatus defines the observed state of GalasaEcosystem
type GalasaEcosystemStatus struct {
	CPSReadyReplicas              int32  `json:"CPSReadyReplicas"`
	APIReadyReplicas              int32  `json:"APIReadyReplicas"`
	RASReadyReplicas              int32  `json:"RASReadyReplicas"`
	BootstrapURL                  string `json:"BootstrapURL"`
	GrafanaURL                    string `json:"GrafanaURL"`
	EngineControllerReadyReplicas int32  `json:"EngineControllerReadyReplicas"`
	ResmonReadyReplicas           int32  `json:"ResmonReadyReplicas"`
	MonitoringReadyReplicas       int32  `json:"MonitoringReadyReplicas"`
	EcosystemReady                bool   `json:"EcosystemReady"`
}

// kubebuilder:object:root=true
// kubebuilder:subresource:status
// +kubebuilder:printcolumn:JSONPath=".status.EcosystemReady",name=READY,type=boolean
// +kubebuilder:printcolumn:JSONPath=".status.BootstrapURL",name=BOOTSTRAPURL,type=string
// +kubebuilder:printcolumn:JSONPath=".status.GrafanaURL",name=GRAFANAURL,type=string
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
type GalasaEcosystem struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   GalasaEcosystemSpec   `json:"spec,omitempty"`
	Status GalasaEcosystemStatus `json:"status,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// GalasaEcosystemList contains a list of GalasaEcosystem
type GalasaEcosystemList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []GalasaEcosystem `json:"items"`
}

func init() {
	SchemeBuilder.Register(&GalasaEcosystem{}, &GalasaEcosystemList{})
}
