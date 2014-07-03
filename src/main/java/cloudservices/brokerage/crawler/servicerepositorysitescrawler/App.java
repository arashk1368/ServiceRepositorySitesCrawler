package cloudservices.brokerage.crawler.servicerepositorysitescrawler;

import cloudservices.brokerage.commons.utils.logging.LoggerSetup;
import cloudservices.brokerage.crawler.crawlingcommons.model.DAO.BaseDAO;
import cloudservices.brokerage.crawler.crawlingcommons.model.DAO.DAOException;
import cloudservices.brokerage.crawler.servicerepositorysitescrawler.servicerepository.WSDLFinderFromSR;
import cloudservices.brokerage.crawler.servicerepositorysitescrawler.xmethods.WSDLFinderFromXM;
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
    private final static String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.114 Safari/537.36";
    private final static long POLITENESS_DELAY = 500; //ms

    public static void main(String[] args) {
        try {
            LoggerSetup.setup("log.txt", "log.html", Level.INFO);
        } catch (IOException e) {
            throw new RuntimeException("Problems with creating the log files");
        }

        long startTime = System.currentTimeMillis();
        LOGGER.log(Level.SEVERE, "Searching Start");
        WSDLFinderFromSR finderSR = new WSDLFinderFromSR(POLITENESS_DELAY, USER_AGENT);
        WSDLFinderFromXM finderXM = new WSDLFinderFromXM(POLITENESS_DELAY, USER_AGENT);

        try {
            Configuration configuration = new Configuration();
            configuration.configure("hibernate.cfg.xml");
            BaseDAO.openSession(configuration);

            finderSR.start("?offset=0&max=10000");
            finderXM.start("");
        } catch (DAOException | IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            BaseDAO.closeSession();
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            LOGGER.log(Level.SEVERE, "Searching End in {0}ms", totalTime);
            LOGGER.log(Level.SEVERE, "Total Results Found from Service Repository Site: {0}", finderSR.getTotalResultsNum());
            LOGGER.log(Level.SEVERE, "Total WSDL Saved from Service Repository Site: {0}", finderSR.getSavedResultsNum());
            LOGGER.log(Level.SEVERE, "Total WSDL Modified from Service Repository Site: {0}", finderSR.getModifiedResultsNum());
            LOGGER.log(Level.SEVERE, "Total Results Found from XMethods Site: {0}", finderXM.getTotalResultsNum());
            LOGGER.log(Level.SEVERE, "Total WSDL Saved from XMethods Site: {0}", finderXM.getSavedResultsNum());
            LOGGER.log(Level.SEVERE, "Total WSDL Modified from XMethods Site: {0}", finderXM.getModifiedResultsNum());
        }
    }
}
