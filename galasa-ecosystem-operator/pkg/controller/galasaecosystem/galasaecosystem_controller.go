package galasaecosystem

import (
	"bytes"
	"context"
	"io/ioutil"
	"net/http"
	"net/url"
	"strings"
	"time"

	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-operator/pkg/apis/galasa/v1alpha1"
	"github.com/galasa-dev/extensions/galasa-ecosystem-operator/pkg/apiserver"
	"github.com/galasa-dev/extensions/galasa-ecosystem-operator/pkg/cps"
	"github.com/galasa-dev/extensions/galasa-ecosystem-operator/pkg/engines"
	"github.com/galasa-dev/extensions/galasa-ecosystem-operator/pkg/monitoring"
	"github.com/galasa-dev/extensions/galasa-ecosystem-operator/pkg/ras"
	"github.com/galasa-dev/extensions/galasa-ecosystem-operator/pkg/simbank"
	"github.com/go-logr/logr"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	v1beta1 "k8s.io/api/networking/v1beta1"
	"k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"sigs.k8s.io/controller-runtime/pkg/source"
)

var log = logf.Log.WithName("controller_galasaecosystem")
var newCpsPvc = false
var ecosystemReady = false

// Add creates a new GalasaEcosystem Controller and adds it to the Manager. The Manager will set fields on the Controller
// and Start it when the Manager is Started.
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

// newReconciler returns a new reconcile.Reconciler
func newReconciler(mgr manager.Manager) reconcile.Reconciler {
	return &ReconcileGalasaEcosystem{client: mgr.GetClient(), scheme: mgr.GetScheme()}
}

// add adds a new Controller to mgr with r as the reconcile.Reconciler
func add(mgr manager.Manager, r reconcile.Reconciler) error {
	// Create a new controller
	c, err := controller.New("galasaecosystem-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	// Watch for changes to primary resource GalasaEcosystem
	err = c.Watch(&source.Kind{Type: &galasav1alpha1.GalasaEcosystem{}}, &handler.EnqueueRequestForObject{})
	if err != nil {
		return err
	}

	// Watch for changes to secondary resource Pods and requeue the owner GalasaEcosystem
	err = c.Watch(&source.Kind{Type: &appsv1.StatefulSet{}}, &handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &galasav1alpha1.GalasaEcosystem{},
	})
	if err != nil {
		return err
	}
	err = c.Watch(&source.Kind{Type: &appsv1.Deployment{}}, &handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &galasav1alpha1.GalasaEcosystem{},
	})
	if err != nil {
		return err
	}
	err = c.Watch(&source.Kind{Type: &corev1.PersistentVolumeClaim{}}, &handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &galasav1alpha1.GalasaEcosystem{},
	})
	if err != nil {
		return err
	}
	err = c.Watch(&source.Kind{Type: &corev1.ConfigMap{}}, &handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &galasav1alpha1.GalasaEcosystem{},
	})
	if err != nil {
		return err
	}
	err = c.Watch(&source.Kind{Type: &corev1.Service{}}, &handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &galasav1alpha1.GalasaEcosystem{},
	})
	if err != nil {
		return err
	}
	err = c.Watch(&source.Kind{Type: &v1beta1.Ingress{}}, &handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &galasav1alpha1.GalasaEcosystem{},
	})
	if err != nil {
		return err
	}

	return nil
}

// blank assignment to verify that ReconcileGalasaEcosystem implements reconcile.Reconciler
var _ reconcile.Reconciler = &ReconcileGalasaEcosystem{}

// ReconcileGalasaEcosystem reconciles a GalasaEcosystem object
type ReconcileGalasaEcosystem struct {
	// This client, initialized using mgr.Client() above, is a split client
	// that reads objects from the cache and writes to the apiserver
	client client.Client
	scheme *runtime.Scheme
}

