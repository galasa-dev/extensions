/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.extensions.mocks;

import java.time.Instant;

import dev.galasa.framework.spi.utils.ITimeService;

public class MockTimeService implements ITimeService {

    private Instant currentTime;

    public MockTimeService(Instant currentTime) {
        this.currentTime = currentTime;
    }

    @Override
    public Instant now() {
        return currentTime;
    }

    public void setCurrentTime(Instant currentTime) {
        this.currentTime = currentTime;
    }
}
