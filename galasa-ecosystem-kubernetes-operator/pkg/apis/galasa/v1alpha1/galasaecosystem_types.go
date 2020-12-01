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
	DockerRegistry   string               `json:"dockerRegistry"`
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
	// +kubebuilder:default=1
	PrometheusReplicas *int32 `json:"prometheusReplicas,omitempty"`
	// +kubebuilder:default=1
	GrafanaReplicas *int32 `json:"grafanaReplicas,omitempty"`
	// +kubebuilder:default="200Mi"
	PrometheusStorage string `json:"prometheusStorage"`
	// +kubebuilder:default="200Mi"
	GrafanaStorage string            `json:"grafanaStorage"`
	NodeSelector   map[string]string `json:"nodeSelector,omitempty"`
}

type Simbank struct {
	Resources corev1.ResourceRequirements `json:"resources,omitempty"`
	// +kubebuilder:default=1
	Replicas     *int32            `json:"replicas,omitempty"`
	NodeSelector map[string]string `json:"nodeSelector,omitempty"`
}

type EngineController struct {
	Resources corev1.ResourceRequirements `json:"resources,omitempty"`
	// +kubebuilder:default=1
	Replicas     *int32            `json:"replicas,omitempty"`
	NodeSelector map[string]string `json:"nodeSelector,omitempty"`
}

type ResourceMonitor struct {
	Resources corev1.ResourceRequirements `json:"resources,omitempty"`
	// +kubebuilder:default=1
	Replicas     *int32            `json:"replicas,omitempty"`
	NodeSelector map[string]string `json:"nodeSelector,omitempty"`
}

type ApiServer struct {
	Resources corev1.ResourceRequirements `json:"resources,omitempty"`
	// +kubebuilder:default=1
	Replicas     *int32            `json:"replicas,omitempty"`
	NodeSelector map[string]string `json:"nodeSelector,omitempty"`
	// +kubebuilder:default="200Mi"
	Storage string `json:"storage"`
}

//Config for the RasSpec
type RasSpec struct {
	Resources corev1.ResourceRequirements `json:"resources,omitempty"`
	// +kubebuilder:default=1
	Replicas     *int32            `json:"replicas,omitempty"`
	NodeSelector map[string]string `json:"nodeSelector,omitempty"`
	// +kubebuilder:default="1Gi"
	Storage string `json:"storage"`
}

// PropertyStoreCluster spec for the CPS cluster
type PropertyStoreCluster struct {
	Resources corev1.ResourceRequirements `json:"resources,omitempty"`
	// +kubebuilder:default=1
	PropertyClusterSize int32             `json:"clusterSize,omitempty"`
	InitProps           map[string]string `json:"InitProps,omitempty"`
	NodeSelector        map[string]string `json:"nodeSelector,omitempty"`
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
