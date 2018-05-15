package sam.manga.downloader.scrapper;

import java.io.IOException;

import sam.manga.downloader.chapter.Chapter;
import sam.manga.downloader.manga.Manga;
import sam.manga.downloader.page.Page;
import sam.manga.scrapper.scrappers.impl.ScrapperCached;

public class Scrapper extends ScrapperCached<Manga, Chapter, Page> {
    private static volatile Scrapper instance;
    public static final String URL_COLUMN = "mangafox";

    public static Scrapper getInstance() {
        if (instance == null) {
            synchronized (Scrapper.class) {
                if (instance == null) {
                    try {
                        instance = new Scrapper();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return instance;
    }
    private Scrapper() throws IOException {
        super(ScrapperType.MANGAFOX, true, "http://fanfox.net");
    }
}