// Reconcile reads that state of the cluster for a GalasaEcosystem object and makes changes based on the state read
// and what is in the GalasaEcosystem.Spec
// TODO(user): Modify this Reconcile function to implement your Controller logic.  This example creates
// a Pod as an example
// Note:
// The Controller will requeue the Request to be processed again if the returned error is non-nil or
// Result.Requeue is true, otherwise upon completion it will remove the work from the queue.
func (r *ReconcileGalasaEcosystem) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	reqLogger := log.WithName("Operator-Log") //("Request.Namespace", request.Namespace, "Request.Name", request.Name)
	reqLogger.Info("Reconciling GalasaEcosystem")

	// Fetch the GalasaEcosystem instance
	instance := &galasav1alpha1.GalasaEcosystem{}
	err := r.client.Get(context.TODO(), request.NamespacedName, instance)
	if err != nil {
		if errors.IsNotFound(err) {
			// Request object not found, could have been deleted after reconcile request.
			// Owned objects are automatically garbage collected. For additional cleanup logic use finalizers.
			// Return and don't requeue
			return reconcile.Result{}, nil
		}
		// Error reading the object - requeue the request.
		r.ecosystemReady(false, instance)
		return reconcile.Result{}, err
	}

	// Generate the Ecosystem objects.

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ CPS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
	cps := cpsP{cps.New(instance)}
	reqLogger.Info("Check operator controller for CPS resource")
	if err := r.setOperatorController(instance, &cps); err != nil {
		r.ecosystemReady(false, instance)
		return reconcile.Result{}, err
	}

	// Check for if PVC is already defined. If not load InitProps
	cpsPVCs := &corev1.PersistentVolumeClaimList{}
	if err := r.client.List(context.TODO(), cpsPVCs, &client.ListOptions{LabelSelector: client.MatchingLabelsSelector{labels.SelectorFromSet(labels.Set{"app": "galasa-ecosystem-cps"})}}); err != nil {
		reqLogger.Info("Error", "error", err)
		return reconcile.Result{}, err
	}
	if len(cpsPVCs.Items) < 1 {
		reqLogger.Info("No backing PVC for CPS. PVC will be defined and Init props loaded.", "error", err)
		newCpsPvc = true
	} else {
		reqLogger.Info("No requirement to enter InitProps")
	}

	reqLogger.Info("Reconcile CPS resource")
	if err := r.reconcileResources(&cps, reqLogger); err != nil {
		r.ecosystemReady(false, instance)
		return reconcile.Result{}, err
	}
	// Wait for CPS to be ready
	reqLogger.Info("Waiting for CPS to become ready", "Current ready replicas", cps.StatefulSet.Status.ReadyReplicas)
	if cps.StatefulSet.Status.ReadyReplicas < 1 {
		r.ecosystemReady(false, instance)
		return reconcile.Result{RequeueAfter: time.Second * 5, Requeue: true}, nil
	}

	// Load Init Props from Ecosystem Spec
	if newCpsPvc {
		reqLogger.Info("Loading Init Props")
		if err := cps.LoadInitProps(instance); err != nil {
			r.ecosystemReady(false, instance)
			return reconcile.Result{}, err
		}
		newCpsPvc = false
	}

	//Load DSS and CREDS location
	nodePort := getNodePort(cps.ExposedService, "etcd-client")
	cpsURI := "etcd:" + instance.Spec.ExternalHostname + ":" + nodePort
	cps.LoadProp("framework.dynamicstatus.store", cpsURI)
	cps.LoadProp("framework.credentials.store", cpsURI)

	// Update the Galasa Ecosystem status
	reqLogger.Info("Updating Galasa Ecosystem Status")
	instance.Status.CPSReadyReplicas = cps.StatefulSet.Status.ReadyReplicas
	r.client.Update(context.TODO(), instance)

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ RAS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
	ras := rasP{ras.New(instance)}
	if err := r.setOperatorController(instance, &ras); err != nil {
		r.ecosystemReady(false, instance)
		return reconcile.Result{}, err
	}
	if err := r.reconcileResources(&ras, reqLogger); err != nil {
		r.ecosystemReady(false, instance)
		return reconcile.Result{}, err
	}
	reqLogger.Info("Waiting for RAS to become ready", "Current ready replicas", ras.StatefulSet.Status.ReadyReplicas)
	if ras.StatefulSet.Status.ReadyReplicas < 1 {
		r.ecosystemReady(false, instance)
		return reconcile.Result{RequeueAfter: time.Second * 5, Requeue: true}, nil
	}

	nodePort = getNodePort(ras.ExposedService, "couchdbport")
	cps.LoadProp("framework.resultarchive.store", "couchdb:"+instance.Spec.ExternalHostname+":"+nodePort)

	// if len(ras.Ingress.Status.LoadBalancer.Ingress) < 1 {
	// 	r.ecosystemReady(false, instance)
	// 	return reconcile.Result{RequeueAfter: time.Second * 30, Requeue: true}, nil
	// }

	// // This appears to be a solution that works for GCP
	// annotation := ras.Ingress.Annotations
	// state := annotation["ingress.kubernetes.io/backends"]
	// reqLogger.Info("The state", "state", state)
	// if strings.Contains(state, "UNHEALTHY") || strings.Contains(state, "Unknown") {
	// 	r.ecosystemReady(false, instance)
	// 	return reconcile.Result{RequeueAfter: time.Second * 30, Requeue: true}, nil
	// }

	instance.Status.RASReadyReplicas = ras.StatefulSet.Status.ReadyReplicas
	r.client.Update(context.TODO(), instance)

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ API ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//

	apiserver := apiP{apiserver.New(instance, cps.ExposedService)}
	if err := r.setOperatorController(instance, &apiserver); err != nil {
		r.ecosystemReady(false, instance)
		return reconcile.Result{}, err
	}
	if err := r.reconcileResources(&apiserver, reqLogger); err != nil {
		r.ecosystemReady(false, instance)
		return reconcile.Result{}, err
	}
	if apiserver.Deployment.Status.ReadyReplicas < 1 {
		r.ecosystemReady(false, instance)
		return reconcile.Result{RequeueAfter: time.Second * 10, Requeue: true}, nil
	}

	instance.Status.APIReadyReplicas = apiserver.Deployment.Status.ReadyReplicas
	reqLogger.Info("Setting boostrap", "URL", instance.Spec.ExternalHostname+":"+getNodePort(apiserver.ExposedService, "http"))
	instance.Status.BootstrapURL = instance.Spec.ExternalHostname + ":" + getNodePort(apiserver.ExposedService, "http") + "/boostrap"
	r.client.Update(context.TODO(), instance)

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Engine Controller ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//

	engineController := engineControllerP{engines.NewController(instance, apiserver.ExposedService)}
	if err := r.setOperatorController(instance, &engineController); err != nil {
		r.ecosystemReady(false, instance)
		return reconcile.Result{}, err
	}
	if err := r.reconcileResources(&engineController, reqLogger); err != nil {
		r.ecosystemReady(false, instance)
		return reconcile.Result{}, err
	}

	instance.Status.EngineControllerReadyReplicas = engineController.Deployment.Status.ReadyReplicas
	r.client.Update(context.TODO(), instance)

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ RESMAN ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
	resmon := resmonP{engines.NewResmon(instance)}
	if err := r.setOperatorController(instance, &resmon); err != nil {
		r.ecosystemReady(false, instance)
		return reconcile.Result{}, err
	}
	if err := r.reconcileResources(&resmon, reqLogger); err != nil {
		r.ecosystemReady(false, instance)
		return reconcile.Result{}, err
	}
	instance.Status.ResmonReadyReplicas = resmon.Deployment.Status.ReadyReplicas

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Simbank ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//

	r.client.Update(context.TODO(), instance)
	simbank := simbankP{simbank.New(instance)}
	if err := r.setOperatorController(instance, &simbank); err != nil {
		r.ecosystemReady(false, instance)
		return reconcile.Result{}, err
	}
	if err := r.reconcileResources(&simbank, reqLogger); err != nil {
		r.ecosystemReady(false, instance)
		return reconcile.Result{}, err
	}
	if err := simbankSetup(instance, apiserver, cps, simbank); err != nil {
		return reconcile.Result{}, err
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Monitoring ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
	grafana := grafanaP{monitoring.NewGrafana(instance)}
	if err := r.setOperatorController(instance, &grafana); err != nil {
		r.ecosystemReady(false, instance)
		return reconcile.Result{}, err
	}
	if err := r.reconcileResources(&grafana, reqLogger); err != nil {
		r.ecosystemReady(false, instance)
		return reconcile.Result{}, err
	}
	prometheus := prometheusP{monitoring.NewPrometheus(instance)}
	if err := r.setOperatorController(instance, &prometheus); err != nil {
		r.ecosystemReady(false, instance)
		return reconcile.Result{}, err
	}
	if err := r.reconcileResources(&prometheus, reqLogger); err != nil {
		r.ecosystemReady(false, instance)
		return reconcile.Result{}, err
	}
	metrics := metricsP{monitoring.NewMetrics(instance)}
	if err := r.setOperatorController(instance, &metrics); err != nil {
		r.ecosystemReady(false, instance)
		return reconcile.Result{}, err
	}
	if err := r.reconcileResources(&metrics, reqLogger); err != nil {
		r.ecosystemReady(false, instance)
		return reconcile.Result{}, err
	}

	instance.Status.MonitoringReadyReplicas = metrics.Deployment.Status.ReadyReplicas + prometheus.Deployment.Status.ReadyReplicas + grafana.Deployment.Status.ReadyReplicas
	r.client.Update(context.TODO(), instance)

	r.ecosystemReady(true, instance)
	return reconcile.Result{}, nil
}

