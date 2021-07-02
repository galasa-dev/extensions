package galasacps

import (
	"context"
	"time"

	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/apis/galasa/v1alpha1"
	"github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/cps"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
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

var log = logf.Log.WithName("controller_galasacps")

/**
* USER ACTION REQUIRED: This is a scaffold file intended for the user to modify with their own Controller
* business logic.  Delete these comments after modifying this file.*
 */

// Add creates a new GalasaCps Controller and adds it to the Manager. The Manager will set fields on the Controller
// and Start it when the Manager is Started.
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

// newReconciler returns a new reconcile.Reconciler
func newReconciler(mgr manager.Manager) reconcile.Reconciler {
	return &ReconcileGalasaCps{client: mgr.GetClient(), scheme: mgr.GetScheme()}
}

// add adds a new Controller to mgr with r as the reconcile.Reconciler
func add(mgr manager.Manager, r reconcile.Reconciler) error {
	// Create a new controller
	c, err := controller.New("galasacps-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	// Watch for changes to primary resource GalasaCps
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

// blank assignment to verify that ReconcileGalasaCps implements reconcile.Reconciler
var _ reconcile.Reconciler = &ReconcileGalasaCps{}

// ReconcileGalasaCps reconciles a GalasaCps object
type ReconcileGalasaCps struct {
	// This client, initialized using mgr.Client() above, is a split client
	// that reads objects from the cache and writes to the apiserver
	client client.Client
	scheme *runtime.Scheme
}

func (r *ReconcileGalasaCps) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
	reqLogger.Info("Reconciling GalasaCps")

	// Fetch the GalasaCps instance
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

	cpsCluster := cps.New(instance)
	newCpsPvc := false
	reqLogger.Info("Check operator controller for CPS resource")
	if err := controllerutil.SetControllerReference(instance, cpsCluster.StatefulSet, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, cpsCluster.InternalService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, cpsCluster.ExposedService, r.scheme); err != nil {
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
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: cpsCluster.PersistentVol.Name, Namespace: cpsCluster.PersistentVol.Namespace}, cpsCluster.PersistentVol)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), cpsCluster.PersistentVol)
		if err != nil {
			reqLogger.Error(err, "Failed to Create CPS PVC.", "Name", cpsCluster.PersistentVol.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: cpsCluster.StatefulSet.Name, Namespace: cpsCluster.StatefulSet.Namespace}, cpsCluster.StatefulSet)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), cpsCluster.StatefulSet)
		if err != nil {
			reqLogger.Error(err, "Failed to Create CPS Statefulset.", "Name", cpsCluster.StatefulSet.Name)
			return reconcile.Result{}, err
		}
	}
	instance.Status.CPSReadyReplicas = cpsCluster.StatefulSet.Status.ReadyReplicas
	r.client.Update(context.TODO(), instance)
	if instance.Status.CPSReadyReplicas != instance.Spec.Propertystore.PropertyClusterSize {
		reqLogger.Info("Waiting for CPS to become ready", "Current ready replicas", cpsCluster.StatefulSet.Status.ReadyReplicas)
		return reconcile.Result{RequeueAfter: time.Second * 5, Requeue: true}, nil
	}

	err = r.client.Get(context.TODO(), types.NamespacedName{Name: cpsCluster.ExposedService.Name, Namespace: cpsCluster.ExposedService.Namespace}, cpsCluster.ExposedService)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), cpsCluster.ExposedService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create CPS Service.", "Name", cpsCluster.ExposedService.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: cpsCluster.InternalService.Name, Namespace: cpsCluster.InternalService.Namespace}, cpsCluster.InternalService)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), cpsCluster.InternalService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create CPS Service.", "Name", cpsCluster.InternalService.Name)
			return reconcile.Result{}, err
		}
	}

	// Check all updateable fields from the GalasaEcosystem CRD.
	clusterSize := instance.Spec.Propertystore.PropertyClusterSize
	if *cpsCluster.StatefulSet.Spec.Replicas != clusterSize {
		cpsCluster.StatefulSet.Spec.Replicas = &clusterSize
		err = r.client.Update(context.TODO(), cpsCluster.StatefulSet)
		if err != nil {
			reqLogger.Error(err, "Failed to update CPS Stateful set.", "Name", cpsCluster.StatefulSet.Name)
			return reconcile.Result{}, err
		}
	}

	// // Wait for CPS to be ready
	// r.client.Get(context.TODO(), types.NamespacedName{Name: cpsCluster.StatefulSet.GetName(), Namespace: cpsCluster.StatefulSet.GetNamespace()}, cpsCluster.StatefulSet)
	// if cpsCluster.StatefulSet.Status.ReadyReplicas < 1 {
	// 	reqLogger.Info("Waiting for CPS to become ready", "Current ready replicas", cpsCluster.StatefulSet.Status.ReadyReplicas)
	// 	return reconcile.Result{RequeueAfter: time.Second * 5, Requeue: true}, nil
	// }

	// Load Init Props from Ecosystem Spec
	if newCpsPvc {
		reqLogger.Info("Loading Init Props")
		if err := cpsCluster.LoadInitProps(instance); err != nil {
			return reconcile.Result{}, err
		}
		newCpsPvc = false
	}

	// Load DSS and CREDS location
	nodePort := getNodePort(cpsCluster.ExposedService, "etcd-client")
	cpsURI := "etcd:" + instance.Spec.ExternalHostname + ":" + nodePort
	cpsCluster.LoadProp("framework.dynamicstatus.store", cpsURI)
	cpsCluster.LoadProp("framework.credentials.store", cpsURI)

	instance.Status.CPSURL = instance.Spec.ExternalHostname + ":" + nodePort
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
