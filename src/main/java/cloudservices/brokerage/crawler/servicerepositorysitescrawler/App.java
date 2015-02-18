package cloudservices.brokerage.crawler.servicerepositorysitescrawler;

import cloudservices.brokerage.commons.utils.file_utils.DirectoryUtil;
import cloudservices.brokerage.commons.utils.logging.LoggerSetup;
import cloudservices.brokerage.crawler.crawlingcommons.model.DAO.BaseDAO;
import cloudservices.brokerage.crawler.crawlingcommons.model.DAO.DAOException;
import cloudservices.brokerage.crawler.crawlingcommons.model.enums.v3.ServiceDescriptionType;
import cloudservices.brokerage.crawler.servicerepositorysitescrawler.programmableweb.ServiceFinderFromPW;
import cloudservices.brokerage.crawler.servicerepositorysitescrawler.servicerepository.WSDLFinderFromSR;
import cloudservices.brokerage.crawler.servicerepositorysitescrawler.xmethods.WSDLFinderFromXM;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.cfg.Configuration;

/**
 * Hello world!
 *
 */
public class App {

    private final static Logger LOGGER = Logger.getLogger(App.class.getName());
    private final static String USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; rv:36.0)"
            + " Gecko/20100101 Firefox/36.0";
    private final static long POLITENESS_DELAY = 1000; //ms
    private static WSDLFinderFromXM finderXM;
    private static WSDLFinderFromSR finderSR;
    private static ServiceFinderFromPW finderPW;

    public static void main(String[] args) {
        createLogFile();
//        createNewDB();

        long startTime = System.currentTimeMillis();
        LOGGER.log(Level.SEVERE, "Searching Start");
        finderSR = new WSDLFinderFromSR(POLITENESS_DELAY, USER_AGENT);
        finderXM = new WSDLFinderFromXM(POLITENESS_DELAY, USER_AGENT);
        finderPW = new ServiceFinderFromPW(POLITENESS_DELAY, USER_AGENT);

        try {
//            SearchFromSRXM();
            SearchFromPW();
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
            LOGGER.log(Level.SEVERE, "Total Results Found from ProgrammableWeb Site: {0}", finderPW.getTotalResultsNum());
            LOGGER.log(Level.SEVERE, "Total Services Saved from ProgrammableWeb Site: {0}", finderPW.getSavedResultsNum());
            LOGGER.log(Level.SEVERE, "Total Services Modified from ProgrammableWeb Site: {0}", finderPW.getModifiedResultsNum());
            LOGGER.log(Level.SEVERE, "Total Providers Found from ProgrammableWeb Site: {0}", finderPW.getTotalProvidersNum());
            LOGGER.log(Level.SEVERE, "Total Providers Saved from ProgrammableWeb Site: {0}", finderPW.getSavedProvidersNum());
            LOGGER.log(Level.SEVERE, "Total Providers Modified from ProgrammableWeb Site: {0}", finderPW.getModifiedProvidersNum());
            LOGGER.log(Level.SEVERE, "Total Results Found from ProgrammableWeb Site without URL: {0}", finderPW.getWithoutDescriptionUrlNum());
            LOGGER.log(Level.SEVERE, "Total Results Found from ProgrammableWeb Site without Provider: {0}", finderPW.getWithoutProvidersNum());
        }
    }

    private static void createNewDB() {
        try {
            Configuration configuration = new Configuration();
            configuration.configure("v3hibernate.cfg.xml");
            BaseDAO.openSession(configuration);
            LOGGER.log(Level.INFO, "Database Creation Successful");
        } finally {
            BaseDAO.closeSession();
        }
    }

    private static boolean createLogFile() {
        try {
            StringBuilder sb = new StringBuilder();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm");
            Calendar cal = Calendar.getInstance();
            sb.append(dateFormat.format(cal.getTime()));
            String filename = sb.toString();
            DirectoryUtil.createDir("logs");
            LoggerSetup.setup("logs/" + filename + ".txt", "logs/" + filename + ".html", Level.FINER);
            return true;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            return false;
        }
    }

    private static void SearchFromSRXM() throws IOException, DAOException {
        Configuration configuration = new Configuration();
        configuration.configure("hibernate.cfg.xml");
        BaseDAO.openSession(configuration);

        finderSR.start("?offset=0&max=10000");
        finderXM.start("");
    }

    private static void SearchFromPW() throws IOException, DAOException {
        Configuration configuration = new Configuration();
        configuration.configure("v3hibernate.cfg.xml");
        BaseDAO.openSession(configuration);

        String query;
        
        // Rest
//        query = "category/all/apis?data_format=21190&order=created&sort=desc";
//        finderPW.start(query, ServiceDescriptionType.REST, 0, 1);

        // WSDL
        // YOU SHOULD NOT USE THIS BECAUSE ENDPOINT IS THE URL!
        query = "category/all/apis?data_format=21183&order=created&sort=desc";
        finderPW.start(query, ServiceDescriptionType.WSDL, 0, 1);
    }
}
