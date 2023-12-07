/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal.dependencies.api;

import org.apache.commons.logging.Log;

// We have our own LogFactory interface, so that unit tests find it easy to inject their own logger, 
// and capture the logs made by the production code for checking.
public interface LogFactory {
    Log getLog(Class<?> clazz);
}
