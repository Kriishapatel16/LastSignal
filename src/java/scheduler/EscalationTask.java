package scheduler;

import java.util.logging.Level;
import java.util.logging.Logger;
import service.EscalationService;

public final class EscalationTask implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(EscalationTask.class.getName());
    private final EscalationService service = new EscalationService();

    @Override
    public void run() {
        try {
            service.processDueEscalations();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Escalation scan failed.", ex);
        }
    }
}