func (r *ReconcileGalasaEcosystem) ecosystemReady(ready bool, instance *galasav1alpha1.GalasaEcosystem) {
	ecosystemReady = ready
	instance.Status.EcosystemReady = ecosystemReady
	r.client.Update(context.TODO(), instance)
}

func (r *ReconcileGalasaEcosystem) setOperatorController(instance *galasav1alpha1.GalasaEcosystem, v1Objects galasaObject) error {
	for _, object := range v1Objects.getResourceObjects() {
		if err := controllerutil.SetControllerReference(instance, object.Meta, r.scheme); err != nil {
			return err
		}
	}
	return nil
}

func (r *ReconcileGalasaEcosystem) reconcileResources(v1Objects galasaObject, reqLogger logr.Logger) error {
	for _, object := range v1Objects.getResourceObjects() {
		err := r.client.Get(context.TODO(), types.NamespacedName{Name: object.Meta.GetName(), Namespace: object.Meta.GetNamespace()}, object.Runtime)
		if err != nil && errors.IsNotFound(err) {
			reqLogger.Info("Creating the Object", "Name", object.Meta.GetName())
			err = r.client.Create(context.TODO(), object.Runtime)
			if err != nil {
				return err
			}
			return nil
		} else if err != nil {
			return err
		}

		// Resource already exists - don't requeue
		reqLogger.Info("Skip reconcile: Resource already exists", "Name", object.Meta.GetName())
	}
	return nil
}

