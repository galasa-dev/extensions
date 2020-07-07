package v1alpha1

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// GalasaEcosystemSpec defines the desired state of GalasaEcosystem
type GalasaEcosystemSpec struct {
	GalasaVersion    string               `json:"galasaVersion"`
	DockerRegistry   string               `json:"dockerRegistry"`
	ExternalIP       string               `json:"externalip"`
	Propertystore    PropertyStoreCluster `json:"propertystore"`
	APIServer        ApiServer            `json:"apiserver"`
	RasSpec          RasSpec              `json:"rasSpec"`
	EngineController EngineController     `json:"engineController"`
	EngineResmon     ResourceMonitor      `json:"engineResmon"`
	Simbank          Simbank              `json:"simbank"`
	Monitoring       Monitoring           `json:"monitoring"`
}

type Monitoring struct {
	MetricsReplicas    *int32            `json:"metricsReplicas,omitempty"`
	PrometheusReplicas *int32            `json:"prometheusReplicas,omitempty"`
	GrafanaReplicas    *int32            `json:"grafanaReplicas,omitempty"`
	NodeSelector       map[string]string `json:"nodeselector,omitempty"`
}

type Simbank struct {
	Replicas     *int32            `json:"replicas,omitempty"`
	NodeSelector map[string]string `json:"nodeselector,omitempty"`
}

type EngineController struct {
	Replicas     *int32            `json:"replicas,omitempty"`
	NodeSelector map[string]string `json:"nodeselector,omitempty"`
}

type ResourceMonitor struct {
	Replicas     *int32            `json:"replicas,omitempty"`
	NodeSelector map[string]string `json:"nodeselector,omitempty"`
}

//Config for the ApiServer
type ApiServer struct {
	// Number of replicas
	Replicas *int32 `json:"replicas,omitempty"`

	NodeSelector map[string]string `json:"nodeselector,omitempty"`
}

//Config for the RasSpec
type RasSpec struct {
	// Number of replicas
	Replicas *int32 `json:"replicas,omitempty"`

	NodeSelector map[string]string `json:"nodeselector,omitempty"`
}

// PropertyStoreCluster spec for the CPS cluster
type PropertyStoreCluster struct {

	// Optional
	Resources corev1.ResourceRequirements `json:"resources,omitempty"`

	// Number of replicas
	Replicas *int32 `json:"replicas,omitempty"`

	// This size of the etcd cluster which hosts the CPS and DSS
	// Default size is 1
	PropertyClusterSize int32 `json:"property_cluster_size,omitempty"`

	// Any additional properties to be added to the CPS upon initialization
	InitProps []string `json:"init_props,omitempty"`

	NodeSelector map[string]string `json:"nodeselector,omitempty"`
}

// GalasaEcosystemStatus defines the observed state of GalasaEcosystem
type GalasaEcosystemStatus struct {
	// INSERT ADDITIONAL STATUS FIELD - define observed state of cluster
	// Important: Run "operator-sdk generate k8s" to regenerate code after modifying this file
	// Add custom validation using kubebuilder tags: https://book-v1.book.kubebuilder.io/beyond_basics/generating_crd.html
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// GalasaEcosystem is the Schema for the galasaecosystems API
// +kubebuilder:subresource:status
// +kubebuilder:resource:path=galasaecosystems,scope=Namespaced
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
