/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cloudservices.brokerage.crawler.servicerepositorysitescrawler.programmableweb;

import cloudservices.brokerage.commons.utils.url_utils.URLExtracter;
import cloudservices.brokerage.crawler.crawlingcommons.model.DAO.DAOException;
import cloudservices.brokerage.crawler.crawlingcommons.model.DAO.v3.ServiceDescriptionDAO;
import cloudservices.brokerage.crawler.crawlingcommons.model.DAO.v3.ServiceProviderDAO;
import cloudservices.brokerage.crawler.crawlingcommons.model.entities.v3.ServiceDescription;
import cloudservices.brokerage.crawler.crawlingcommons.model.entities.v3.ServiceProvider;
import cloudservices.brokerage.crawler.crawlingcommons.model.enums.v3.ServiceDescriptionColType;
import cloudservices.brokerage.crawler.crawlingcommons.model.enums.v3.ServiceDescriptionType;
import cloudservices.brokerage.crawler.servicerepositorysitescrawler.utils.DocumentLoader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Arash Khodadadi http://www.arashkhodadadi.com/
 */
public class ServiceFinderFromPW {

    private long politenessDelay;
    private long totalResultsNum;
    private long savedResultsNum;
    private long modifiedResultsNum;
    private long withoutDescriptionUrlNum;
    private long totalProvidersNum;
    private long savedProvidersNum;
    private long modifiedProvidersNum;
    private long withoutProvidersNum;
    private final ServiceDescriptionDAO serviceDescDAO;
    private ServiceProviderDAO serviceProviderDAO;
    private String userAgent;
    private final static String TOKEN = ";;;";
    private final static String DOMAIN = "http://www.programmableweb.com/";
    private final static Logger LOGGER = Logger.getLogger(ServiceFinderFromPW.class.getName());
    private boolean providerAdded;

    public ServiceFinderFromPW(long politenessDelay, String userAgent) {
        this.politenessDelay = politenessDelay;
        this.userAgent = userAgent;
        this.serviceDescDAO = new ServiceDescriptionDAO();
        this.serviceProviderDAO = new ServiceProviderDAO();
    }

    public void start(String query, ServiceDescriptionType type, int startingPage, int numberOfPages) throws IOException, DAOException {
        LOGGER.log(Level.INFO, "Service Finder from Programmable Web website started for query= {0}", query);

        for (int page = startingPage; page <= numberOfPages; page++) {
            StringBuilder sb = new StringBuilder();
            sb.append(DOMAIN);
            sb.append(query);
            sb.append("&page=");
            sb.append(page);
            LOGGER.log(Level.INFO, "Getting results in page : {0} with url : {1}", new Object[]{page, sb.toString()});
            Document doc = DocumentLoader.getDocument(sb.toString(), this.userAgent);
            Elements results = doc.select("div.view-content>table>tbody>tr");

            LOGGER.log(Level.INFO, "Found {0} results from parsing document", results.size());
            this.totalResultsNum += results.size();

            for (Element element : results) {
                if (!element.children().isEmpty()) {
                    // first td
                    Element nameCol = element.child(0);
                    if (!nameCol.children().isEmpty()) {
                        // a href
                        Element a = nameCol.child(0);
                        String pageUrl = a.absUrl("href");
                        ServiceDescription serviceDesc = getInfoPage(pageUrl);
                        if (serviceDesc != null) {
                            providerAdded = false;
                            serviceDesc.setSource("ProgrammableWeb");
                            serviceDesc.setType(type);
                            if (serviceDesc.getServiceProvider() != null) {
                                updateProvider(serviceDesc, serviceDescDAO, serviceProviderDAO);
                            } else {
                                this.withoutProvidersNum++;
                            }
                            addOrUpdateService(serviceDesc, serviceDescDAO);
                        } else {
                            this.withoutDescriptionUrlNum++;
                        }
                        delay();
                    }

                }
            }
        }
    }