func getPodNames(pods []corev1.Pod) []string {
	var podNames []string
	for _, pod := range pods {
		podNames = append(podNames, pod.Name)
	}
	return podNames
}

type resourceObjects struct {
	Meta    metav1.Object
	Runtime runtime.Object
}

type galasaObject interface {
	getResourceObjects() []resourceObjects
}
type cpsP struct {
	*cps.CPS
}
type apiP struct {
	*apiserver.APIServer
}
type rasP struct {
	*ras.RAS
}
type engineControllerP struct {
	*engines.Controller
}
type resmonP struct {
	*engines.Resmon
}
type simbankP struct {
	*simbank.Simbank
}
type metricsP struct {
	*monitoring.Metrics
}
type prometheusP struct {
	*monitoring.Prometheus
}
type grafanaP struct {
	*monitoring.Grafana
}

func (c *cpsP) getResourceObjects() []resourceObjects {
	return []resourceObjects{
		{
			Meta:    metav1.Object(c.PersistentVolumeClaim),
			Runtime: runtime.Object(c.PersistentVolumeClaim),
		},
		{
			Meta:    metav1.Object(c.ExposedService),
			Runtime: runtime.Object(c.ExposedService),
		},
		{
			Meta:    metav1.Object(c.InternalService),
			Runtime: runtime.Object(c.InternalService),
		},
		{
			Meta:    metav1.Object(c.StatefulSet),
			Runtime: runtime.Object(c.StatefulSet),
		},
	}
}

func (a *apiP) getResourceObjects() []resourceObjects {
	return []resourceObjects{
		{
			Meta:    metav1.Object(a.TestCatalog),
			Runtime: runtime.Object(a.TestCatalog),
		},
		{
			Meta:    metav1.Object(a.BootstrapConf),
			Runtime: runtime.Object(a.BootstrapConf),
		},
		{
			Meta:    metav1.Object(a.ExposedService),
			Runtime: runtime.Object(a.ExposedService),
		},
		{
			Meta:    metav1.Object(a.InternalService),
			Runtime: runtime.Object(a.InternalService),
		},
		{
			Meta:    metav1.Object(a.Ingress),
			Runtime: runtime.Object(a.Ingress),
		},
		{
			Meta:    metav1.Object(a.PersistentVol),
			Runtime: runtime.Object(a.PersistentVol),
		},
		{
			Meta:    metav1.Object(a.Deployment),
			Runtime: runtime.Object(a.Deployment),
		},
	}
}

