package cps

import (
	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/apis/galasa/v1alpha1"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
)

type CPS struct {
	InternalService *corev1.Service
	ExposedService  *corev1.Service
	StatefulSet     *appsv1.StatefulSet
}

func New(cr *galasav1alpha1.GalasaEcosystem) *CPS {
	//We can generate the objects but nor create them on the cluster
	return &CPS{
		InternalService: generateInternalService(cr),
		ExposedService:  generateExposedService(cr),
		StatefulSet:     generateStatefulSet(cr),
	}
}

func generateInternalService(cr *galasav1alpha1.GalasaEcosystem) *corev1.Service {
	return &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Annotations: map[string]string{
				"service.alpha.kubernetes.io/tolerate-unready-endpoints": "true",
			},
			Name:      cr.Name + "-cps-internal-service",
			Namespace: cr.Namespace,
			Labels: map[string]string{
				"app": cr.Name + "-cps",
			},
		},
		Spec: corev1.ServiceSpec{
			Ports: []corev1.ServicePort{
				{
					Name:       "etcd-server",
					TargetPort: intstr.FromInt(2379),
					Port:       2379,
				},
				{
					Name:       "etcd-client",
					TargetPort: intstr.FromInt(2380),
					Port:       2380,
				},
			},
			Selector: map[string]string{
				"app": cr.Name + "-cps",
			},
		},
	}
}

func generateExposedService(cr *galasav1alpha1.GalasaEcosystem) *corev1.Service {
	return &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-cps-external-service",
			Namespace: cr.Namespace,
		},
		Spec: corev1.ServiceSpec{
			Type: corev1.ServiceType(corev1.ServiceTypeNodePort),
			Ports: []corev1.ServicePort{
				{
					Name:       "etcd-client",
					TargetPort: intstr.FromInt(2379),
					Port:       2379,
				},
			},
			Selector: map[string]string{
				"app": cr.Name + "-cps",
			},
		},
	}
}

