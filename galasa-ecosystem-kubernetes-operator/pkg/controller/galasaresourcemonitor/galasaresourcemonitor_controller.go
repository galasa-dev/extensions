package galasaresourcemonitor

import (
	"context"
	"time"

	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/apis/galasa/v1alpha1"
	"github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/engines"
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

var log = logf.Log.WithName("controller_galasaresourcemonitor")

/**
* USER ACTION REQUIRED: This is a scaffold file intended for the user to modify with their own Controller
* business logic.  Delete these comments after modifying this file.*
 */

// Add creates a new GalasaResourceMonitor Controller and adds it to the Manager. The Manager will set fields on the Controller
// and Start it when the Manager is Started.
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

// newReconciler returns a new reconcile.Reconciler
func newReconciler(mgr manager.Manager) reconcile.Reconciler {
	return &ReconcileGalasaResourceMonitor{client: mgr.GetClient(), scheme: mgr.GetScheme()}
}

// add adds a new Controller to mgr with r as the reconcile.Reconciler
func add(mgr manager.Manager, r reconcile.Reconciler) error {
	// Create a new controller
	c, err := controller.New("galasaresourcemonitor-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	// Watch for changes to primary resource GalasaResourceMonitor
	err = c.Watch(&source.Kind{Type: &galasav1alpha1.GalasaEcosystem{}}, &handler.EnqueueRequestForObject{})
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
	err = c.Watch(&source.Kind{Type: &corev1.Service{}}, &handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &galasav1alpha1.GalasaEcosystem{},
	})
	if err != nil {
		return err
	}

	return nil
}

// blank assignment to verify that ReconcileGalasaResourceMonitor implements reconcile.Reconciler
var _ reconcile.Reconciler = &ReconcileGalasaResourceMonitor{}

// ReconcileGalasaResourceMonitor reconciles a GalasaResourceMonitor object
type ReconcileGalasaResourceMonitor struct {
	// This client, initialized using mgr.Client() above, is a split client
	// that reads objects from the cache and writes to the apiserver
	client client.Client
	scheme *runtime.Scheme
}

// Reconcile reads that state of the cluster for a GalasaResourceMonitor object and makes changes based on the state read
// and what is in the GalasaResourceMonitor.Spec
// TODO(user): Modify this Reconcile function to implement your Controller logic.  This example creates
// a Pod as an example
// Note:
// The Controller will requeue the Request to be processed again if the returned error is non-nil or
// Result.Requeue is true, otherwise upon completion it will remove the work from the queue.
func (r *ReconcileGalasaResourceMonitor) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
	reqLogger.Info("Reconciling GalasaResourceMonitor")

	// Fetch the GalasaResourceMonitor instance
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

	if instance.Status.APIReadyReplicas < 1 {
		return reconcile.Result{RequeueAfter: time.Second * 5, Requeue: true}, nil
	}

	if instance.Status.BootstrapURL == "" {
		return reconcile.Result{RequeueAfter: time.Second * 5, Requeue: true}, nil
	}

	resmon := engines.NewResmon(instance)
	if err := controllerutil.SetControllerReference(instance, resmon.Deployment, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, resmon.InternalService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, resmon.ExposedService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}

	// Create all Resmon resources if not exisisting
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: resmon.Deployment.Name, Namespace: resmon.Deployment.Namespace}, resmon.Deployment)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), resmon.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Resmon Deployment.", "Name", resmon.Deployment.Name)
			return reconcile.Result{}, err
		}
	}
	instance.Status.ResmonReadyReplicas = resmon.Deployment.Status.ReadyReplicas
	r.client.Update(context.TODO(), instance)
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: resmon.InternalService.Name, Namespace: resmon.InternalService.Namespace}, resmon.InternalService)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), resmon.InternalService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Resmon Service.", "Name", resmon.InternalService.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: resmon.ExposedService.Name, Namespace: resmon.ExposedService.Namespace}, resmon.ExposedService)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), resmon.ExposedService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Resmon Service.", "Name", resmon.ExposedService.Name)
			return reconcile.Result{}, err
		}
	}

	// Check all updateable fields from the GalasaEcosystem CRD.
	if *resmon.Deployment.Spec.Replicas != *instance.Spec.EngineResmon.Replicas {
		resmon.Deployment.Spec.Replicas = instance.Spec.EngineResmon.Replicas
		err = r.client.Update(context.TODO(), resmon.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to update Resmon Deployment.", "Deployment.Name", resmon.Deployment.Name)
			return reconcile.Result{}, err
		}
	}

	instance.Status.ResmonReadyReplicas = resmon.Deployment.Status.ReadyReplicas
	return reconcile.Result{}, nil
}
