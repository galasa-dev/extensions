package galasasimbank

import (
	"bytes"
	"context"
	"io/ioutil"
	"net/http"
	"net/url"
	"strings"
	"time"

	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/apis/galasa/v1alpha1"
	"github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/cps"
	"github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/simbank"
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

var log = logf.Log.WithName("controller_galasasimbank")

/**
* USER ACTION REQUIRED: This is a scaffold file intended for the user to modify with their own Controller
* business logic.  Delete these comments after modifying this file.*
 */

// Add creates a new GalasaSimbank Controller and adds it to the Manager. The Manager will set fields on the Controller
// and Start it when the Manager is Started.
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

// newReconciler returns a new reconcile.Reconciler
func newReconciler(mgr manager.Manager) reconcile.Reconciler {
	return &ReconcileGalasaSimbank{client: mgr.GetClient(), scheme: mgr.GetScheme()}
}

// add adds a new Controller to mgr with r as the reconcile.Reconciler
func add(mgr manager.Manager, r reconcile.Reconciler) error {
	// Create a new controller
	c, err := controller.New("galasasimbank-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	// Watch for changes to primary resource GalasaSimbank
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

// blank assignment to verify that ReconcileGalasaSimbank implements reconcile.Reconciler
var _ reconcile.Reconciler = &ReconcileGalasaSimbank{}

// ReconcileGalasaSimbank reconciles a GalasaSimbank object
type ReconcileGalasaSimbank struct {
	// This client, initialized using mgr.Client() above, is a split client
	// that reads objects from the cache and writes to the apiserver
	client client.Client
	scheme *runtime.Scheme
}

// Reconcile reads that state of the cluster for a GalasaSimbank object and makes changes based on the state read
// and what is in the GalasaSimbank.Spec
// TODO(user): Modify this Reconcile function to implement your Controller logic.  This example creates
// a Pod as an example
// Note:
// The Controller will requeue the Request to be processed again if the returned error is non-nil or
// Result.Requeue is true, otherwise upon completion it will remove the work from the queue.
func (r *ReconcileGalasaSimbank) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
	reqLogger.Info("Reconciling GalasaSimbank")

	// Fetch the GalasaSimbank instance
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
	r.client.Update(context.TODO(), instance)
	simbank := simbank.New(instance)
	if err := controllerutil.SetControllerReference(instance, simbank.Deployment, r.scheme); err != nil {
		return reconcile.Result{}, err
	}
	if err := controllerutil.SetControllerReference(instance, simbank.ExposedService, r.scheme); err != nil {
		return reconcile.Result{}, err
	}

	// Create all Simbank resources if not exisisting
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: simbank.Deployment.Name, Namespace: simbank.Deployment.Namespace}, simbank.Deployment)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), simbank.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Simbank Deployment.", "Name", simbank.Deployment.Name)
			return reconcile.Result{}, err
		}
	}
	err = r.client.Get(context.TODO(), types.NamespacedName{Name: simbank.ExposedService.Name, Namespace: simbank.ExposedService.Namespace}, simbank.ExposedService)
	if err != nil && errors.IsNotFound(err) {
		err = r.client.Create(context.TODO(), simbank.ExposedService)
		if err != nil {
			reqLogger.Error(err, "Failed to Create Simbank Service.", "Name", simbank.ExposedService.Name)
			return reconcile.Result{}, err
		}
	}

	// Check all updateable fields from the GalasaEcosystem CRD.
	if *simbank.Deployment.Spec.Replicas != *instance.Spec.Simbank.Replicas {
		simbank.Deployment.Spec.Replicas = instance.Spec.Simbank.Replicas
		err = r.client.Update(context.TODO(), simbank.Deployment)
		if err != nil {
			reqLogger.Error(err, "Failed to update Simbank Deployment.", "Deployment.Name", simbank.Deployment.Name)
			return reconcile.Result{}, err
		}
	}

	if err := simbankSetup(instance, simbank); err != nil {
		return reconcile.Result{}, err
	}
	return reconcile.Result{}, nil

}

func simbankSetup(cr *galasav1alpha1.GalasaEcosystem, simbank *simbank.Simbank) error {
	simlog := log.WithName("SimLog")

	cps := cps.New(cr)
	required := false
	hostname, _ := url.Parse(cr.Spec.ExternalHostname)
	location := cr.Status.TestCatalogURL + "/simbank"
	simlog.Info("Test catatlog location", "location", location)
	streams, _ := cps.GetProp("framework.test.streams")
	simlog.Info("Streams", "Streams", streams)

	// Make sure the stream is define propertly for the ecosystem
	cps.LoadProp("framework.test.stream.SIMBANK.description", "Simbank tests")
	cps.LoadProp("framework.test.stream.SIMBANK.location", location)
	cps.LoadProp("framework.test.stream.SIMBANK.obr", "mvn:dev.galasa/dev.galasa.simbank.obr/"+cr.Spec.GalasaVersion+"/obr")
	cps.LoadProp("framework.test.stream.SIMBANK.repo", cr.Spec.MavenRepository)

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
		if hostname.Scheme == "http" {
			cps.LoadProp("zosmf.server.SIMBANK.https", "false")
		}

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
		simlog.Info("Checking", "err", err)
	}
	return nil
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
