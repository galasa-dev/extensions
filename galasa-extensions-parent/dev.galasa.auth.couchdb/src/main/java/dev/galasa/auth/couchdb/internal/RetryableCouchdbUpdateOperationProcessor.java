/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.auth.couchdb.internal;

import java.util.Random;

import dev.galasa.extensions.common.couchdb.CouchdbClashingUpdateException;
import dev.galasa.extensions.common.couchdb.CouchdbException;
import dev.galasa.framework.spi.utils.ITimeService;

import org.apache.commons.logging.Log;

/**
 * Allows a lambda function to be used, and that function will be retried a number of times before giving up.
 */
public class RetryableCouchdbUpdateOperationProcessor {

    public int MAX_ATTEMPTS_TO_GO_BEFORE_GIVE_UP = 10;
    private ITimeService timeService ;
    private Log logger;
    public interface RetryableCouchdbUpdateOperation {
        public void tryToUpdateCouchDb() throws CouchdbException;
    }


    public RetryableCouchdbUpdateOperationProcessor(ITimeService timeService) {
        this.timeService = timeService;
    }

    public void retryCouchDbUpdateOperation(RetryableCouchdbUpdateOperation retryableOperation) throws CouchdbException{
        int attemptsToGoBeforeGiveUp = MAX_ATTEMPTS_TO_GO_BEFORE_GIVE_UP;
        boolean isDone = false;

        while (!isDone) {

            try {
                retryableOperation.tryToUpdateCouchDb();

                isDone = true;
            } catch (CouchdbClashingUpdateException updateClashedEx) {

                logger.info("Clashing update detected. Backing off for a short time to avoid another clash immediately. ");

                waitForBackoffDelay(timeService);

                attemptsToGoBeforeGiveUp -= 1;
                if (attemptsToGoBeforeGiveUp == 0) {
                    throw new CouchdbException("Failed after " + Integer.toString(MAX_ATTEMPTS_TO_GO_BEFORE_GIVE_UP) + " attempts to update the design document in CouchDB due to conflicts.", updateClashedEx);
                } else {
                    logger.info("Failed to update CouchDB design document, retrying...");
                }
            }
        }
    }

    private void waitForBackoffDelay(ITimeService timeService) {
        Long delayMilliSecs = 1000L + new Random().nextInt(3000);

        try {
            logger.info("Waiting "+delayMilliSecs+" during a back-off delay. starting now.");
            timeService.sleepMillis(delayMilliSecs);
        } catch(InterruptedException ex ) {
            logger.info("Interrupted from waiting during a back-off delay. Ignoring this, but cutting our wait short.");
        }
    }
}