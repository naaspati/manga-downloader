package sam.manga.downloader.data;

import static sam.manga.newsamrock.mangas.MangasMeta.MANGA_ID;
import static sam.manga.newsamrock.mangas.MangasMeta.MANGA_NAME;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import sam.manga.downloader.manga.Manga;
import sam.manga.downloader.scrapper.Scrapper;
import sam.tsv.Row;
import sam.tsv.Tsv;

public class TsvMangaLoader {
    private final Path[] paths;

    public TsvMangaLoader(Path...ps) {
        this.paths = ps;
    }
    public List<Manga> load() throws IOException {
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
