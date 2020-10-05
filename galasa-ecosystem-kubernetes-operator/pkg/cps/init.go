package cps

import (
	"bytes"
	"context"

	galasav1alpha1 "github.com/galasa-dev/extensions/galasa-ecosystem-operator/pkg/apis/galasa/v1alpha1"

	corev1 "k8s.io/api/core/v1"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/remotecommand"
	"k8s.io/kubectl/pkg/scheme"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
)

var log = logf.Log.WithName("controller_galasaecosystem")

func (c *CPS) LoadInitProps(cr *galasav1alpha1.GalasaEcosystem) error {
	initProps := cr.Spec.Propertystore.InitProps
	if initProps == nil {
		return nil
	}
	log.Info("Found props:", "props", initProps)
	for k, v := range initProps {
		log.Info("Prop:", "key", k, "value", v)
		if err := c.LoadProp(k, v); err != nil {
			return err
		}
	}
	return nil
}

func (c *CPS) LoadProp(key string, value string) error {
	cmd := []string{"etcdctl put " + key + " " + value}
	_, err := c.execCmd(cmd)
	return err

}

func (c *CPS) GetProp(key string) (value string, err error) {
	cmd := []string{"etcdctl get " + key}
	v, execErr := c.execCmd(cmd)
	if execErr != nil {
		return "", err
	}
	return v, nil
}

func (c *CPS) execCmd(cmd []string) (stdO string, err error) {
	command := []string{"sh", "-c"}
	command = append(command, cmd...)

	config, err := rest.InClusterConfig()
	if err != nil {
		return "", err
	}
	clientSet, err := kubernetes.NewForConfig(config)
	if err != nil {
		return "", err
	}

	set := labels.Set(c.StatefulSet.Labels) //map[string]string{"app": "galasa-ecosystem-etcd-cluster"})
	pods, err := clientSet.CoreV1().Pods(c.StatefulSet.Namespace).List(context.TODO(), metav1.ListOptions{LabelSelector: set.AsSelector().String()})
	if err != nil {
		return "", err
	}
	log.Info("found pods", "First pod name", pods.Items[0].GetName())
	if len(pods.Items) < 1 {
		return "", &errorString{"No pods found"}
	}
	req := clientSet.CoreV1().RESTClient().Post().Resource("pods").Name(pods.Items[0].Name).Namespace(c.StatefulSet.Namespace).SubResource("exec")

	option := &corev1.PodExecOptions{
		Command: command,
		Stdin:   false,
		Stdout:  true,
		Stderr:  true,
		TTY:     true,
	}
	req.VersionedParams(
		option,
		scheme.ParameterCodec,
	)

	exec, err := remotecommand.NewSPDYExecutor(config, "POST", req.URL())
	if err != nil {
		log.Info("err", "err", err)
		return "", err
	}
	var stdout, stderr bytes.Buffer
	err = exec.Stream(remotecommand.StreamOptions{
		Stdin:  nil,
		Stdout: &stdout,
		Stderr: &stderr,
	})
	log.Info("Exec Done", "stdout", stdout, "stderr", stderr)

	if err != nil {
		log.Info("err", "err", err)
		return "", err
	}
	return stdout.String(), nil
}

type errorString struct {
	s string
}

func (e *errorString) Error() string {
	return e.s
}