func (r *rasP) getResourceObjects() []resourceObjects {
	return []resourceObjects{
		{
			Meta:    metav1.Object(r.PersistentVolumeClaim),
			Runtime: runtime.Object(r.PersistentVolumeClaim),
		},
		{
			Meta:    metav1.Object(r.InternalService),
			Runtime: runtime.Object(r.InternalService),
		},
		{
			Meta:    metav1.Object(r.ExposedService),
			Runtime: runtime.Object(r.ExposedService),
		},
		{
			Meta:    metav1.Object(r.StatefulSet),
			Runtime: runtime.Object(r.StatefulSet),
		},
		{
			Meta:    metav1.Object(r.Ingress),
			Runtime: runtime.Object(r.Ingress),
		},
	}
}

func (e *engineControllerP) getResourceObjects() []resourceObjects {
	return []resourceObjects{
		{
			Meta:    metav1.Object(e.ConfigMap),
			Runtime: runtime.Object(e.ConfigMap),
		},
		{
			Meta:    metav1.Object(e.InternalService),
			Runtime: runtime.Object(e.InternalService),
		},
		{
			Meta:    metav1.Object(e.Deployment),
			Runtime: runtime.Object(e.Deployment),
		},
	}
}

func (r *resmonP) getResourceObjects() []resourceObjects {
	return []resourceObjects{
		{
			Meta:    metav1.Object(r.InternalService),
			Runtime: runtime.Object(r.InternalService),
		},
		{
			Meta:    metav1.Object(r.ExposedService),
			Runtime: runtime.Object(r.ExposedService),
		},
		{
			Meta:    metav1.Object(r.Deployment),
			Runtime: runtime.Object(r.Deployment),
		},
	}
}

func (s *simbankP) getResourceObjects() []resourceObjects {
	return []resourceObjects{
		{
			Meta:    metav1.Object(s.ExposedService),
			Runtime: runtime.Object(s.ExposedService),
		},
		{
			Meta:    metav1.Object(s.Deployment),
			Runtime: runtime.Object(s.Deployment),
		},
	}
}

func (m *metricsP) getResourceObjects() []resourceObjects {
	return []resourceObjects{
		{
			Meta:    metav1.Object(m.InternalService),
			Runtime: runtime.Object(m.InternalService),
		},
		{
			Meta:    metav1.Object(m.ExposedService),
			Runtime: runtime.Object(m.ExposedService),
		},
		{
			Meta:    metav1.Object(m.Deployment),
			Runtime: runtime.Object(m.Deployment),
		},
	}
}

func (p *prometheusP) getResourceObjects() []resourceObjects {
	return []resourceObjects{
		{
			Meta:    metav1.Object(p.ConfigMap),
			Runtime: runtime.Object(p.ConfigMap),
		},
		{
			Meta:    metav1.Object(p.ExposedService),
			Runtime: runtime.Object(p.ExposedService),
		},
		{
			Meta:    metav1.Object(p.InternalService),
			Runtime: runtime.Object(p.InternalService),
		},
		{
			Meta:    metav1.Object(p.PersistentVolumeClaim),
			Runtime: runtime.Object(p.PersistentVolumeClaim),
		},
		{
			Meta:    metav1.Object(p.Deployment),
			Runtime: runtime.Object(p.Deployment),
		},
	}
}

func (g *grafanaP) getResourceObjects() []resourceObjects {
	return []resourceObjects{
		{
			Meta:    metav1.Object(g.ConfigMap),
			Runtime: runtime.Object(g.ConfigMap),
		},
		{
			Meta:    metav1.Object(g.DashboardConfigMap),
			Runtime: runtime.Object(g.DashboardConfigMap),
		},
		{
			Meta:    metav1.Object(g.AutoDashboardConfigMap),
			Runtime: runtime.Object(g.AutoDashboardConfigMap),
		},
		{
			Meta:    metav1.Object(g.InternalService),
			Runtime: runtime.Object(g.InternalService),
		},
		{
			Meta:    metav1.Object(g.ExposedService),
			Runtime: runtime.Object(g.ExposedService),
		},
		{
			Meta:    metav1.Object(g.PersistentVolumeClaim),
			Runtime: runtime.Object(g.PersistentVolumeClaim),
		},
		{
			Meta:    metav1.Object(g.ProvisioningConfigMap),
			Runtime: runtime.Object(g.ProvisioningConfigMap),
		},
		{
			Meta:    metav1.Object(g.Deployment),
			Runtime: runtime.Object(g.Deployment),
		},
	}
}

func getNodePort(service *corev1.Service, name string) string {
	ports := service.Spec.Ports
	for _, p := range ports {
		if p.Name == name {
			return String(p.NodePort)
		}
	}
	return ""
}