func generateStatefulSet(cr *galasav1alpha1.GalasaEcosystem) *appsv1.StatefulSet {
	labels := map[string]string{
		"app": cr.Name + "-cps",
	}
	trueBool := true
	return &appsv1.StatefulSet{
		ObjectMeta: metav1.ObjectMeta{
			Name:      cr.Name + "-cps",
			Namespace: cr.Namespace,
			Labels:    labels,
		},
		Spec: appsv1.StatefulSetSpec{
			ServiceName: cr.Name + "-cps-internal-service",
			Replicas:    &cr.Spec.Propertystore.PropertyClusterSize,
			Selector: &metav1.LabelSelector{
				MatchLabels: labels,
			},
			Template: corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name:   cr.Name + "-cps",
					Labels: labels,
				},
				Spec: corev1.PodSpec{
					NodeSelector: cr.Spec.Propertystore.NodeSelector,
					Containers: []corev1.Container{
						{
							Name:            "etcd",
							Image:           cr.Spec.Propertystore.PropertyStoreImageName + ":" + cr.Spec.Propertystore.PropertyStoreImageVersion,
							ImagePullPolicy: corev1.PullPolicy(cr.Spec.ImagePullPolicy),
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
									Value: String(cr.Spec.Propertystore.PropertyClusterSize),
								},
								{
									Name:  "SET_NAME",
									Value: cr.Name + "-cps",
								},
								{
									Name:  "SERVICE",
									Value: cr.Name + "-cps-internal-service",
								},
								{
									Name:  "ETCDCTL_API",
									Value: "3",
								},
							},
							VolumeMounts: []corev1.VolumeMount{
								{
									Name:      "cps-datadir",
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
												EPS="${EPS}${EPS:+,}http://${SET_NAME}-${i}.${SERVICE}:2379"
											done
											HOSTNAME=$(hostname)
											member_hash() {
												etcdctl member list | grep http://${HOSTNAME}:2380 | cut -d':' -f1 | cut -d'[' -f1
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
			
									#while ! etcdctl member list &>/dev/null; do sleep 1; done
									etcdctl member list | grep http://${HOSTNAME}:2380 | awk -F',' '{print $1}' > /var/run/etcd/member_id
									exit 0
								}
								eps() {
									EPS=""
									for i in $(seq 0 $((${INITIAL_CLUSTER_SIZE} - 1))); do
										EPS="${EPS}${EPS:+,}http://${SET_NAME}-${i}:2379"
									done
									echo ${EPS}
								}
								member_hash() {
									etcdctl member list | grep http://${HOSTNAME}:2380 | awk -F',' '{print $1}'
								}
								# we should wait for other pods to be up before trying to join
								# otherwise we got "no such host" errors when trying to resolve other members
								for i in $(seq 0 $((${INITIAL_CLUSTER_SIZE} - 1))); do
									while true; do
										echo "Waiting for ${SET_NAME}-${i}.${SERVICE} to come up"
										ping -W 1 -c 1 ${SET_NAME}-${i}.${SERVICE} > /dev/null && break
										sleep 1s
									done
								done
								# re-joining after failure?
								if [ -e /var/run/etcd/default.etcd ]; then
									echo "Re-joining etcd member"
									member_id=$(cat /var/run/etcd/member_id)
									# re-join member
									ETCDCTL_ENDPOINT=$(eps) etcdctl member update ${member_id} http://${HOSTNAME}.${SERVICE}:2380 | true
									exec etcd --name ${HOSTNAME} \
										--listen-peer-urls http://0.0.0.0:2380 \
										--listen-client-urls http://0.0.0.0:2379\
										--advertise-client-urls http://${HOSTNAME}.${SERVICE}:2379 \
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
									etcdctl member add ${HOSTNAME} http://${HOSTNAME}.${SERVICE}:2380 | grep "^ETCD_" > /var/run/etcd/new_member_envs
									if [ $? -ne 0 ]; then
										echo "Exiting"
										rm -f /var/run/etcd/new_member_envs
										exit 1
									fi
									cat /var/run/etcd/new_member_envs
									source /var/run/etcd/new_member_envs
									echo "Collect 1"
									collect_member &
									exec etcd --name ${HOSTNAME} \
										--listen-peer-urls http://0.0.0.0:2380 \
										--listen-client-urls http://0.0.0.0:2379 \
										--advertise-client-urls http://${HOSTNAME}.${SERVICE}:2379 \
										--data-dir /var/run/etcd/default.etcd \
										--initial-advertise-peer-urls http://${HOSTNAME}.${SERVICE}:2380 \
										--initial-cluster ${ETCD_INITIAL_CLUSTER} \
										--initial-cluster-state ${ETCD_INITIAL_CLUSTER_STATE}
								fi
								PEERS=""
								for i in $(seq 0 $((${INITIAL_CLUSTER_SIZE} - 1))); do
									PEERS="${PEERS}${PEERS:+,}${SET_NAME}-${i}=http://${SET_NAME}-${i}:2380"
								done
								echo "Collect 2"
								collect_member &
								# join member
								exec etcd --name ${HOSTNAME} \
									--initial-advertise-peer-urls http://${HOSTNAME}:2380 \
									--listen-peer-urls http://0.0.0.0:2380 \
									--listen-client-urls http://0.0.0.0:2379 \
									--advertise-client-urls http://${HOSTNAME}:2379 \
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
						Name: "cps-datadir",
						OwnerReferences: []metav1.OwnerReference{
							{
								APIVersion:         cr.APIVersion,
								Kind:               cr.Kind,
								Name:               cr.Name,
								Controller:         &trueBool,
								BlockOwnerDeletion: &trueBool,
								UID:                cr.UID,
							},
						},
					},
					Spec: corev1.PersistentVolumeClaimSpec{
						AccessModes: []corev1.PersistentVolumeAccessMode{
							"ReadWriteOnce",
						},
						StorageClassName: cr.Spec.StorageClassName,
						Resources: corev1.ResourceRequirements{
							Requests: corev1.ResourceList{
								corev1.ResourceName(corev1.ResourceStorage): resource.MustParse(cr.Spec.Propertystore.Storage),
							},
						},
					},
				},
			},
		},
	}
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
