/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.events.kafka;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import dev.galasa.events.kafka.internal.KafkaEventsService;
import dev.galasa.events.kafka.internal.KafkaEventsServiceRegistration;
import dev.galasa.extensions.mocks.MockFrameworkInitialisation;
import dev.galasa.framework.spi.IEventsService;

public class TestKafkaEventsServiceRegistration {

    @Test
    public void TestCanCreateARegistrationOK() {
        new KafkaEventsServiceRegistration();
    }

    @Test
    public void TestCanInitialiseARegistrationOK() throws Exception {
        // Given...
        KafkaEventsServiceRegistration registration = new KafkaEventsServiceRegistration();

        MockFrameworkInitialisation mockFrameworkInit = new MockFrameworkInitialisation();

        // When...
        registration.initialise(mockFrameworkInit);

        // Then...
        List<IEventsService> eventsServices = mockFrameworkInit.getRegisteredEventsServices();
        assertThat(eventsServices).isNotNull().hasSize(1);
        assertThat(eventsServices.get(0)).isInstanceOf(KafkaEventsService.class);
    }
    
}
