package sam.manga.downloader.data;

import static sam.manga.newsamrock.mangas.MangasMeta.MANGA_ID;
import static sam.manga.newsamrock.mangas.MangasMeta.MANGA_NAME;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import sam.config.MyConfig;
import sam.manga.downloader.manga.Manga;
import sam.manga.downloader.scrapper.Scrapper;
import sam.tsv.Row;
import sam.tsv.Tsv;

class TsvMangaLoader {
    public static List<Manga> load() throws IOException {
        Path p1 = Paths.get(MyConfig.NEW_MANGAS_TSV_FILE);
        Path p2 = Paths.get(MyConfig.UPDATED_MANGAS_TSV_FILE);

        if(Files.exists(p1) && Files.exists(p2))
            return new TsvMangaLoader(p1, p2)._load();
        if(Files.exists(p1))
            return new TsvMangaLoader(p1)._load();
        if(Files.exists(p2))
            return new TsvMangaLoader(p2)._load();

        return null;
    }

    private final Path[] paths;

    private TsvMangaLoader(Path...ps) {
        this.paths = ps;
    }
    private List<Manga> _load() throws IOException {
        List<Manga> mangas = new ArrayList<>();
        
        for (Path path : paths) 
            load(path, mangas);
        
        return mangas;
    }
    private void load(Path path, List<Manga> sink) throws IOException {
        for (Row row : Tsv.parse(path))
            sink.add(new Manga(row.getInt(MANGA_ID), row.get(MANGA_NAME), row.get(Scrapper.URL_COLUMN)));
    }
}
