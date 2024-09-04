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

/**
 * Allows a lambda function to be used, and that function will be retried a number of times before giving up.
 */
public class RetryableCouchdbUpdateOperationProcessor {

    public int MAX_ATTEMPTS_TO_GO_BEFORE_GIVE_UP = 10;
    private ITimeService timeService ;

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

                waitForBackoffDelay();

                // TODO: Log it
                attemptsToGoBeforeGiveUp -= 1;
                if (attemptsToGoBeforeGiveUp == 0) {
                    throw new CouchdbException("tried x times and could not update doc...", updateClashedEx);
                }
            }
        }
    }

    private void waitForBackoffDelay() {
        Long delayMilliSecs = 1000L + new Random().nextInt(3000);

        try {
            timeService.wait(delayMilliSecs);
        } catch(InterruptedException ex ) {
            // TODO: Log it and continue. 
        }
        
    }
}