    private void delay() {
        try {
            long delay = 0;
            while (delay < 1000) {
                long rand = Math.round(Math.random() * 10);
                delay = rand * this.politenessDelay;
            }
            LOGGER.log(Level.SEVERE, "Waiting for {0}", delay);
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private ServiceDescription getInfoPage(String pageUrl) throws IOException {
        LOGGER.log(Level.INFO, "Visiting {0} for getting info page", pageUrl);
        try {
            Document doc = DocumentLoader.getDocument(pageUrl, this.userAgent);

            ServiceDescription serviceDescription = new ServiceDescription();
            serviceDescription.setTitle(doc.select("header>div.node-header>h1").text());
            serviceDescription.setDescription(doc.select("div.api_description").text());
            String providerUrl = "";
            String endpoint = "";
            String descUrl = "";
            String tags = "";
            Elements elems = doc.select("div#tabs-content>div.section.specs>div.field");
            for (Element elem : elems) {
                String label = elem.child(0).text();
                Element value = elem.child(1);
                if (label.compareTo("API Provider") == 0) {
                    // a href
                    if (!value.children().isEmpty()) {
                        Element a = value.child(0);
                        providerUrl = a.absUrl("href");
                    }
                } else if (label.compareTo("API Endpoint") == 0) {
                    // a href
                    if (!value.children().isEmpty()) {
                        Element a = value.child(0);
                        endpoint = a.absUrl("href");
                    }
                } else if (label.compareTo("API Homepage") == 0) {
                    // a href
                    if (!value.children().isEmpty()) {
                        Element a = value.child(0);
                        descUrl = a.absUrl("href");
                    }
                } else if (label.compareTo("Primary Category") == 0) {
                    // a text
                    if (!value.children().isEmpty()) {
                        Element a = value.child(0);
                        tags = tags.concat(a.text()).concat(TOKEN);
                    }
                } else if (label.compareTo("Secondary Categories") == 0) {
                    if (!value.children().isEmpty()) {
                        for (Element a : value.select("a")) {
                            tags = tags.concat(a.text()).concat(TOKEN);
                        }
                    }
                }
            }
            if (!tags.isEmpty()) {
                tags = tags.substring(0, tags.length() - 3);
            }
            if (descUrl.isEmpty()) {
                LOGGER.log(Level.INFO, "Info page for page = {0} does not contain service description url", pageUrl);
                return null;
            }
            if (!providerUrl.isEmpty()) {
                try {
                    ServiceProvider provider = new ServiceProvider(providerUrl);
                    provider.setNumberOfServices(1);
                    provider.setName(URLExtracter.getDomainName(providerUrl));
                    serviceDescription.setServiceProvider(provider);
                    this.totalProvidersNum++;
                } catch (URISyntaxException ex) {
                    LOGGER.log(Level.INFO, "Info page for page = {0} contains a wrong provider url", pageUrl);
                }

            } else {
                LOGGER.log(Level.INFO, "Info page for page = {0} does not contain provider url", pageUrl);
            }

            serviceDescription.setUrl(descUrl);
            serviceDescription.setExtraInfo(endpoint);
            serviceDescription.setTags(tags);

            return serviceDescription;
        } catch (Exception ex) {
            LOGGER.log(Level.INFO, "Exception in Getting Info page for page = {0}", pageUrl);
            return null;
        }
    }

    private void addOrUpdateService(ServiceDescription sd, ServiceDescriptionDAO sdDAO) throws DAOException {
        ServiceDescription inDB = sdDAO.findByUrl(sd.getUrl());

        if (inDB == null) {
            LOGGER.log(Level.FINE, "There is no service in DB, Saving a new one");
            sd.setUpdated(true);
            sdDAO.addServiceDescription(sd);
            this.savedResultsNum++;
        } else {
            LOGGER.log(Level.FINE, "Found the same url with ID = {0} in DB, Trying to update", inDB.getId());

            if (inDB.getTitle().compareTo(sd.getTitle()) != 0) {
                LOGGER.log(Level.FINER, "Titles are different;new one: {0} , indb: {1}", new Object[]{sd.getTitle(), inDB.getTitle()});
                String[] titles = sd.getTitle().split(TOKEN);
                for (String title : titles) {
                    if (!inDB.getTitle().contains(title)) {
                        String newString = inDB.getTitle().concat(TOKEN).concat(title);
                        LOGGER.log(Level.FINER, "Adding Title: {0}", title);
                        if (ServiceDescription.checkLength(newString.length(), ServiceDescriptionColType.TITLE)) {
                            inDB.setTitle(newString);
                        } else {
                            LOGGER.log(Level.WARNING, "Title can not be updated because it is too large!");
                        }
                    }
                }
                inDB.setUpdated(true);
            }

            if (inDB.getDescription().compareTo(sd.getDescription()) != 0) {
                LOGGER.log(Level.FINER, "Descriptions are different;new one: {0} , indb: {1}", new Object[]{sd.getDescription(), inDB.getDescription()});
                String[] descriptions = sd.getDescription().split(TOKEN);
                for (String str : descriptions) {
                    if (!inDB.getDescription().contains(str)) {
                        String newString = inDB.getDescription().concat(TOKEN).concat(str);
                        LOGGER.log(Level.FINER, "Adding Description: {0}", str);
                        if (ServiceDescription.checkLength(newString.length(), ServiceDescriptionColType.DESCRIPTION)) {
                            inDB.setDescription(newString);
                        } else {
                            LOGGER.log(Level.WARNING, "Description can not be updated because it is too large!");
                        }
                    }
                }
                inDB.setUpdated(true);
            }

            if (inDB.getSource().compareTo(sd.getSource()) != 0) {
                LOGGER.log(Level.FINER, "Sources are different;new one: {0} , indb: {1}", new Object[]{sd.getSource(), inDB.getSource()});
                String[] newOnes = sd.getSource().split(TOKEN);
                for (String str : newOnes) {
                    if (!inDB.getSource().contains(str)) {
                        String newString = inDB.getSource().concat(TOKEN).concat(str);
                        LOGGER.log(Level.FINER, "Adding Source: {0}", str);
                        if (ServiceDescription.checkLength(newString.length(), ServiceDescriptionColType.SOURCE)) {
                            inDB.setSource(newString);
                        } else {
                            LOGGER.log(Level.WARNING, "Source can not be updated because it is too large!");
                        }
                    }
                }
                inDB.setUpdated(true);
            }

            if (inDB.getTags().compareTo(sd.getTags()) != 0) {
                LOGGER.log(Level.FINER, "Tags are different;new one: {0} , indb: {1}", new Object[]{sd.getTags(), inDB.getTags()});
                String[] newOnes = sd.getTags().split(TOKEN);
                for (String str : newOnes) {
                    if (!inDB.getTags().contains(str)) {
                        String newString = inDB.getTags().concat(TOKEN).concat(str);
                        LOGGER.log(Level.FINER, "Adding Tag: {0}", str);
                        if (ServiceDescription.checkLength(newString.length(), ServiceDescriptionColType.TAGS)) {
                            inDB.setTags(newString);
                        } else {
                            LOGGER.log(Level.WARNING, "Tags can not be updated because it is too large!");
                        }
                    }
                }
                inDB.setUpdated(true);
            }

            if (inDB.getExtraInfo().compareTo(sd.getExtraInfo()) != 0) {
                LOGGER.log(Level.FINER, "Extra info are different;new one: {0} , indb: {1}", new Object[]{sd.getExtraInfo(), inDB.getExtraInfo()});
                String[] newOnes = sd.getExtraInfo().split(TOKEN);
                for (String str : newOnes) {
                    if (!inDB.getExtraInfo().contains(str)) {
                        String newString = inDB.getExtraInfo().concat(TOKEN).concat(str);
                        LOGGER.log(Level.FINER, "Adding Extra Info: {0}", str);
                        if (ServiceDescription.checkLength(newString.length(), ServiceDescriptionColType.EXTRA_INFO)) {
                            inDB.setExtraInfo(newString);
                        } else {
                            LOGGER.log(Level.WARNING, "Extra info can not be updated because it is too large!");
                        }
                    }
                }
                inDB.setUpdated(true);
            }

            // service exists but without provider
            if (inDB.getServiceProvider() == null && sd.getServiceProvider() != null) {
                ServiceProvider sp = sd.getServiceProvider();
                LOGGER.log(Level.FINER, "Service Provider Updated to ", sp.getName());
                // service exists, provider exists
                if (!providerAdded) {
                    sp.setNumberOfServices(sp.getNumberOfServices() + 1);
                    serviceProviderDAO.saveOrUpdate(sp);
                }
                inDB.setServiceProvider(sp);
                inDB.setUpdated(true);
            }

            if (inDB.isUpdated()) {
                sdDAO.saveOrUpdate(inDB);
                this.modifiedResultsNum++;
            }
        }
    }

    private void updateProvider(ServiceDescription serviceDesc, ServiceDescriptionDAO serviceDescDAO, ServiceProviderDAO serviceProviderDAO) throws DAOException {
        ServiceProvider serviceProvider = serviceDesc.getServiceProvider();

        ServiceProvider inDB = serviceProviderDAO.findByName(serviceProvider.getName());
        if (inDB == null) {
            LOGGER.log(Level.FINE, "There is no service provider in DB with Name = {0}, Saving a new one", serviceProvider.getName());
            serviceProviderDAO.addServiceProvider(serviceProvider);
            serviceDesc.setServiceProvider(serviceProvider);
            providerAdded = true;
            this.savedProvidersNum++;
        } else {
            LOGGER.log(Level.FINE, "Found the same provider name with ID = {0} in DB, Updating", inDB.getId());
            if (!serviceProvider.getCountry().isEmpty()) {
                inDB.setCountry(inDB.getCountry().concat(TOKEN).concat(serviceProvider.getCountry()));
            }
            if (!serviceProvider.getDescription().isEmpty()) {
                inDB.setDescription(inDB.getDescription().concat(TOKEN).concat(serviceProvider.getDescription()));

            }
            if (!serviceProvider.getExtraInfo().isEmpty()) {
                inDB.setExtraInfo(inDB.getExtraInfo().concat(TOKEN).concat(serviceProvider.getExtraInfo()));

            }
            if (!serviceProvider.getTags().isEmpty()) {
                inDB.setTags(inDB.getTags().concat(TOKEN).concat(serviceProvider.getTags()));

            }

            if (serviceDescDAO.findByUrl(serviceDesc.getUrl()) == null) {
                inDB.setNumberOfServices(inDB.getNumberOfServices() + serviceProvider.getNumberOfServices());
            }

            inDB.setUrl(serviceProvider.getUrl());

            serviceProviderDAO.saveOrUpdate(inDB);
            this.modifiedProvidersNum++;
            serviceDesc.setServiceProvider(inDB);
        }
    }

    public long getPolitenessDelay() {
        return politenessDelay;
    }

    public void setPolitenessDelay(long politenessDelay) {
        this.politenessDelay = politenessDelay;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public long getTotalResultsNum() {
        return totalResultsNum;
    }

    public long getSavedResultsNum() {
        return savedResultsNum;
    }

    public long getModifiedResultsNum() {
        return modifiedResultsNum;
    }

    public long getWithoutDescriptionUrlNum() {
        return withoutDescriptionUrlNum;
    }

    public void setWithoutDescriptionUrlNum(long withoutDescriptionUrlNum) {
        this.withoutDescriptionUrlNum = withoutDescriptionUrlNum;
    }

    public long getTotalProvidersNum() {
        return totalProvidersNum;
    }

    public void setTotalProvidersNum(long totalProvidersNum) {
        this.totalProvidersNum = totalProvidersNum;
    }

    public long getSavedProvidersNum() {
        return savedProvidersNum;
    }

    public void setSavedProvidersNum(long savedProvidersNum) {
        this.savedProvidersNum = savedProvidersNum;
    }

    public long getModifiedProvidersNum() {
        return modifiedProvidersNum;
    }

    public void setModifiedProvidersNum(long modifiedProvidersNum) {
        this.modifiedProvidersNum = modifiedProvidersNum;
    }

    public long getWithoutProvidersNum() {
        return withoutProvidersNum;
    }

    public void setWithoutProvidersNum(long withoutProvidersNum) {
        this.withoutProvidersNum = withoutProvidersNum;
    }

    public ServiceProviderDAO getServiceProviderDAO() {
        return serviceProviderDAO;
    }

    public void setServiceProviderDAO(ServiceProviderDAO serviceProviderDAO) {
        this.serviceProviderDAO = serviceProviderDAO;
    }

}
