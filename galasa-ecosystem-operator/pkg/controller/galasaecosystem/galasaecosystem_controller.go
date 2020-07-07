package galasaecosystem

import (
	"context"

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
	"k8s.io/apimachinery/pkg/api/errors"
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

	// TODO(user): Modify this to be the types you create that are owned by the primary resource
	// Watch for changes to secondary resource Pods and requeue the owner GalasaEcosystem
	err = c.Watch(&source.Kind{Type: &appsv1.StatefulSet{}}, &handler.EnqueueRequestForOwner{
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

type ecosystem struct {
	cps              *cps.CPS
	apiServer        *apiserver.APIServer
	ras              *ras.RAS
	engineController *engines.Controller
	resmon           *engines.Resmon
	simbank          *simbank.Simbank
	metrics          *monitoring.Metrics
	// prometheus       *monitoring.Prometheus
	// grafana          *monitoring.Grafana
}

// Reconcile reads that state of the cluster for a GalasaEcosystem object and makes changes based on the state read
// and what is in the GalasaEcosystem.Spec
// TODO(user): Modify this Reconcile function to implement your Controller logic.  This example creates
// a Pod as an example
// Note:
// The Controller will requeue the Request to be processed again if the returned error is non-nil or
// Result.Requeue is true, otherwise upon completion it will remove the work from the queue.
func (r *ReconcileGalasaEcosystem) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
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
		return reconcile.Result{}, err
	}

	// So we want all the logic for the entire Galasa_ecosystem to come through this reconcile loop
	// It has to be idempotent as it will operate against missing and existing resources.

	// I think we might want some sort of struct in this controller for all the infomation that has to be formed by the cluster

	/////////////////////// CPS ///////////////////////
	cps := cps.New(instance)
	if err := controllerutil.SetControllerReference(instance, cps.StatefulSet, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, cps.InternalService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, cps.ExposedService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}

	if _, err := r.reconcileService(cps.InternalService, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileService(cps.ExposedService, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileStatefulSet(cps.StatefulSet, reqLogger); err != nil {
		return reconcile.Result{}, err
	}

	/////////////////////// API Server ///////////////////////
	apiServer := apiserver.New(instance, cps.ExposedService)
	if err := controllerutil.SetControllerReference(instance, apiServer.BootstrapConf, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, apiServer.InternalService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, apiServer.ExposedService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, apiServer.PersistentVol, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, apiServer.TestCatalog, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, apiServer.Deployment, r.scheme); err != nil {
		return reconcile.Result{}, err
	}

	if _, err := r.reconcileService(apiServer.InternalService, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileService(apiServer.ExposedService, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileConfigMap(apiServer.BootstrapConf, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcilePersistentVolume(apiServer.PersistentVol, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileConfigMap(apiServer.TestCatalog, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileDeployment(apiServer.Deployment, reqLogger); err != nil {
		return reconcile.Result{}, err
	}

	/////////////////////// RAS ///////////////////////
	ras := ras.New(instance)
	if err := controllerutil.SetControllerReference(instance, ras.ExposedService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, ras.InternalService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, ras.StatefulSet, r.scheme); err != nil {
		return reconcile.Result{}, err
	}

	if _, err := r.reconcileService(ras.InternalService, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileService(ras.ExposedService, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileStatefulSet(ras.StatefulSet, reqLogger); err != nil {
		return reconcile.Result{}, err
	}

	/////////////////////// Engine Controller ///////////////////////
	engineController := engines.NewController(instance, apiServer.InternalService)
	if err := controllerutil.SetControllerReference(instance, engineController.InternalService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, engineController.Deployment, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, engineController.ConfigMap, r.scheme); err != nil {
		return reconcile.Result{}, err
	}

	if _, err := r.reconcileService(engineController.InternalService, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileDeployment(engineController.Deployment, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileConfigMap(engineController.ConfigMap, reqLogger); err != nil {
		return reconcile.Result{}, err
	}

	/////////////////////// Resmon ///////////////////////
	resmon := engines.NewResmon(instance)
	if err := controllerutil.SetControllerReference(instance, resmon.InternalService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, resmon.ExposedService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, resmon.Deployment, r.scheme); err != nil {
		return reconcile.Result{}, err
	}

	if _, err := r.reconcileService(resmon.InternalService, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileService(resmon.ExposedService, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileDeployment(resmon.Deployment, reqLogger); err != nil {
		return reconcile.Result{}, err
	}

	/////////////////////// Simbank ///////////////////////
	simbank := simbank.New(instance)
	if err := controllerutil.SetControllerReference(instance, simbank.ExposedService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, simbank.Deployment, r.scheme); err != nil {
		return reconcile.Result{}, err
	}

	if _, err := r.reconcileService(simbank.ExposedService, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileDeployment(simbank.Deployment, reqLogger); err != nil {
		return reconcile.Result{}, err
	}

	/////////////////////// Monitoring ///////////////////////
	metrics := monitoring.NewMetrics(instance)
	grafana := monitoring.NewGrafana(instance)
	prometheus := monitoring.NewPrometheus(instance)
	if err := controllerutil.SetControllerReference(instance, metrics.InternalService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, metrics.ExposedService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, metrics.Deployment, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, grafana.InternalService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, grafana.ExposedService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, grafana.Deployment, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, grafana.ConfigMap, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, grafana.AutoDashboardConfigMap, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, grafana.DashboardConfigMap, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, grafana.ProvisioningConfigMap, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, grafana.PersistentVolumeClaim, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, prometheus.ConfigMap, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, prometheus.Deployment, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, prometheus.ExposedService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, prometheus.InternalService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, prometheus.PersistentVolumeClaim, r.scheme); err != nil {
		return reconcile.Result{}, err
	}

	if _, err := r.reconcileService(metrics.InternalService, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileService(metrics.ExposedService, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileDeployment(metrics.Deployment, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileService(grafana.ExposedService, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileService(grafana.InternalService, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileConfigMap(grafana.ConfigMap, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileConfigMap(grafana.DashboardConfigMap, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileConfigMap(grafana.AutoDashboardConfigMap, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileConfigMap(grafana.ProvisioningConfigMap, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcilePersistentVolume(grafana.PersistentVolumeClaim, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileDeployment(grafana.Deployment, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileConfigMap(prometheus.ConfigMap, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileService(prometheus.InternalService, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileService(prometheus.ExposedService, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcilePersistentVolume(prometheus.PersistentVolumeClaim, reqLogger); err != nil {
		return reconcile.Result{}, err
	}
	if _, err := r.reconcileDeployment(prometheus.Deployment, reqLogger); err != nil {
		return reconcile.Result{}, err
	}

	return reconcile.Result{}, nil
}

func (r *ReconcileGalasaEcosystem) reconcileDeployment(deployment *appsv1.Deployment, reqLogger logr.Logger) (reconcile.Result, error) {
	err := r.client.Get(context.TODO(), types.NamespacedName{Name: deployment.Name, Namespace: deployment.Namespace}, deployment)
	if err != nil && errors.IsNotFound(err) {
		reqLogger.Info("Creating the Deployment", "Namespace", deployment, "Name", deployment.Name)
		err = r.client.Create(context.TODO(), deployment)
		if err != nil {
			return reconcile.Result{}, err
		}
		return reconcile.Result{}, nil
	} else if err != nil {
		return reconcile.Result{}, err
	}

	// Service already exists - don't requeue
	reqLogger.Info("Skip reconcile: Deployment already exists", "Deployment.Namespace", deployment.Namespace, "Deployment.Name", deployment.Name)
	return reconcile.Result{}, nil
}

func (r *ReconcileGalasaEcosystem) reconcilePersistentVolume(pvc *corev1.PersistentVolumeClaim, reqLogger logr.Logger) (reconcile.Result, error) {
	err := r.client.Get(context.TODO(), types.NamespacedName{Name: pvc.Name, Namespace: pvc.Namespace}, pvc)
	if err != nil && errors.IsNotFound(err) {
		reqLogger.Info("Creating the PVC", "Namespace", pvc, "Name", pvc.Name)
		err = r.client.Create(context.TODO(), pvc)
		if err != nil {
			return reconcile.Result{}, err
		}
		return reconcile.Result{}, nil
	} else if err != nil {
		return reconcile.Result{}, err
	}

	// Service already exists - don't requeue
	reqLogger.Info("Skip reconcile: PVC already exists", "PVC.Namespace", pvc.Namespace, "PVC.Name", pvc.Name)
	return reconcile.Result{}, nil
}

func (r *ReconcileGalasaEcosystem) reconcileConfigMap(configMap *corev1.ConfigMap, reqLogger logr.Logger) (reconcile.Result, error) {
	err := r.client.Get(context.TODO(), types.NamespacedName{Name: configMap.Name, Namespace: configMap.Namespace}, configMap)
	if err != nil && errors.IsNotFound(err) {
		reqLogger.Info("Creating the Service", "Namespace", configMap, "Name", configMap.Name)
		err = r.client.Create(context.TODO(), configMap)
		if err != nil {
			return reconcile.Result{}, err
		}
		return reconcile.Result{}, nil
	} else if err != nil {
		return reconcile.Result{}, err
	}

	// Service already exists - don't requeue
	reqLogger.Info("Skip reconcile: ConfigMap already exists", "ConfigMap.Namespace", configMap.Namespace, "ConfigMap.Name", configMap.Name)
	return reconcile.Result{}, nil
}

func (r *ReconcileGalasaEcosystem) reconcileService(service *corev1.Service, reqLogger logr.Logger) (reconcile.Result, error) {
	err := r.client.Get(context.TODO(), types.NamespacedName{Name: service.Name, Namespace: service.Namespace}, service)
	if err != nil && errors.IsNotFound(err) {
		reqLogger.Info("Creating the Service", "Namespace", service, "Name", service.Name)
		err = r.client.Create(context.TODO(), service)
		if err != nil {
			return reconcile.Result{}, err
		}
		return reconcile.Result{}, nil
	} else if err != nil {
		return reconcile.Result{}, err
	}

	// Service already exists - don't requeue
	reqLogger.Info("Skip reconcile: Service already exists", "Service.Namespace", service.Namespace, "Service.Name", service.Name)
	return reconcile.Result{}, nil
}

func (r *ReconcileGalasaEcosystem) reconcileStatefulSet(statefulSet *appsv1.StatefulSet, reqLogger logr.Logger) (reconcile.Result, error) {
	err := r.client.Get(context.TODO(), types.NamespacedName{Name: statefulSet.Name, Namespace: statefulSet.Namespace}, statefulSet)
	if err != nil && errors.IsNotFound(err) {
		reqLogger.Info("Creating the Service", "Namespace", statefulSet, "Name", statefulSet.Name)
		err = r.client.Create(context.TODO(), statefulSet)
		if err != nil {
			return reconcile.Result{}, err
		}
		return reconcile.Result{}, nil
	} else if err != nil {
		return reconcile.Result{}, err
	}

	// Service already exists - don't requeue
	reqLogger.Info("Skip reconcile: Service already exists", "Service.Namespace", statefulSet.Namespace, "Service.Name", statefulSet.Name)
	return reconcile.Result{}, nil
}
