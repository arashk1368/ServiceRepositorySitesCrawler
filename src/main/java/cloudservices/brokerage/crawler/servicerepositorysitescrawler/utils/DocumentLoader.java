/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cloudservices.brokerage.crawler.servicerepositorysitescrawler.utils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 *
 * @author Arash Khodadadi http://www.arashkhodadadi.com/
 */
public class DocumentLoader {

    private final static Logger LOGGER = Logger.getLogger(DocumentLoader.class.getName());

    public static Document getDocument(String url, String userAgent) throws IOException {
        LOGGER.log(Level.INFO, "GET document with User Agent={0} from URL= {1}",
                new Object[]{userAgent, url});
        return Jsoup.connect(url).userAgent(userAgent).get();
    }
}
