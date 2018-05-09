package sam.manga.downloader.scrapper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import sam.config.MyConfig;
import sam.internetutils.InternetUtils;
import sam.manga.downloader.chapter.Chapter;
import sam.manga.downloader.manga.Manga;
import sam.manga.downloader.page.Page;
import sam.manga.newsamrock.urls.MangaUrlsMeta;
import sam.manga.scrapper.scrappers.ScrapperBase;

public class Scrapper extends ScrapperBase<Manga, Chapter, Page> {
    private static volatile Scrapper instance;
    public static final String URL_COLUMN = MangaUrlsMeta.MANGAFOX;
    private final Path htmldir = Paths.get(MyConfig.COMMONS_DIR, "manga_dir", "html");
    private final InternetUtils internetUtils = new InternetUtils();

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
        super(ScrapperType.MANGAFOX);
    }
    @Override
    protected Document getDocument(String url) throws MalformedURLException, IOException {
        URL u = new URL(url);
        Path p = htmldir.resolve(Paths.get(u.getPath()).getFileName());

        if(Files.notExists(p))
            internetUtils.download(u, p);

        return Jsoup.parse(Files.newInputStream(p), "utf-8", url);
    }
}
