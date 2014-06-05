package cloudservices.brokerage.crawler.servicerepositorysitescrawler;

import cloudservices.brokerage.commons.utils.logging.LoggerSetup;
import cloudservices.brokerage.crawler.crawlingcommons.model.DAO.BaseDAO;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.cfg.Configuration;

/**
 * Hello world!
 *
 */
public class App {

    private final static Logger LOGGER = Logger.getLogger(App.class.getName());

    public static void main(String[] args) {
        try {
            LoggerSetup.setup("log.txt", "log.html", Level.INFO);
        } catch (IOException e) {
            throw new RuntimeException("Problems with creating the log files");
        }

        long startTime = System.currentTimeMillis();
        LOGGER.log(Level.SEVERE, "Searching Start");
        try {
            Configuration configuration = new Configuration();
            configuration.configure("hibernate.cfg.xml");
            BaseDAO.openSession(configuration);

//        } catch (DAOException ex) {
//            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            BaseDAO.closeSession();
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            LOGGER.log(Level.SEVERE, "Searching End in {0}ms", totalTime);
        }
    }
}
