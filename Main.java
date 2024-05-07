import java.time.Instant;

public class Main {

    public static void main(String[] args) {

        EventProducer eventProducer = new EventProducer("Tests.StatusChangedEvent");

        IEvent event = new TestRunLifecycleStatusChangeEvent(Instant.now().toString(), "testing!!!");

        eventProducer.sendEvent(event);
        eventProducer.close();

        System.out.println("Finished.");
    }
    
}
