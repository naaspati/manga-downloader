package sam.manga.downloader.parts;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sam.manga.downloader.extra.Status;
import sam.manga.downloader.extra.StatusHelper;

public class Manga implements StatusHelper, Iterable<Chapter> {
    final String name;
    final int id;
    final String url;

    private Status status;
    private final Chapter[] chapters;

    public Manga(String name, int id, String url, Status status, List<Chapter> chapters) {
        this.name = name;
        this.id = id;
        this.url = url;
        this.status = status;
        
        if(chapters != null) {
            chapters.sort(Comparator.comparing(Chapter::getNumber));
            this.chapters = chapters.toArray(new Chapter[chapters.size()]);
        } else
            this.chapters = new Chapter[0];
    }
    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(Status status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public int chaptersCount() {
        return chapters.length;
    }

    @Override
    public Iterator<Chapter> iterator() {
        return new Iterator<Chapter>() {
            int n = 0;
            
            @Override
            public Chapter next() {
                return chapters[n++];
            }
            
            @Override
            public boolean hasNext() {
                return n < chapters.length;
            }
        };
    }

    public Stream<Chapter> stream(){
        return Arrays.stream(chapters);
    }
    
    /**
     * this check should be performed before add to selection listView so the name change can be reflected in List
     * @return 
     */
    public boolean hasDuplicateChapterNames() {

        //checking same file chapter_name with different chapter id (this happens when two different volumes have same chapter_name)
        //first check
        Map<String, List<Chapter>> map = Stream.of(chapters).collect(Collectors.groupingBy(Chapter::getChapterName));

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
            map = Arrays.stream(chapters).collect(Collectors.groupingBy(Chapter::getChapterName));

            String result = map.values().stream().anyMatch(l -> l.size() > 1) ? "FAILED": "SUCESS"; 
            b1.append("\r\n").append("Volume Patch: "+result);

            Logger.getLogger(getClass().getName()).warning(result);
            return true;
        }
        return false;
    }
}
