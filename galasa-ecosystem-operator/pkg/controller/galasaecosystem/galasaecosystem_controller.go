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
		r.ecosystemReady(instance)
		return reconcile.Result{}, err
	}

	// Generate the Ecosystem objects.

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ CPS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
	cpsCluster := cpsP{cps.New(instance)}

	reqLogger.Info("Check operator controller for CPS resource")
	if err := r.setOperatorController(instance, &cpsCluster); err != nil {
		r.ecosystemReady(instance)
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

	// Create all CPS resources if not exisisting
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: cpsCluster.StatefulSet.Name, Namespace: cpsCluster.StatefulSet.Namespace}, cpsCluster.StatefulSet)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(cpsCluster.StatefulSet)
		if err != nil {
			reqLogger.Error(err, "Failed to Create CPS Statefulset.", "Name", cpsCluster.StatefulSet.Name)
			return reconcile.Result{}, err
		}
	}
	instance.Status.CPSReadyReplicas = cpsCluster.StatefulSet.Status.ReadyReplicas
	r.client.Update(context.TODO(), instance)
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: cpsCluster.ExposedService.Name, Namespace: cpsCluster.ExposedService.Namespace}, cpsCluster.ExposedService)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(cpsCluster.ExposedService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create CPS Service.", "Name", cpsCluster.ExposedService.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: cpsCluster.InternalService.Name, Namespace: cpsCluster.InternalService.Namespace}, cpsCluster.InternalService)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(cpsCluster.InternalService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create CPS Service.", "Name", cpsCluster.InternalService.Name)
			return reconcile.Result{}, err
		}
	}

	// Check all updateable fields from the GalasaEcosystem CRD.
	clusterSize := instance.Spec.Propertystore.PropertyClusterSize
	if *cpsCluster.StatefulSet.Spec.Replicas != clusterSize {
		cpsCluster.StatefulSet.Spec.Replicas = &clusterSize
		err = r.updateResource(cpsCluster.StatefulSet)
		if err != nil {
			reqLogger.Error(err, "Failed to update CPS Stateful set.", "Name", cpsCluster.StatefulSet.Name)
			return reconcile.Result{}, err
		}
	}

	// Wait for CPS to be ready
	r.client.Get(context.TODO(), types.NamespacedName{Name: cpsCluster.StatefulSet.GetName(), Namespace: cpsCluster.StatefulSet.GetNamespace()}, cpsCluster.StatefulSet)
	reqLogger.Info("Waiting for CPS to become ready", "Current ready replicas", cpsCluster.StatefulSet.Status.ReadyReplicas)
	if cpsCluster.StatefulSet.Status.ReadyReplicas < 1 {
		r.reset(instance, cpsCluster)
		r.ecosystemReady(instance)
		return reconcile.Result{RequeueAfter: time.Second * 5, Requeue: true}, nil
	}

	// Load Init Props from Ecosystem Spec
	if newCpsPvc {
		reqLogger.Info("Loading Init Props")
		if err := cpsCluster.LoadInitProps(instance); err != nil {
			r.ecosystemReady(instance)
			return reconcile.Result{}, err
		}
		newCpsPvc = false
	}

	// Load DSS and CREDS location
	nodePort := getNodePort(cpsCluster.ExposedService, "etcd-client")
	cpsURI := "etcd:" + instance.Spec.ExternalHostname + ":" + nodePort
	cpsCluster.LoadProp("framework.dynamicstatus.store", cpsURI)
	cpsCluster.LoadProp("framework.credentials.store", cpsURI)

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ RAS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
	rasDB := rasP{ras.New(instance)}
	if err := r.setOperatorController(instance, &rasDB); err != nil {
		r.ecosystemReady(instance)
		return reconcile.Result{}, err
	}

	// Create all RAS resources if not exisisting
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: rasDB.StatefulSet.Name, Namespace: rasDB.StatefulSet.Namespace}, rasDB.StatefulSet)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(rasDB.StatefulSet)
		if err != nil {
			reqLogger.Error(err, "Failed to Create RAS Statefulset.", "Name", rasDB.StatefulSet.Name)
			return reconcile.Result{}, err
		}
	}
	instance.Status.RASReadyReplicas = rasDB.StatefulSet.Status.ReadyReplicas
	r.client.Update(context.TODO(), instance)
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: rasDB.ExposedService.Name, Namespace: rasDB.ExposedService.Namespace}, rasDB.ExposedService)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(rasDB.ExposedService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create RAS Service.", "Name", rasDB.ExposedService.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: rasDB.InternalService.Name, Namespace: rasDB.InternalService.Namespace}, rasDB.InternalService)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(rasDB.InternalService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create RAS Service.", "Name", rasDB.InternalService.Name)
			return reconcile.Result{}, err
		}
	}

	// Check all updateable fields from the GalasaEcosystem CRD.
	if *rasDB.StatefulSet.Spec.Replicas != *instance.Spec.RasSpec.Replicas {
		rasDB.StatefulSet.Spec.Replicas = instance.Spec.RasSpec.Replicas
		err = r.updateResource(rasDB.StatefulSet)
		if err != nil {
			reqLogger.Error(err, "Failed to update RAS Stateful set.", "Name", rasDB.StatefulSet.Name)
			return reconcile.Result{}, err
		}
	}

	reqLogger.Info("Waiting for RAS to become ready", "Current ready replicas", rasDB.StatefulSet.Status.ReadyReplicas)
	if rasDB.StatefulSet.Status.ReadyReplicas < 1 {
		r.reset(instance, cpsCluster)
		r.ecosystemReady(instance)
		return reconcile.Result{RequeueAfter: time.Second * 5, Requeue: true}, nil
	}

	nodePort = getNodePort(rasDB.ExposedService, "couchdbport")
	cpsCluster.LoadProp("framework.resultarchive.store", "couchdb:"+instance.Spec.ExternalHostname+":"+nodePort)

	instance.Status.RASReadyReplicas = rasDB.StatefulSet.Status.ReadyReplicas
	r.client.Update(context.TODO(), instance)

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ API ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
	apiServer := apiP{apiserver.New(instance, cpsCluster.ExposedService)}
	if err := r.setOperatorController(instance, &apiServer); err != nil {
		r.ecosystemReady(instance)
		return reconcile.Result{}, err
	}

	// Create all API resources if not exisisting
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: apiServer.PersistentVol.Name, Namespace: apiServer.PersistentVol.Namespace}, apiServer.PersistentVol)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(apiServer.PersistentVol)
		if err != nil {
			reqLogger.Error(err, "Failed to Create API PVC.", "Name", apiServer.PersistentVol.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: apiServer.Deployment.Name, Namespace: apiServer.Deployment.Namespace}, apiServer.Deployment)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(apiServer.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to Create API Deployment.", "Name", apiServer.Deployment.Name)
			return reconcile.Result{}, err
		}
	}
	instance.Status.APIReadyReplicas = apiServer.Deployment.Status.ReadyReplicas
	r.client.Update(context.TODO(), instance)
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: apiServer.BootstrapConf.Name, Namespace: apiServer.BootstrapConf.Namespace}, apiServer.BootstrapConf)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(apiServer.BootstrapConf)
		if err != nil {
			reqLogger.Error(err, "Failed to Create API ConfigMap.", "Name", apiServer.BootstrapConf.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: apiServer.ExposedService.Name, Namespace: apiServer.ExposedService.Namespace}, apiServer.ExposedService)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(apiServer.ExposedService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create API Service.", "Name", apiServer.ExposedService.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: apiServer.InternalService.Name, Namespace: apiServer.InternalService.Namespace}, apiServer.InternalService)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(apiServer.InternalService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create API Service.", "Name", apiServer.InternalService.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: apiServer.Ingress.Name, Namespace: apiServer.Ingress.Namespace}, apiServer.Ingress)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(apiServer.Ingress)
		if err != nil {
			reqLogger.Error(err, "Failed to Create API Ingress.", "Name", apiServer.Ingress.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: apiServer.TestCatalog.Name, Namespace: apiServer.TestCatalog.Namespace}, apiServer.TestCatalog)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(apiServer.TestCatalog)
		if err != nil {
			reqLogger.Error(err, "Failed to Create API Test Catalog.", "Name", apiServer.TestCatalog.Name)
			return reconcile.Result{}, err
		}
	}

	// Check all updateable fields from the GalasaEcosystem CRD.
	if *apiServer.Deployment.Spec.Replicas != *instance.Spec.APIServer.Replicas {
		apiServer.Deployment.Spec.Replicas = instance.Spec.APIServer.Replicas
		err = r.updateResource(apiServer.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to update API Deployment.", "Deployment.Name", apiServer.Deployment.Name)
			return reconcile.Result{}, err
		}
	}

	if apiServer.Deployment.Status.ReadyReplicas < 1 {
		r.ecosystemReady(instance)
		return reconcile.Result{RequeueAfter: time.Second * 10, Requeue: true}, nil
	}

	instance.Status.APIReadyReplicas = apiServer.Deployment.Status.ReadyReplicas
	if instance.Spec.IngressHostname != "" {
		reqLogger.Info("Setting boostrap", "URL", instance.Spec.IngressHostname+"/bootstrap")
		instance.Status.BootstrapURL = instance.Spec.IngressHostname + "/bootstrap"
	} else {
		reqLogger.Info("Setting boostrap", "URL", instance.Spec.ExternalHostname+":"+getNodePort(apiServer.ExposedService, "http"))
		instance.Status.BootstrapURL = instance.Spec.ExternalHostname + ":" + getNodePort(apiServer.ExposedService, "http") + "/bootstrap"
	}

	r.client.Update(context.TODO(), instance)

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Engine Controller ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//

	engineController := engineControllerP{engines.NewController(instance, apiServer.ExposedService)}
	if err := r.setOperatorController(instance, &engineController); err != nil {
		r.ecosystemReady(instance)
		return reconcile.Result{}, err
	}

	// Create all EngineController resources if not exisisting
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: engineController.Deployment.Name, Namespace: engineController.Deployment.Namespace}, engineController.Deployment)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(engineController.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to Create EngineController Deployment.", "Name", engineController.Deployment.Name)
			return reconcile.Result{}, err
		}
	}
	instance.Status.EngineControllerReadyReplicas = engineController.Deployment.Status.ReadyReplicas
	r.client.Update(context.TODO(), instance)
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: engineController.ConfigMap.Name, Namespace: engineController.ConfigMap.Namespace}, engineController.ConfigMap)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(engineController.ConfigMap)
		if err != nil {
			reqLogger.Error(err, "Failed to Create EngineController ConfigMap.", "Name", engineController.ConfigMap.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: engineController.InternalService.Name, Namespace: engineController.InternalService.Namespace}, engineController.InternalService)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(engineController.InternalService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create EngineController Service.", "Name", engineController.InternalService.Name)
			return reconcile.Result{}, err
		}
	}

	// Check all updateable fields from the GalasaEcosystem CRD.
	if *engineController.Deployment.Spec.Replicas != *instance.Spec.EngineController.Replicas {
		engineController.Deployment.Spec.Replicas = instance.Spec.EngineController.Replicas
		err = r.updateResource(engineController.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to update API Deployment.", "Deployment.Name", engineController.Deployment.Name)
			return reconcile.Result{}, err
		}
	}

	instance.Status.EngineControllerReadyReplicas = engineController.Deployment.Status.ReadyReplicas
	r.client.Update(context.TODO(), instance)

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ RESMAN ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
	resmon := resmonP{engines.NewResmon(instance)}
	if err := r.setOperatorController(instance, &resmon); err != nil {
		r.ecosystemReady(instance)
		return reconcile.Result{}, err
	}
	// Create all Resmon resources if not exisisting
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: resmon.Deployment.Name, Namespace: resmon.Deployment.Namespace}, resmon.Deployment)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(resmon.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Resmon Deployment.", "Name", resmon.Deployment.Name)
			return reconcile.Result{}, err
		}
	}
	instance.Status.ResmonReadyReplicas = resmon.Deployment.Status.ReadyReplicas
	r.client.Update(context.TODO(), instance)
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: resmon.InternalService.Name, Namespace: resmon.InternalService.Namespace}, resmon.InternalService)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(resmon.InternalService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Resmon Service.", "Name", resmon.InternalService.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: resmon.ExposedService.Name, Namespace: resmon.ExposedService.Namespace}, resmon.ExposedService)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(resmon.ExposedService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Resmon Service.", "Name", resmon.ExposedService.Name)
			return reconcile.Result{}, err
		}
	}

	// Check all updateable fields from the GalasaEcosystem CRD.
	if *resmon.Deployment.Spec.Replicas != *instance.Spec.EngineResmon.Replicas {
		resmon.Deployment.Spec.Replicas = instance.Spec.EngineResmon.Replicas
		err = r.updateResource(resmon.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to update Resmon Deployment.", "Deployment.Name", resmon.Deployment.Name)
			return reconcile.Result{}, err
		}
	}

	instance.Status.ResmonReadyReplicas = resmon.Deployment.Status.ReadyReplicas

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Simbank ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//

	r.client.Update(context.TODO(), instance)
	simbank := simbankP{simbank.New(instance)}
	if err := r.setOperatorController(instance, &simbank); err != nil {
		r.ecosystemReady(instance)
		return reconcile.Result{}, err
	}

	// Create all Simbank resources if not exisisting
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: simbank.Deployment.Name, Namespace: simbank.Deployment.Namespace}, simbank.Deployment)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(simbank.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Simbank Deployment.", "Name", simbank.Deployment.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: simbank.ExposedService.Name, Namespace: simbank.ExposedService.Namespace}, simbank.ExposedService)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(simbank.ExposedService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Simbank Service.", "Name", simbank.ExposedService.Name)
			return reconcile.Result{}, err
		}
	}

	// Check all updateable fields from the GalasaEcosystem CRD.
	if *simbank.Deployment.Spec.Replicas != *instance.Spec.Simbank.Replicas {
		simbank.Deployment.Spec.Replicas = instance.Spec.Simbank.Replicas
		err = r.updateResource(simbank.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to update Simbank Deployment.", "Deployment.Name", simbank.Deployment.Name)
			return reconcile.Result{}, err
		}
	}

	if err := simbankSetup(instance, apiServer, cpsCluster, simbank); err != nil {
		return reconcile.Result{}, err
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Monitoring ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
	grafana := grafanaP{monitoring.NewGrafana(instance)}
	if err := r.setOperatorController(instance, &grafana); err != nil {
		r.ecosystemReady(instance)
		return reconcile.Result{}, err
	}

	// Create all Grafana resources if not exisisting
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: grafana.PersistentVolumeClaim.Name, Namespace: grafana.PersistentVolumeClaim.Namespace}, grafana.PersistentVolumeClaim)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(grafana.PersistentVolumeClaim)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Grafana PVC.", "Name", grafana.PersistentVolumeClaim.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: grafana.Deployment.Name, Namespace: grafana.Deployment.Namespace}, grafana.Deployment)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(grafana.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Grafana Deployment.", "Name", grafana.Deployment.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: grafana.AutoDashboardConfigMap.Name, Namespace: grafana.AutoDashboardConfigMap.Namespace}, grafana.AutoDashboardConfigMap)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(grafana.AutoDashboardConfigMap)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Grafana ConfigMap.", "Name", grafana.AutoDashboardConfigMap.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: grafana.ConfigMap.Name, Namespace: grafana.ConfigMap.Namespace}, grafana.ConfigMap)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(grafana.ConfigMap)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Grafana ConfigMap.", "Name", grafana.ConfigMap.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: grafana.DashboardConfigMap.Name, Namespace: grafana.DashboardConfigMap.Namespace}, grafana.DashboardConfigMap)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(grafana.DashboardConfigMap)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Grafana ConfigMap.", "Name", grafana.DashboardConfigMap.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: grafana.ProvisioningConfigMap.Name, Namespace: grafana.ProvisioningConfigMap.Namespace}, grafana.ProvisioningConfigMap)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(grafana.ProvisioningConfigMap)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Grafana ConfigMap.", "Name", grafana.ProvisioningConfigMap.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: grafana.ExposedService.Name, Namespace: grafana.ExposedService.Namespace}, grafana.ExposedService)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(grafana.ExposedService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Grafana Service.", "Name", grafana.ExposedService.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: grafana.InternalService.Name, Namespace: grafana.InternalService.Namespace}, grafana.InternalService)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(grafana.InternalService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Grafana Service.", "Name", grafana.InternalService.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: grafana.Ingress.Name, Namespace: grafana.Ingress.Namespace}, grafana.Ingress)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(grafana.Ingress)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Grafana Ingress.", "Name", grafana.Ingress.Name)
			return reconcile.Result{}, err
		}
	}
	if instance.Spec.IngressHostname != "" {
		instance.Status.GrafanaURL = instance.Spec.IngressHostname + "/" + instance.Name + "-grafana"
	} else {
		instance.Status.GrafanaURL = instance.Spec.ExternalHostname + ":" + getNodePort(grafana.ExposedService, instance.Name+"-grafana-external-service") + "/galasa-grafana"
	}

	// Check all updateable fields from the GalasaEcosystem CRD.
	if *grafana.Deployment.Spec.Replicas != *instance.Spec.Monitoring.GrafanaReplicas {
		grafana.Deployment.Spec.Replicas = instance.Spec.Monitoring.GrafanaReplicas
		err = r.updateResource(grafana.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to update Grafana Deployment.", "Deployment.Name", grafana.Deployment.Name)
			return reconcile.Result{}, err
		}
	}

	prometheus := prometheusP{monitoring.NewPrometheus(instance)}
	if err := r.setOperatorController(instance, &prometheus); err != nil {
		r.ecosystemReady(instance)
		return reconcile.Result{}, err
	}

	// Create all Prometheus resources if not exisisting
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: prometheus.PersistentVolumeClaim.Name, Namespace: prometheus.PersistentVolumeClaim.Namespace}, prometheus.PersistentVolumeClaim)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(prometheus.PersistentVolumeClaim)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Prometheus PVC.", "Name", prometheus.PersistentVolumeClaim.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: prometheus.Deployment.Name, Namespace: prometheus.Deployment.Namespace}, prometheus.Deployment)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(prometheus.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Prometheus Deployment.", "Name", prometheus.Deployment.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: prometheus.ConfigMap.Name, Namespace: prometheus.ConfigMap.Namespace}, prometheus.ConfigMap)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(prometheus.ConfigMap)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Prometheus ConfigMap.", "Name", prometheus.ConfigMap.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: prometheus.ExposedService.Name, Namespace: prometheus.ExposedService.Namespace}, prometheus.ExposedService)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(prometheus.ExposedService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Prometheus Service.", "Name", prometheus.ExposedService.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: prometheus.InternalService.Name, Namespace: prometheus.InternalService.Namespace}, prometheus.InternalService)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(prometheus.InternalService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Prometheus Service.", "Name", prometheus.InternalService.Name)
			return reconcile.Result{}, err
		}
	}

	// Check all updateable fields from the GalasaEcosystem CRD.
	if *prometheus.Deployment.Spec.Replicas != *instance.Spec.Monitoring.PrometheusReplicas {
		prometheus.Deployment.Spec.Replicas = instance.Spec.Monitoring.PrometheusReplicas
		err = r.updateResource(prometheus.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to update Grafana Deployment.", "Deployment.Name", prometheus.Deployment.Name)
			return reconcile.Result{}, err
		}
	}

	metrics := metricsP{monitoring.NewMetrics(instance)}
	if err := r.setOperatorController(instance, &metrics); err != nil {
		r.ecosystemReady(instance)
		return reconcile.Result{}, err
	}

	// Create all metrics resources if not exisisting
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: metrics.Deployment.Name, Namespace: metrics.Deployment.Namespace}, metrics.Deployment)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(metrics.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Prometheus Deployment.", "Name", metrics.Deployment.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: metrics.InternalService.Name, Namespace: metrics.InternalService.Namespace}, metrics.InternalService)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(metrics.InternalService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Prometheus Service.", "Name", metrics.InternalService.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: metrics.ExposedService.Name, Namespace: metrics.ExposedService.Namespace}, metrics.ExposedService)
	if err != nil && errors.IsNotFound(err) {
		err = r.createResource(metrics.ExposedService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Prometheus Service.", "Name", metrics.ExposedService.Name)
			return reconcile.Result{}, err
		}
	}

	// Check all updateable fields from the GalasaEcosystem CRD.
	if *metrics.Deployment.Spec.Replicas != *instance.Spec.Monitoring.MetricsReplicas {
		metrics.Deployment.Spec.Replicas = instance.Spec.Monitoring.MetricsReplicas
		err = r.updateResource(metrics.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to update Metrics Deployment.", "Deployment.Name", metrics.Deployment.Name)
			return reconcile.Result{}, err
		}
	}

	instance.Status.MonitoringReadyReplicas = metrics.Deployment.Status.ReadyReplicas + prometheus.Deployment.Status.ReadyReplicas + grafana.Deployment.Status.ReadyReplicas
	r.client.Update(context.TODO(), instance)

	r.ecosystemReady(instance)
	return reconcile.Result{}, nil
}

