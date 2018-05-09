package sam.manga.downloader.manga;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import sam.manga.downloader.chapter.Chapter;
import sam.manga.downloader.extra.Status;
import sam.manga.downloader.extra.StatusHelper;
import sam.manga.downloader.page.Page;
import sam.manga.scrapper.units.MangaBase;

public class Manga extends MangaBase<Chapter, Page> implements StatusHelper, Iterable<Chapter> {
    private Status status;

    public Manga(int id, String mangaName, String url) {
        super(id, mangaName, url);
    }

    public Manga(String name, int id, String url, Status status) {
        super(id, name, url);
        this.status = status;
    }
    @Override
    public Status getStatus() {
        return status;
    }
    @Override
    public void setStatus(Status status) {
        this.status = status;
    }

    public void addChapter(Chapter c) {
        if(chapters == null)
            chapters = new ArrayList<>();
        
        chapters.add(c);
    }
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Manga [");
        if (status != null) {
            builder.append("status=");
            builder.append(status);
            builder.append(", ");
        }
        builder.append("id=");
        builder.append(id);
        builder.append(", ");
        if (mangaName != null) {
            builder.append("mangaName=");
            builder.append(mangaName);
            builder.append(", ");
        }
        if (url != null) {
            builder.append("url=");
            builder.append(url);
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public Chapter newChapter(double number, String volume, String title, String url) {
        return new Chapter(id, volume, number, url, title);
    }
    /**
     * this check should be performed before add to selection listView so the name change can be reflected in List
     * @return 
     */
    public boolean hasDuplicateChapterNames() {

        //checking same file chapter_name with different chapter id (this happens when two different volumes have same chapter_name)
        //first check
        Map<String, List<Chapter>> map = stream().collect(Collectors.groupingBy(Chapter::getChapterName));

        if(map.values().stream().anyMatch(l -> l.size() > 1)){
            StringBuilder b1 = new StringBuilder("Double chapter name Error\r\n\r\n");

            map.forEach((s,t) -> {
                if(t.size() < 2)
                    return;

                b1.append("\t").append(s).append("\r\n");
                t.forEach(c -> b1.append("\t\t").append(c.toString()).append("\r\n"));
            });

            //volume patch up
            for (Chapter c : chapters) 
                c.applyVolumePatch();

            //second check
            map = stream().collect(Collectors.groupingBy(Chapter::getChapterName));

            String result = map.values().stream().anyMatch(l -> l.size() > 1) ? "FAILED": "SUCESS"; 
            b1.append("\r\n").append("Volume Patch: "+result);

            Logger.getLogger(getClass().getName()).warning(result);
            return true;
        }
        return false;
    }
    
}
