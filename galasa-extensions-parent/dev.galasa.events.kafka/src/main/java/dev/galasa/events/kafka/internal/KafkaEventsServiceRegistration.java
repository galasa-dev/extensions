package dev.galasa.events.kafka.internal;

import javax.validation.constraints.NotNull;

import org.osgi.service.component.annotations.Component;

import dev.galasa.framework.spi.IEventsServiceRegistration;
import dev.galasa.framework.spi.IFrameworkInitialisation;

@Component(service = { IEventsServiceRegistration.class })
public class KafkaEventsServiceRegistration implements IEventsServiceRegistration {
    
    @Override
    public void initialise(@NotNull IFrameworkInitialisation frameworkInitialisation)
        throws KafkaException {

    // Initialise in here
    
    }

}