func ecosystemCheck(instance *galasav1alpha1.GalasaEcosystem) bool {
	checkLog := log.WithName("Check-Log")
	checkLog.Info("Instance", "Instance", instance)
	if instance.Status.CPSReadyReplicas != instance.Spec.Propertystore.PropertyClusterSize {
		checkLog.Info("Etcd cluster not ready", "Required", instance.Spec.Propertystore.PropertyClusterSize, "Available", String(instance.Status.CPSReadyReplicas))
		return false
	}
	if instance.Status.RASReadyReplicas != *instance.Spec.RasSpec.Replicas {
		checkLog.Info("RAS not ready", "Required", *instance.Spec.RasSpec.Replicas, "Available", instance.Status.RASReadyReplicas)
		return false
	}
	if instance.Status.APIReadyReplicas != *instance.Spec.APIServer.Replicas {
		checkLog.Info("API server not ready", "Required", *instance.Spec.APIServer.Replicas, "Available", instance.Status.APIReadyReplicas)
		return false
	}
	if instance.Status.MonitoringReadyReplicas != *instance.Spec.Monitoring.PrometheusReplicas+*instance.Spec.Monitoring.GrafanaReplicas+*instance.Spec.Monitoring.MetricsReplicas {
		checkLog.Info("Monitoring not ready", "Required", *instance.Spec.Monitoring.PrometheusReplicas+*instance.Spec.Monitoring.GrafanaReplicas+*instance.Spec.Monitoring.MetricsReplicas, "Available", instance.Status.MonitoringReadyReplicas)
		return false
	}
	if instance.Status.EngineControllerReadyReplicas != *instance.Spec.EngineController.Replicas {
		checkLog.Info("Engine Controller not ready", "Required", *instance.Spec.EngineController.Replicas, "Available", instance.Status.EngineControllerReadyReplicas)
		return false
	}
	if instance.Status.ResmonReadyReplicas != *instance.Spec.EngineResmon.Replicas {
		checkLog.Info("Resource Management not ready", "Required", *instance.Spec.EngineResmon.Replicas, "Available", instance.Status.ResmonReadyReplicas)
		return false
	}
	return true
}

