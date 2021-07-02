package galasaras

import (
	"context"
	"time"

	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/apis/galasa/v1alpha1"
	"github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/cps"
	"github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/ras"
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

var log = logf.Log.WithName("controller_galasaras")

/**
* USER ACTION REQUIRED: This is a scaffold file intended for the user to modify with their own Controller
* business logic.  Delete these comments after modifying this file.*
 */

// Add creates a new GalasaRas Controller and adds it to the Manager. The Manager will set fields on the Controller
// and Start it when the Manager is Started.
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

// newReconciler returns a new reconcile.Reconciler
func newReconciler(mgr manager.Manager) reconcile.Reconciler {
	return &ReconcileGalasaRas{client: mgr.GetClient(), scheme: mgr.GetScheme()}
}

// add adds a new Controller to mgr with r as the reconcile.Reconciler
func add(mgr manager.Manager, r reconcile.Reconciler) error {
	// Create a new controller
	c, err := controller.New("galasaras-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	// Watch for changes to primary resource GalasaRas
	err = c.Watch(&source.Kind{Type: &galasav1alpha1.GalasaEcosystem{}}, &handler.EnqueueRequestForObject{})
	if err != nil {
		return err
	}

	err = c.Watch(&source.Kind{Type: &appsv1.StatefulSet{}}, &handler.EnqueueRequestForOwner{
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
	err = c.Watch(&source.Kind{Type: &corev1.Service{}}, &handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &galasav1alpha1.GalasaEcosystem{},
	})
	if err != nil {
		return err
	}

	return nil
}

// blank assignment to verify that ReconcileGalasaRas implements reconcile.Reconciler
var _ reconcile.Reconciler = &ReconcileGalasaRas{}

// ReconcileGalasaRas reconciles a GalasaRas object
type ReconcileGalasaRas struct {
	// This client, initialized using mgr.Client() above, is a split client
	// that reads objects from the cache and writes to the apiserver
	client client.Client
	scheme *runtime.Scheme
}

// Reconcile reads that state of the cluster for a GalasaRas object and makes changes based on the state read
// and what is in the GalasaRas.Spec
// TODO(user): Modify this Reconcile function to implement your Controller logic.  This example creates
// a Pod as an example
// Note:
// The Controller will requeue the Request to be processed again if the returned error is non-nil or
// Result.Requeue is true, otherwise upon completion it will remove the work from the queue.
func (r *ReconcileGalasaRas) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
	reqLogger.Info("Reconciling GalasaRas")

	// Fetch the GalasaRas instance
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

	// Is CPS ready?
	if instance.Status.CPSReadyReplicas < 1 {
		return reconcile.Result{RequeueAfter: time.Second * 5, Requeue: true}, nil
	}

	rasDB := ras.New(instance)
	if err := controllerutil.SetControllerReference(instance, rasDB.StatefulSet, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, rasDB.InternalService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, rasDB.ExposedService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}

	// Create all RAS resources if not exisisting
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: rasDB.PersistentVol.Name, Namespace: rasDB.PersistentVol.Namespace}, rasDB.PersistentVol)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), rasDB.PersistentVol)
		if err != nil {
			reqLogger.Error(err, "Failed to Create ras PVC.", "Name", rasDB.PersistentVol.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: rasDB.StatefulSet.Name, Namespace: rasDB.StatefulSet.Namespace}, rasDB.StatefulSet)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), rasDB.StatefulSet)
		if err != nil {
			reqLogger.Error(err, "Failed to Create RAS Statefulset.", "Name", rasDB.StatefulSet.Name)
			return reconcile.Result{}, err
		}
	}
	instance.Status.RASReadyReplicas = rasDB.StatefulSet.Status.ReadyReplicas
	r.client.Update(context.TODO(), instance)
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: rasDB.ExposedService.Name, Namespace: rasDB.ExposedService.Namespace}, rasDB.ExposedService)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), rasDB.ExposedService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create RAS Service.", "Name", rasDB.ExposedService.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: rasDB.InternalService.Name, Namespace: rasDB.InternalService.Namespace}, rasDB.InternalService)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), rasDB.InternalService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create RAS Service.", "Name", rasDB.InternalService.Name)
			return reconcile.Result{}, err
		}
	}

	// Check all updateable fields from the GalasaEcosystem CRD.
	if *rasDB.StatefulSet.Spec.Replicas != *instance.Spec.RasSpec.Replicas {
		rasDB.StatefulSet.Spec.Replicas = instance.Spec.RasSpec.Replicas
		err = r.client.Create(context.TODO(), rasDB.StatefulSet)
		if err != nil {
			reqLogger.Error(err, "Failed to update RAS Stateful set.", "Name", rasDB.StatefulSet.Name)
			return reconcile.Result{}, err
		}
	}

	if rasDB.StatefulSet.Status.ReadyReplicas < 1 {
		reqLogger.Info("Waiting for RAS to become ready", "Current ready replicas", rasDB.StatefulSet.Status.ReadyReplicas)
		return reconcile.Result{RequeueAfter: time.Second * 5, Requeue: true}, nil
	}

	cps := cps.New(instance)
	nodePort := getNodePort(rasDB.ExposedService, "couchdbport")
	cps.LoadProp("framework.resultarchive.store", "couchdb:"+instance.Spec.ExternalHostname+":"+nodePort)

	instance.Status.RASReadyReplicas = rasDB.StatefulSet.Status.ReadyReplicas
	r.client.Update(context.TODO(), instance)

	return reconcile.Result{}, nil
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
