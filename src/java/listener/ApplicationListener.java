package listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import scheduler.SchedulerService;

@WebListener
public final class ApplicationListener implements ServletContextListener {
    private SchedulerService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        scheduler = new SchedulerService();
        scheduler.start();
        event.getServletContext().log("LastSignal scheduler initialized.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (scheduler != null) scheduler.stop();
        event.getServletContext().log("LastSignal scheduler stopped.");
    }
}
