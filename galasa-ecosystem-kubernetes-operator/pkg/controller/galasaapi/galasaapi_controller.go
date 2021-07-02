package galasaapi

import (
	"context"
	"time"

	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/apis/galasa/v1alpha1"
	"github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/apiserver"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/api/extensions/v1beta1"
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

var log = logf.Log.WithName("controller_galasaapi")

/**
* USER ACTION REQUIRED: This is a scaffold file intended for the user to modify with their own Controller
* business logic.  Delete these comments after modifying this file.*
 */

// Add creates a new GalasaApi Controller and adds it to the Manager. The Manager will set fields on the Controller
// and Start it when the Manager is Started.
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

// newReconciler returns a new reconcile.Reconciler
func newReconciler(mgr manager.Manager) reconcile.Reconciler {
	return &ReconcileGalasaApi{client: mgr.GetClient(), scheme: mgr.GetScheme()}
}

// add adds a new Controller to mgr with r as the reconcile.Reconciler
func add(mgr manager.Manager, r reconcile.Reconciler) error {
	// Create a new controller
	c, err := controller.New("galasaapi-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	// Watch for changes to primary resource GalasaApi
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
	err = c.Watch(&source.Kind{Type: &corev1.ConfigMap{}}, &handler.EnqueueRequestForOwner{
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

// blank assignment to verify that ReconcileGalasaApi implements reconcile.Reconciler
var _ reconcile.Reconciler = &ReconcileGalasaApi{}

// ReconcileGalasaApi reconciles a GalasaApi object
type ReconcileGalasaApi struct {
	// This client, initialized using mgr.Client() above, is a split client
	// that reads objects from the cache and writes to the apiserver
	client client.Client
	scheme *runtime.Scheme
}

// Reconcile reads that state of the cluster for a GalasaApi object and makes changes based on the state read
// and what is in the GalasaApi.Spec
// TODO(user): Modify this Reconcile function to implement your Controller logic.  This example creates
// a Pod as an example
// Note:
// The Controller will requeue the Request to be processed again if the returned error is non-nil or
// Result.Requeue is true, otherwise upon completion it will remove the work from the queue.
func (r *ReconcileGalasaApi) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
	reqLogger.Info("Reconciling GalasaApi")

	// Fetch the GalasaApi instance
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

	if instance.Status.RASReadyReplicas < 1 {
		return reconcile.Result{RequeueAfter: time.Second * 5, Requeue: true}, nil
	}

	apiServer := apiserver.New(instance)
	if err := controllerutil.SetControllerReference(instance, apiServer.BootstrapConf, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, apiServer.Deployment, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, apiServer.InternalService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, apiServer.ExposedService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, apiServer.Ingress, r.scheme); err != nil {
		return reconcile.Result{}, err
	}

	// Create all API resources if not exisisting
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: apiServer.PersistentVol.Name, Namespace: apiServer.PersistentVol.Namespace}, apiServer.PersistentVol)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), apiServer.PersistentVol)
		if err != nil {
			reqLogger.Error(err, "Failed to Create API PVC.", "Name", apiServer.PersistentVol.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: apiServer.Deployment.Name, Namespace: apiServer.Deployment.Namespace}, apiServer.Deployment)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), apiServer.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to Create API Deployment.", "Name", apiServer.Deployment.Name)
			return reconcile.Result{}, err
		}
	}
	instance.Status.APIReadyReplicas = apiServer.Deployment.Status.ReadyReplicas

	r.client.Update(context.TODO(), instance)
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: apiServer.BootstrapConf.Name, Namespace: apiServer.BootstrapConf.Namespace}, apiServer.BootstrapConf)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), apiServer.BootstrapConf)
		if err != nil {
			reqLogger.Error(err, "Failed to Create API ConfigMap.", "Name", apiServer.BootstrapConf.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: apiServer.ExposedService.Name, Namespace: apiServer.ExposedService.Namespace}, apiServer.ExposedService)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), apiServer.ExposedService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create API Service.", "Name", apiServer.ExposedService.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: apiServer.InternalService.Name, Namespace: apiServer.InternalService.Namespace}, apiServer.InternalService)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), apiServer.InternalService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create API Service.", "Name", apiServer.InternalService.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: apiServer.Ingress.Name, Namespace: apiServer.Ingress.Namespace}, apiServer.Ingress)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), apiServer.Ingress)
		if err != nil {
			reqLogger.Error(err, "Failed to Create API Ingress.", "Name", apiServer.Ingress.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: apiServer.TestCatalog.Name, Namespace: apiServer.TestCatalog.Namespace}, apiServer.TestCatalog)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), apiServer.TestCatalog)
		if err != nil {
			reqLogger.Error(err, "Failed to Create API Test Catalog.", "Name", apiServer.TestCatalog.Name)
			return reconcile.Result{}, err
		}
	}

	// Check all updateable fields from the GalasaEcosystem CRD.
	if *apiServer.Deployment.Spec.Replicas != *instance.Spec.APIServer.Replicas {
		apiServer.Deployment.Spec.Replicas = instance.Spec.APIServer.Replicas
		err = r.client.Update(context.TODO(), apiServer.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to update API Deployment.", "Deployment.Name", apiServer.Deployment.Name)
			return reconcile.Result{}, err
		}
	}

	if apiServer.Deployment.Status.ReadyReplicas < 1 {
		reqLogger.Info("Waiting for the APIserver to become ready")
		return reconcile.Result{RequeueAfter: time.Second * 10, Requeue: true}, nil
	}

	instance.Status.APIReadyReplicas = apiServer.Deployment.Status.ReadyReplicas
	if instance.Spec.IngressHostname != "" {
		reqLogger.Info("Setting boostrap", "URL", instance.Spec.IngressHostname+"/bootstrap")
		instance.Status.BootstrapURL = instance.Spec.IngressHostname + "/bootstrap"
		instance.Status.TestCatalogURL = instance.Spec.IngressHostname + "/testcatalog"
	} else {
		reqLogger.Info("Setting boostrap", "URL", instance.Spec.ExternalHostname+":"+getNodePort(apiServer.ExposedService, "http"))
		instance.Status.BootstrapURL = instance.Spec.ExternalHostname + ":" + getNodePort(apiServer.ExposedService, "http") + "/bootstrap"
		instance.Status.TestCatalogURL = instance.Spec.ExternalHostname + ":" + getNodePort(apiServer.ExposedService, "http") + "/testcatalog"
	}

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
