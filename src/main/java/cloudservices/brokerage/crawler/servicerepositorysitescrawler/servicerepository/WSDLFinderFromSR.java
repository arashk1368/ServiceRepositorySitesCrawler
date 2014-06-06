/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cloudservices.brokerage.crawler.servicerepositorysitescrawler.servicerepository;

import cloudservices.brokerage.crawler.crawlingcommons.model.DAO.DAOException;
import cloudservices.brokerage.crawler.crawlingcommons.model.DAO.WSDLDAO;
import cloudservices.brokerage.crawler.crawlingcommons.model.entities.WSDL;
import cloudservices.brokerage.crawler.servicerepositorysitescrawler.utils.DocumentLoader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Arash Khodadadi http://www.arashkhodadadi.com/
 */
public class WSDLFinderFromSR {

    private long politenessDelay;
    private long totalResultsNum;
    private long savedResultsNum;
    private WSDLDAO wsdlDAO;
    private String userAgent;
    private final static String CHARSET = "UTF-8";
    private final static String DOMAIN = "http://www.service-repository.com/";
    private final static Logger LOGGER = Logger.getLogger(WSDLFinderFromSR.class.getName());

    public WSDLFinderFromSR(long politenessDelay, String userAgent) {
        this.politenessDelay = politenessDelay;
        this.userAgent = userAgent;
        this.wsdlDAO = new WSDLDAO();
    }

    public void start(String query) throws IOException, DAOException {
        LOGGER.log(Level.INFO, "WSDL Finder from Service-Repository website started for query= {0}", query);

        StringBuilder sb = new StringBuilder();
        sb.append(DOMAIN);
        sb.append(query);

        LOGGER.log(Level.INFO, "Getting results in {0}", sb.toString());
        Document doc = DocumentLoader.getDocument(sb.toString(), this.userAgent);
        Elements results = doc.select("div.yui-b>table>tbody>tr");
        Elements mainResults = doc.select("div.yui-b>div>table>tbody>tr");
        results.addAll(mainResults);

        LOGGER.log(Level.INFO, "Found {0} results from parsing document", results.size());
        this.totalResultsNum = results.size();

        for (Element element : results) {
            if (element.childNodeSize() > 1) {
                Element nameCol = element.child(1);
                if (element.childNodeSize() > 0) {
                    Element a = nameCol.child(0);
                    String pageUrl = a.absUrl("href");
                    WSDL wsdl = getInfoPage(pageUrl);
                    if (wsdl != null) {
                        if (checkWSDL(wsdl)) {
                            wsdlDAO.addWSDL(wsdl);
                            LOGGER.log(Level.INFO, "WSDL with url= {0} added successfully with Id= {1}", new Object[]{wsdl.getUrl(), wsdl.getId()});
                            this.savedResultsNum++;

                        }
                    }
                    delay();
                }

            }
        }
    }

    private void delay() {
        try {
            Thread.sleep(this.politenessDelay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private WSDL getInfoPage(String pageUrl) throws IOException {
        LOGGER.log(Level.INFO, "Visiting {0} for getting info page", pageUrl);
        Document doc = DocumentLoader.getDocument(pageUrl, this.userAgent);
        String title = doc.select("div.service>h1>a").text();
        String url = "";
        String desc = "";
        Elements elems = doc.select("div.tabBody").get(0).children();
        for (Element elem : elems) {
            if (elem.text().compareTo("WSDL (original):") == 0) {
                url = elem.nextElementSibling().absUrl("href");
            } else if (elem.text().compareTo("Description:") == 0) {
                desc = elem.nextSibling().toString();
            }
        }
        if (url.isEmpty()) {
            LOGGER.log(Level.INFO, "Info page for page = {0} does not contain wsdl url", pageUrl);
            return null;
        }
        if (desc.isEmpty()) {
            LOGGER.log(Level.INFO, "Info page for page = {0} does not contain description", pageUrl);
        }
        return new WSDL(title, url, desc);
    }

    private boolean checkWSDL(WSDL result) throws DAOException {
        if (wsdlDAO.URLExists(result.getUrl())) {
            LOGGER.log(Level.INFO, "WSDL with url ={0} already exists", result.getUrl());
            return false;
        }
        //TODO: validate wsdl and other logic here
        return true;
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
}
