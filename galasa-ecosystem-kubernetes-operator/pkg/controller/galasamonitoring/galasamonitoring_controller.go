package galasamonitoring

import (
	"context"
	"time"

	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/apis/galasa/v1alpha1"
	"github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/monitoring"
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

var log = logf.Log.WithName("controller_galasamonitoring")

/**
* USER ACTION REQUIRED: This is a scaffold file intended for the user to modify with their own Controller
* business logic.  Delete these comments after modifying this file.*
 */

// Add creates a new GalasaMonitoring Controller and adds it to the Manager. The Manager will set fields on the Controller
// and Start it when the Manager is Started.
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

// newReconciler returns a new reconcile.Reconciler
func newReconciler(mgr manager.Manager) reconcile.Reconciler {
	return &ReconcileGalasaMonitoring{client: mgr.GetClient(), scheme: mgr.GetScheme()}
}

// add adds a new Controller to mgr with r as the reconcile.Reconciler
func add(mgr manager.Manager, r reconcile.Reconciler) error {
	// Create a new controller
	c, err := controller.New("galasamonitoring-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	// Watch for changes to primary resource GalasaMonitoring
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

// blank assignment to verify that ReconcileGalasaMonitoring implements reconcile.Reconciler
var _ reconcile.Reconciler = &ReconcileGalasaMonitoring{}

// ReconcileGalasaMonitoring reconciles a GalasaMonitoring object
type ReconcileGalasaMonitoring struct {
	// This client, initialized using mgr.Client() above, is a split client
	// that reads objects from the cache and writes to the apiserver
	client client.Client
	scheme *runtime.Scheme
}

// Reconcile reads that state of the cluster for a GalasaMonitoring object and makes changes based on the state read
// and what is in the GalasaMonitoring.Spec
// TODO(user): Modify this Reconcile function to implement your Controller logic.  This example creates
// a Pod as an example
// Note:
// The Controller will requeue the Request to be processed again if the returned error is non-nil or
// Result.Requeue is true, otherwise upon completion it will remove the work from the queue.
func (r *ReconcileGalasaMonitoring) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
	reqLogger.Info("Reconciling GalasaMonitoring")

	// Fetch the GalasaMonitoring instance
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

	grafana := monitoring.NewGrafana(instance)
	if err := controllerutil.SetControllerReference(instance, grafana.ConfigMap, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, grafana.AutoDashboardConfigMap, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, grafana.DashboardConfigMap, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, grafana.Deployment, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, grafana.InternalService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, grafana.ExposedService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, grafana.ProvisioningConfigMap, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, grafana.Ingress, r.scheme); err != nil {
		return reconcile.Result{}, err
	}

	// Create all Grafana resources if not exisisting
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: grafana.PersistentVolumeClaim.Name, Namespace: grafana.PersistentVolumeClaim.Namespace}, grafana.PersistentVolumeClaim)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), grafana.PersistentVolumeClaim)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Grafana PVC.", "Name", grafana.PersistentVolumeClaim.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: grafana.Deployment.Name, Namespace: grafana.Deployment.Namespace}, grafana.Deployment)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), grafana.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Grafana Deployment.", "Name", grafana.Deployment.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: grafana.AutoDashboardConfigMap.Name, Namespace: grafana.AutoDashboardConfigMap.Namespace}, grafana.AutoDashboardConfigMap)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), grafana.AutoDashboardConfigMap)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Grafana ConfigMap.", "Name", grafana.AutoDashboardConfigMap.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: grafana.ConfigMap.Name, Namespace: grafana.ConfigMap.Namespace}, grafana.ConfigMap)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), grafana.ConfigMap)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Grafana ConfigMap.", "Name", grafana.ConfigMap.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: grafana.DashboardConfigMap.Name, Namespace: grafana.DashboardConfigMap.Namespace}, grafana.DashboardConfigMap)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), grafana.DashboardConfigMap)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Grafana ConfigMap.", "Name", grafana.DashboardConfigMap.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: grafana.ProvisioningConfigMap.Name, Namespace: grafana.ProvisioningConfigMap.Namespace}, grafana.ProvisioningConfigMap)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), grafana.ProvisioningConfigMap)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Grafana ConfigMap.", "Name", grafana.ProvisioningConfigMap.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: grafana.ExposedService.Name, Namespace: grafana.ExposedService.Namespace}, grafana.ExposedService)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), grafana.ExposedService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Grafana Service.", "Name", grafana.ExposedService.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: grafana.InternalService.Name, Namespace: grafana.InternalService.Namespace}, grafana.InternalService)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), grafana.InternalService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Grafana Service.", "Name", grafana.InternalService.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: grafana.Ingress.Name, Namespace: grafana.Ingress.Namespace}, grafana.Ingress)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), grafana.Ingress)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Grafana Ingress.", "Name", grafana.Ingress.Name)
			return reconcile.Result{}, err
		}
	}
	if instance.Spec.IngressHostname != "" {
		instance.Status.GrafanaURL = instance.Spec.IngressHostname + "/" + instance.Name + "-grafana"
	} else {
		instance.Status.GrafanaURL = instance.Spec.ExternalHostname + ":" + getNodePort(grafana.ExposedService, "grafana") + "/galasa-grafana"
	}

	// Check all updateable fields from the GalasaEcosystem CRD.
	if *grafana.Deployment.Spec.Replicas != *instance.Spec.Monitoring.GrafanaReplicas {
		grafana.Deployment.Spec.Replicas = instance.Spec.Monitoring.GrafanaReplicas
		err = r.client.Update(context.TODO(), grafana.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to update Grafana Deployment.", "Deployment.Name", grafana.Deployment.Name)
			return reconcile.Result{}, err
		}
	}

	prometheus := monitoring.NewPrometheus(instance)
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

	// Create all Prometheus resources if not exisisting
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: prometheus.PersistentVolumeClaim.Name, Namespace: prometheus.PersistentVolumeClaim.Namespace}, prometheus.PersistentVolumeClaim)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), prometheus.PersistentVolumeClaim)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Prometheus PVC.", "Name", prometheus.PersistentVolumeClaim.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: prometheus.Deployment.Name, Namespace: prometheus.Deployment.Namespace}, prometheus.Deployment)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), prometheus.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Prometheus Deployment.", "Name", prometheus.Deployment.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: prometheus.ConfigMap.Name, Namespace: prometheus.ConfigMap.Namespace}, prometheus.ConfigMap)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), prometheus.ConfigMap)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Prometheus ConfigMap.", "Name", prometheus.ConfigMap.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: prometheus.ExposedService.Name, Namespace: prometheus.ExposedService.Namespace}, prometheus.ExposedService)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), prometheus.ExposedService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Prometheus Service.", "Name", prometheus.ExposedService.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: prometheus.InternalService.Name, Namespace: prometheus.InternalService.Namespace}, prometheus.InternalService)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), prometheus.InternalService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Prometheus Service.", "Name", prometheus.InternalService.Name)
			return reconcile.Result{}, err
		}
	}

	// Check all updateable fields from the GalasaEcosystem CRD.
	if *prometheus.Deployment.Spec.Replicas != *instance.Spec.Monitoring.PrometheusReplicas {
		prometheus.Deployment.Spec.Replicas = instance.Spec.Monitoring.PrometheusReplicas
		err = r.client.Update(context.TODO(), prometheus.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to update Grafana Deployment.", "Deployment.Name", prometheus.Deployment.Name)
			return reconcile.Result{}, err
		}
	}

	metrics := monitoring.NewMetrics(instance)
	if err := controllerutil.SetControllerReference(instance, metrics.Deployment, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, metrics.ExposedService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, metrics.InternalService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}

	// Create all metrics resources if not exisisting
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: metrics.Deployment.Name, Namespace: metrics.Deployment.Namespace}, metrics.Deployment)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), metrics.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Prometheus Deployment.", "Name", metrics.Deployment.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: metrics.InternalService.Name, Namespace: metrics.InternalService.Namespace}, metrics.InternalService)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), metrics.InternalService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Prometheus Service.", "Name", metrics.InternalService.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: metrics.ExposedService.Name, Namespace: metrics.ExposedService.Namespace}, metrics.ExposedService)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), metrics.ExposedService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Prometheus Service.", "Name", metrics.ExposedService.Name)
			return reconcile.Result{}, err
		}
	}

	// Check all updateable fields from the GalasaEcosystem CRD.
	if *metrics.Deployment.Spec.Replicas != *instance.Spec.Monitoring.MetricsReplicas {
		metrics.Deployment.Spec.Replicas = instance.Spec.Monitoring.MetricsReplicas
		err = r.client.Update(context.TODO(), metrics.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to update Metrics Deployment.", "Deployment.Name", metrics.Deployment.Name)
			return reconcile.Result{}, err
		}
	}

	instance.Status.MonitoringReadyReplicas = metrics.Deployment.Status.ReadyReplicas + prometheus.Deployment.Status.ReadyReplicas + grafana.Deployment.Status.ReadyReplicas
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
