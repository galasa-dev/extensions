package galasaecosystem

import (
	"context"

	galasav1alpha1 "github.com/extensions/galasa-ecosystem-operator/pkg/apis/galasa/v1alpha1"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/intstr"
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

	statefulSet := cpsStatefulSet(instance)

	if err := controllerutil.SetControllerReference(instance, statefulSet, r.scheme); err != nil {
		return reconcile.Result{}, err
	}

	found := &appsv1.StatefulSet{}

	err = r.client.Get(context.TODO(), types.NamespacedName{Name: statefulSet.Name, Namespace: statefulSet.Namespace}, found)
	if err != nil && errors.IsNotFound(err) {
		reqLogger.Info("Creating the CPS stateful set", "Namespace", statefulSet.Namespace, "Name", statefulSet.Name)
		err = r.client.Create(context.TODO(), statefulSet)
		if err != nil {
			return reconcile.Result{}, err
		}

		// StatefulSet created successfully - don't requeue
		return reconcile.Result{}, nil
	} else if err != nil {
		return reconcile.Result{}, err
	}

	// Pod already exists - don't requeue
	reqLogger.Info("Skip reconcile: StatfulSet already exists", "StatefulSet.Namespace", found.Namespace, "StatefulSet.Name", found.Name)
	return reconcile.Result{}, nil
}

// newPodForCR returns a busybox pod with the same name/namespace as the cr

