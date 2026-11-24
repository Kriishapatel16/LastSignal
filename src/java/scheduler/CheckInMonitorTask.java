package scheduler;

import java.util.logging.Level;
import java.util.logging.Logger;
import service.NotificationService;

public final class CheckInMonitorTask implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(CheckInMonitorTask.class.getName());
    private final NotificationService service = new NotificationService();

    @Override
    public void run() {
        try {
            service.processDueReminders();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Check-in reminder scan failed.", ex);
        }
    }
}
