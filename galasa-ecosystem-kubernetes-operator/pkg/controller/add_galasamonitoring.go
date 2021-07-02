package controller

import (
	"github.com/galasa-dev/extensions/galasa-ecosystem-kubernetes-operator/pkg/controller/galasamonitoring"
)

func init() {
	// AddToManagerFuncs is a list of functions to create controllers and add them to a manager.
	AddToManagerFuncs = append(AddToManagerFuncs, galasamonitoring.Add)
}
