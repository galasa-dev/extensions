package galasaecosystem

import (
	"context"
	"time"

	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/apis/galasa/v1alpha1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"sigs.k8s.io/controller-runtime/pkg/source"
)

var log = logf.Log.WithName("controller_galasaecosystem")
var newCpsPvc = false
var ecosystemReady = false

var apiReplicas int32
var controllerReplicas int32
var resmanReplicas int32
var metricsReplicas int32
var cpsReplicas int32
var rasReplicas int32

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
	err = c.Watch(&source.Kind{Type: &corev1.Pod{}}, &handler.EnqueueRequestForOwner{
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

// Ecosystem Reconcile is concerned with the ecosystem state and is responsible for reseting any of the services in the case
// of a problem.
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

	pods := &corev1.PodList{}
	r.client.List(context.TODO(), pods, &client.ListOptions{LabelSelector: client.MatchingLabelsSelector{labels.SelectorFromSet(labels.Set{"app": instance.Name + "-cps"})}})
	if len(pods.Items) > 0 {
		instance.Status.CPSReadyReplicas = 0
		for _, pod := range pods.Items {
			reqLogger.Info("Found pod: ", pod.Name, "Phase: ", string(pod.Status.Phase))
			if string(pod.Status.Phase) == "Running" {
				instance.Status.CPSReadyReplicas = instance.Status.CPSReadyReplicas + 1
			}
		}
	}

	r.client.List(context.TODO(), pods, &client.ListOptions{LabelSelector: client.MatchingLabelsSelector{labels.SelectorFromSet(labels.Set{"app": instance.Name + "-ras"})}})
	if len(pods.Items) > 0 {
		instance.Status.RASReadyReplicas = 0
		for _, pod := range pods.Items {
			reqLogger.Info("Found pod: ", pod.Name, "Phase: ", string(pod.Status.Phase))
			if string(pod.Status.Phase) == "Running" {
				instance.Status.RASReadyReplicas = instance.Status.RASReadyReplicas + 1
			}
		}
	}

	r.client.List(context.TODO(), pods, &client.ListOptions{LabelSelector: client.MatchingLabelsSelector{labels.SelectorFromSet(labels.Set{"app": instance.Name + "-engine-controller"})}})
	if len(pods.Items) > 0 {
		instance.Status.EngineControllerReadyReplicas = 0
		for _, pod := range pods.Items {
			reqLogger.Info("Found pod: ", pod.Name, "Phase: ", string(pod.Status.Phase))
			if string(pod.Status.Phase) == "Running" {
				instance.Status.EngineControllerReadyReplicas = instance.Status.EngineControllerReadyReplicas + 1
			}
		}
	}

	r.client.List(context.TODO(), pods, &client.ListOptions{LabelSelector: client.MatchingLabelsSelector{labels.SelectorFromSet(labels.Set{"app": instance.Name + "-apiserver"})}})
	if len(pods.Items) > 0 {
		instance.Status.APIReadyReplicas = 0
		for _, pod := range pods.Items {
			reqLogger.Info("Found pod: ", pod.Name, "Phase: ", string(pod.Status.Phase))
			if string(pod.Status.Phase) == "Running" {
				instance.Status.APIReadyReplicas = instance.Status.APIReadyReplicas + 1
			}
		}
	}

	r.client.List(context.TODO(), pods, &client.ListOptions{LabelSelector: client.MatchingLabelsSelector{labels.SelectorFromSet(labels.Set{"app": instance.Name + "-resource-monitor"})}})
	if len(pods.Items) > 0 {
		instance.Status.ResmonReadyReplicas = 0
		for _, pod := range pods.Items {
			reqLogger.Info("Found pod: ", pod.Name, "Phase: ", string(pod.Status.Phase))
			if string(pod.Status.Phase) == "Running" {
				instance.Status.ResmonReadyReplicas = instance.Status.ResmonReadyReplicas + 1
			}
		}
	}

	if instance.Status.EcosystemRestarting {
		return r.reset(instance, request)
	}

	if instance.Status.EcosystemReady {
		if instance.Status.APIReadyReplicas < 1 {
			return r.reset(instance, request)
		}

		if instance.Status.RASReadyReplicas < 1 {
			return r.reset(instance, request)
		}

		if instance.Status.CPSReadyReplicas < 1 {
			return r.reset(instance, request)
		}
	}
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

func ecosystemRestartCheck(instance *galasav1alpha1.GalasaEcosystem) bool {
	checkLog := log.WithName("Check-Log")
	checkLog.Info("Instance", "Instance", instance)
	if instance.Status.CPSReadyReplicas != cpsReplicas {
		checkLog.Info("Etcd cluster not ready", "Required", cpsReplicas, "Available", String(instance.Status.CPSReadyReplicas))
		return false
	}
	if instance.Status.RASReadyReplicas != rasReplicas {
		checkLog.Info("RAS not ready", "Required", rasReplicas, "Available", instance.Status.RASReadyReplicas)
		return false
	}
	if instance.Status.APIReadyReplicas != apiReplicas {
		checkLog.Info("API server not ready", "Required", apiReplicas, "Available", instance.Status.APIReadyReplicas)
		return false
	}
	if instance.Status.EngineControllerReadyReplicas != controllerReplicas {
		checkLog.Info("Engine Controller not ready", "Required", controllerReplicas, "Available", instance.Status.EngineControllerReadyReplicas)
		return false
	}
	if instance.Status.ResmonReadyReplicas != resmanReplicas {
		checkLog.Info("Resource Management not ready", "Required", resmanReplicas, "Available", instance.Status.ResmonReadyReplicas)
		return false
	}
	return true
}

func (r *ReconcileGalasaEcosystem) ecosystemReady(instance *galasav1alpha1.GalasaEcosystem) {
	ecosystemReady = ecosystemCheck(instance)
	instance.Status.EcosystemReady = ecosystemReady
	r.client.Update(context.TODO(), instance)
}

func (r *ReconcileGalasaEcosystem) ecosystemRestartingCheck(instance *galasav1alpha1.GalasaEcosystem) {
	ecosystemReady = ecosystemRestartCheck(instance)
	instance.Status.EcosystemReady = ecosystemReady
	r.client.Update(context.TODO(), instance)
}

func (r *ReconcileGalasaEcosystem) shutdownGalasaFrameworks(instance *galasav1alpha1.GalasaEcosystem, request reconcile.Request) (reconcile.Result, error) {
	zero := int32(0)
	instance.Spec.APIServer.Replicas = &zero
	instance.Spec.EngineController.Replicas = &zero
	instance.Spec.EngineResmon.Replicas = &zero
	instance.Spec.Monitoring.MetricsReplicas = &zero

	r.client.Update(context.TODO(), instance)
	return reconcile.Result{RequeueAfter: time.Second * 5, Requeue: true}, nil
}

func (r *ReconcileGalasaEcosystem) reset(instance *galasav1alpha1.GalasaEcosystem, request reconcile.Request) (reconcile.Result, error) {
	resetLog := log.WithName("Reset-Log")
	resetLog.Info("Replicas set to:", "api", apiReplicas, "enginecontroller", controllerReplicas, "resman", resmanReplicas, "metrics", metricsReplicas)
	r.ecosystemRestartingCheck(instance)

	if !instance.Status.EcosystemRestarting {
		resetLog.Info("Reseting ecosystem - Restarting all galasa frameworks")
		resetLog.Info("Reseting ecosystem - Shutdown all galasa frameworks")
		instance.Status.EcosystemReady = false
		instance.Status.EcosystemRestarting = true

		cpsReplicas = instance.Spec.Propertystore.PropertyClusterSize
		rasReplicas = *instance.Spec.RasSpec.Replicas
		apiReplicas = *instance.Spec.APIServer.Replicas
		controllerReplicas = *instance.Spec.EngineController.Replicas
		resmanReplicas = *instance.Spec.EngineResmon.Replicas
		metricsReplicas = *instance.Spec.Monitoring.MetricsReplicas

		return r.shutdownGalasaFrameworks(instance, request)
	}

	if *instance.Spec.APIServer.Replicas == 0 && *instance.Spec.EngineController.Replicas == 0 && *instance.Spec.EngineResmon.Replicas == 0 && *instance.Spec.Monitoring.MetricsReplicas == 0 {
		galasaPods := &corev1.PodList{}
		r.client.List(context.TODO(), galasaPods, &client.ListOptions{LabelSelector: client.MatchingLabelsSelector{labels.SelectorFromSet(labels.Set{"galasa": "running-framework"})}})
		if len(galasaPods.Items) > 0 {
			for _, pod := range galasaPods.Items {
				resetLog.Info("Found:" + pod.Name)
			}
			resetLog.Info("Reseting ecosystem - Waiting for all frameworks to stop")
			return reconcile.Result{RequeueAfter: time.Second * 5, Requeue: true}, nil
		}
	}

	resetLog.Info("Reseting ecosystem - Settings replicas back", "api", apiReplicas, "enginecontroller", controllerReplicas, "resman", resmanReplicas, "metrics", metricsReplicas)
	instance.Spec.APIServer.Replicas = &apiReplicas
	instance.Spec.EngineController.Replicas = &controllerReplicas
	instance.Spec.EngineResmon.Replicas = &resmanReplicas
	instance.Spec.Monitoring.MetricsReplicas = &metricsReplicas

	r.client.Update(context.TODO(), instance)
	if !instance.Status.EcosystemReady {
		resetLog.Info("Reseting ecosystem - Waiting for the ecosystem to become ready")
		return reconcile.Result{RequeueAfter: time.Second * 5, Requeue: true}, nil
	}
	resetLog.Info("Reseting ecosystem - Finished Restarting")
	instance.Status.EcosystemRestarting = false
	r.client.Update(context.TODO(), instance)

	return reconcile.Result{}, nil
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