func (r *ReconcileGalasaEcosystem) ecosystemReady(instance *galasav1alpha1.GalasaEcosystem) {
	ecosystemReady = ecosystemCheck(instance)
	instance.Status.EcosystemReady = ecosystemReady
	r.client.Update(context.TODO(), instance)
}

func (r *ReconcileGalasaEcosystem) reset(instance *galasav1alpha1.GalasaEcosystem, cps cpsP) {
	zero := int32(0)
	resetLog := log.WithName("Operator-Log")

	if instance.Status.APIReadyReplicas > 0 {
		resetLog.Info("Scaling down any APIservers")
		apiServer := apiP{apiserver.New(instance, cps.ExposedService)}
		if err := r.setOperatorController(instance, &apiServer); err != nil {
			r.ecosystemReady(instance)
			resetLog.Error(err, "Failed to set operator as controller")
			return
		}

		apiServer.Deployment.Spec.Replicas = &zero
		resetLog.Info("Object For API", "Object", apiServer.Deployment)
		err := r.client.Update(context.TODO(), apiServer.Deployment)
		resetLog.Info("Resp", "Err", err)
		apiServer.Deployment.Spec.Replicas = instance.Spec.APIServer.Replicas
		return
	}
	resetLog.Info("Nothing to reset")
}

func (r *ReconcileGalasaEcosystem) setOperatorController(instance *galasav1alpha1.GalasaEcosystem, v1Objects galasaObject) error {
	for _, object := range v1Objects.getResourceObjects() {
		if err := controllerutil.SetControllerReference(instance, object.Meta, r.scheme); err != nil {
			return err
		}
	}
	return nil
}

func (r *ReconcileGalasaEcosystem) updateResource(object runtime.Object) error {
	updateLogger := log.WithName("UpdateLog")
	updateLogger.Info("Updating the Object", "Object", object)
	err := r.client.Update(context.TODO(), object)
	if err != nil {
		return err
	}
	return nil
}

func (r *ReconcileGalasaEcosystem) createResource(object runtime.Object) error {
	createLogger := log.WithName("CreateLog")
	createLogger.Info("Creating the Object", "Object", object)
	err := r.client.Create(context.TODO(), object)
	if err != nil {
		return err
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
			Meta:    metav1.Object(g.Ingress),
			Runtime: runtime.Object(g.Ingress),
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
