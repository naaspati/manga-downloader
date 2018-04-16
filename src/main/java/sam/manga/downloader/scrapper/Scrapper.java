package sam.manga.downloader.scrapper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import sam.manga.newsamrock.urls.MangaUrlsMeta;
import sam.manga.scrapper.scrappers.ScrapperBase;

public class Scrapper extends ScrapperBase {
    private static volatile Scrapper instance;
    public static final String URL_COLUMN = MangaUrlsMeta.MANGAFOX; 

    public static Scrapper getInstance() {
        if (instance == null) {
            synchronized (Scrapper.class) {
                if (instance == null)
                    instance = new Scrapper();
            }
        }
        return instance;
    }
    private Scrapper() {
        super(URL_COLUMN);
    }
    @Override
    protected Document getDocument(String url) throws MalformedURLException, IOException {
        return Jsoup.parse(new URL(url), 30000);
    }
    
    //TODO

}