func simbankSetup(cr *galasav1alpha1.GalasaEcosystem, api apiP, cps cpsP, simbank simbankP) error {
	simlog := log.WithName("SimLog")
	required := false
	hostname, _ := url.Parse(cr.Spec.ExternalHostname)
	location := cr.Spec.ExternalHostname + ":" + getNodePort(api.ExposedService, "http") + "/testcatalog/simbank"
	simlog.Info("Test catatlog location", "location", location)
	streams, _ := cps.GetProp("framework.test.streams")
	simlog.Info("Streams", "Streams", streams)
	if streams == " " {
		simlog.Info("No stream set, applying first stream", "Stream", "SIMBANK")
		cps.LoadProp("framework.test.streams", "SIMBANK")
		required = true

	} else {
		if strings.Contains(streams, "SIMBANK") {
			simlog.Info("Simbank already a test stream")
		} else {
			cps.LoadProp("framework.test.streams", streams+",SIMBANK")
			required = true
		}
		simlog.Info("No setup required, skipping")
	}

	if required {
		simlog.Info("Putting simbank CPS properties")
		cps.LoadProp("framework.test.stream.SIMBANK.description", "Simbank tests")
		cps.LoadProp("framework.test.stream.SIMBANK.location", location)
		cps.LoadProp("framework.test.stream.SIMBANK.obr", "mvn:dev.galasa/dev.galasa.simbank.obr/"+cr.Spec.GalasaVersion+"/obr")
		cps.LoadProp("framework.test.stream.SIMBANK.repo", cr.Spec.MavenRepository)

		//Test props
		cps.LoadProp("secure.credentials.SIMBANK.username", "IBMUSER")
		cps.LoadProp("secure.credentials.SIMBANK.password", "SYS1")

		cps.LoadProp("zos.dse.tag.SIMBANK.imageid", "SIMBANK")
		cps.LoadProp("zos.dse.tag.SIMBANK.clusterid", "SIMBANK")
		cps.LoadProp("zos.image.SIMBANK.ipv4.hostname", hostname.Host)
		cps.LoadProp("zos.image.SIMBANK.telnet.port", getNodePort(simbank.ExposedService, "simbank-telnet"))
		cps.LoadProp("zos.image.SIMBANK.telnet.tls", "false")
		cps.LoadProp("zos.image.SIMBANK.credentials", "SIMBANK")
		cps.LoadProp("zosmf.server.SIMBANK.images", "SIMBANK")
		cps.LoadProp("zosmf.server.SIMBANK.hostname", hostname.Host)
		cps.LoadProp("zosmf.server.SIMBANK.port", getNodePort(simbank.ExposedService, "simbank-mf"))

		cps.LoadProp("simbank.dse.instance.name", "SIMBANK")
		cps.LoadProp("simbank.instance.SIMBANK.zos.image", "SIMBANK")
		cps.LoadProp("simbank.instance.SIMBANK.database.port", getNodePort(simbank.ExposedService, "simbank-database"))
		cps.LoadProp("simbank.instance.SIMBANK.webnet.port", getNodePort(simbank.ExposedService, "simbank-webservice"))
	}

	if resp, err := http.Get(location); err == nil && resp.StatusCode == 404 {
		simlog.Info("Checking", "resp", resp, "err", err)
		simlog.Info("Puting test catalog from simbank tests")
		if data, err := ioutil.ReadFile("/usr/local/bin/galasa-resources/simplatform-testcatalog.json"); err == nil {
			client := &http.Client{}
			req, err := http.NewRequest(http.MethodPut, location, bytes.NewReader(data))
			if err != nil {
				return err
			}
			req.Header.Set("Content-Type", "application/json")
			simlog.Info("Sending testcatalog request")
			resp, err = client.Do(req)
			if err != nil {
				simlog.Info("Failed", "resp", resp, "err", err)
				return err
			}
		} else {
			return err
		}
	} else {
		simlog.Info("Checking", "resp", resp.StatusCode, "err", err)
	}
	return nil
}

func String(n int32) string {
	buf := [11]byte{}
	pos := len(buf)
	i := int64(n)
	signed := i < 0
	if signed {
		i = -i
	}
	for {
		pos--
		buf[pos], i = '0'+byte(i%10), i/10
		if i == 0 {
			if signed {
				pos--
				buf[pos] = '-'
			}
			return string(buf[pos:])
		}
	}
}