func cpsStatefulSet(cr *galasav1alpha1.GalasaEcosystem) *appsv1.StatefulSet {
	labels := map[string]string{
		"app": cr.Name + "-cps",
	}
	return &appsv1.StatefulSet{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-cps",
			Namespace: cr.Namespace,
			Labels:    labels,
		},
		Spec: appsv1.StatefulSetSpec{
			ServiceName: cr.Name + "-service",
			Replicas:    cr.Spec.Propertystore.Replicas,
			Selector: &metav1.LabelSelector{
				MatchLabels: labels,
			},
			Template: corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name:   cr.Name + "-cps",
					Labels: labels,
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name:            "etcd",
							Image:           "quay.io/coreos/etcd:v3.4.3",
							ImagePullPolicy: "IfNotPresent",
							Ports: []corev1.ContainerPort{
								{
									Name:          "peer",
									ContainerPort: int32(2379),
								},
								{
									Name:          "client",
									ContainerPort: int32(2380),
								},
							},
							LivenessProbe: &corev1.Probe{
								InitialDelaySeconds: 60,
								PeriodSeconds:       60,
								Handler: corev1.Handler{
									TCPSocket: &corev1.TCPSocketAction{
										Port: intstr.FromInt(2379),
									},
								},
							},
							Env: []corev1.EnvVar{
								{
									Name:  "INITIAL_CLUSTER_SIZE",
									Value: "1",
								},
								{
									Name:  "SET_NAME",
									Value: "cps",
								},
							},
							VolumeMounts: []corev1.VolumeMount{
								{
									Name:      "datadir",
									MountPath: "/var/run/etcd",
								},
							},
							Lifecycle: &corev1.Lifecycle{
								PreStop: &corev1.Handler{
									Exec: &corev1.ExecAction{
										Command: []string{
											"/bin/sh",
											"-ec",
											`
											EPS=""
											for i in $(seq 0 $((${INITIAL_CLUSTER_SIZE} - 1))); do
												EPS="${EPS}${EPS:+,}http://${SET_NAME}-${i}.${SET_NAME}:2379"
											done

											HOSTNAME=$(hostname)

											member_hash() {
												etcdctl member list | grep http://${HOSTNAME}.${SET_NAME}:2380 | cut -d':' -f1 | cut -d'[' -f1
											}

											SET_ID=${HOSTNAME##*[^0-9]}

											if [ "${SET_ID}" -ge ${INITIAL_CLUSTER_SIZE} ]; then
												echo "Removing ${HOSTNAME} from etcd cluster"
												ETCDCTL_ENDPOINT=${EPS} etcdctl member remove $(member_hash)
												if [ $? -eq 0 ]; then
													# Remove everything otherwise the cluster will no longer scale-up
													rm -rf /var/run/etcd/*
												fi
											fi
											`,
										},
									},
								},
							},
							Command: []string{
								"/bin/sh",
								"-ec",
								`
								HOSTNAME=$(hostname)

								# store member id into PVC for later member replacement
								collect_member() {
									while ! etcdctl member list &>/dev/null; do sleep 1; done
									etcdctl member list | grep http://${HOSTNAME}.${SET_NAME}:2380 | cut -d':' -f1 | cut -d'[' -f1 > /var/run/etcd/member_id
									exit 0
								}

								eps() {
									EPS=""
									for i in $(seq 0 $((${INITIAL_CLUSTER_SIZE} - 1))); do
										EPS="${EPS}${EPS:+,}http://${SET_NAME}-${i}.${SET_NAME}:2379"
									done
									echo ${EPS}
								}

								member_hash() {
									etcdctl member list | grep http://${HOSTNAME}.${SET_NAME}:2380 | cut -d':' -f1 | cut -d'[' -f1
								}

								# we should wait for other pods to be up before trying to join
								# otherwise we got "no such host" errors when trying to resolve other members
								for i in $(seq 0 $((${INITIAL_CLUSTER_SIZE} - 1))); do
									while true; do
										echo "Waiting for ${SET_NAME}-${i}.${SET_NAME} to come up"
										ping -W 1 -c 1 ${SET_NAME}-${i}.${SET_NAME} > /dev/null && break
										sleep 1s
									done
								done

								# re-joining after failure?
								if [ -e /var/run/etcd/default.etcd ]; then
									echo "Re-joining etcd member"
									member_id=$(cat /var/run/etcd/member_id)

									# re-join member
									ETCDCTL_ENDPOINT=$(eps) etcdctl member update ${member_id} http://${HOSTNAME}.${SET_NAME}:2380 | true
									exec etcd --name ${HOSTNAME} \
										--listen-peer-urls http://0.0.0.0:2380 \
										--listen-client-urls http://0.0.0.0:2379\
										--advertise-client-urls http://${HOSTNAME}.${SET_NAME}:2379 \
										--data-dir /var/run/etcd/default.etcd
								fi

								# etcd-SET_ID
								SET_ID=${HOSTNAME##*[^0-9]}

								# adding a new member to existing cluster (assuming all initial pods are available)
								if [ "${SET_ID}" -ge ${INITIAL_CLUSTER_SIZE} ]; then
									export ETCDCTL_ENDPOINT=$(eps)

									# member already added?
									MEMBER_HASH=$(member_hash)
									if [ -n "${MEMBER_HASH}" ]; then
										# the member hash exists but for some reason etcd failed
										# as the datadir has not be created, we can remove the member
										# and retrieve new hash
										etcdctl member remove ${MEMBER_HASH}
									fi

									echo "Adding new member"
									etcdctl member add ${HOSTNAME} http://${HOSTNAME}.${SET_NAME}:2380 | grep "^ETCD_" > /var/run/etcd/new_member_envs

									if [ $? -ne 0 ]; then
										echo "Exiting"
										rm -f /var/run/etcd/new_member_envs
										exit 1
									fi

									cat /var/run/etcd/new_member_envs
									source /var/run/etcd/new_member_envs

									collect_member &

									exec etcd --name ${HOSTNAME} \
										--listen-peer-urls http://0.0.0.0:2380 \
										--listen-client-urls http://0.0.0.0:2379 \
										--advertise-client-urls http://${HOSTNAME}.${SET_NAME}:2379 \
										--data-dir /var/run/etcd/default.etcd \
										--initial-advertise-peer-urls http://${HOSTNAME}.${SET_NAME}:2380 \
										--initial-cluster ${ETCD_INITIAL_CLUSTER} \
										--initial-cluster-state ${ETCD_INITIAL_CLUSTER_STATE}
								fi

								PEERS=""
								for i in $(seq 0 $((${INITIAL_CLUSTER_SIZE} - 1))); do
									PEERS="${PEERS}${PEERS:+,}${SET_NAME}-${i}=http://${SET_NAME}-${i}.${SET_NAME}:2380"
								done

								collect_member &

								# join member
								exec etcd --name ${HOSTNAME} \
									--initial-advertise-peer-urls http://${HOSTNAME}.${SET_NAME}:2380 \
									--listen-peer-urls http://0.0.0.0:2380 \
									--listen-client-urls http://0.0.0.0:2379 \
									--advertise-client-urls http://${HOSTNAME}.${SET_NAME}:2379 \
									--initial-cluster-token etcd-cluster-1 \
									--initial-cluster ${PEERS} \
									--initial-cluster-state new \
									--data-dir /var/run/etcd/default.etcd
								`,
							},
						},
					},
				},
			},
			VolumeClaimTemplates: []corev1.PersistentVolumeClaim{
				{
					ObjectMeta: metav1.ObjectMeta{
						Name: "datadir",
					},
					Spec: corev1.PersistentVolumeClaimSpec{
						AccessModes: []corev1.PersistentVolumeAccessMode{
							"ReadWriteOnce",
						},
						Resources: corev1.ResourceRequirements{
							Requests: corev1.ResourceList{
								corev1.ResourceName(corev1.ResourceStorage): resource.MustParse("200m"),
							},
						},
					},
				},
			},
		},
	}
}
