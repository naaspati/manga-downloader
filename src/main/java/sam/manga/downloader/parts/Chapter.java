package sam.manga.downloader.parts;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import sam.manga.downloader.DownloaderApp;
import sam.manga.downloader.extra.Status;
import sam.manga.downloader.extra.StatusHelper;
import sam.string.stringutils.StringUtils;

public class Chapter implements Iterable<Page>, StatusHelper {
    final int id;
    //final int MANGA_ID;
    final String volume;
    final double number;
    final String url;
    final int manga_id;

    private String title;
    
    private volatile Status status;
    private volatile boolean modified;
    
    private final Page[] pages;
    private final Path savePath;
    
    public Chapter(int pageId, int mangaId, String volume, double number, String url, String title, Status status, List<Page> pageList) {
        Objects.requireNonNull(pageList);
        
        this.id = pageId;
        this.manga_id = mangaId;
        this.volume = volume;
        this.number = number;
        this.url = url;
        this.title = title;
        this.status = status;
 
        savePath = generateChapterSavePath();
        pages = pageList.stream().sorted(Comparator.comparing(p -> p.getOrder())).toArray(Page[]::new);
    }
    
    /**
     * will create chapter name as MangaRock Converter will do for a chapter 
     * <br>
     * purpose of this is to check, if two or more chapter does not have same name after create Names 
     * 
     * @param number
     * @param title
     * @return
     */
    public String getChapterName()  {
        return StringUtils.doubleToString(number)+
                ((title == null || title.trim().isEmpty() || title.trim().equals("null"))? "": " "+title);
    }
    
    public Path generateChapterSavePath(){
        return DownloaderApp.DOWNLOAD_DIR.resolve(String.valueOf(manga_id)).resolve(String.valueOf(id));
    }
    public static Path generateChapterSavePath(int manga_id, int chapter_id){
        return DownloaderApp.DOWNLOAD_DIR.resolve(String.valueOf(manga_id)).resolve(String.valueOf(chapter_id));
    }
    public boolean isModified() {
        return modified;
    }
    public double getNumber() {
        return number;
    }
    @Override
    public Status getStatus() {
        return status;
    }
    @Override
    public void setStatus(Status status) {
        if(this.status != status)
            modified = true;
        this.status = status;
    }

    public int getPageCount() {
        return pages.length;
    }
    public Path getSavePath() {
        return savePath;
    }
    public int getId() {
        return id;
    }
    public String getVolume() {
        return volume;
    }
    public String getUrl() {
        return url;
    }

    @Override
    public Iterator<Page> iterator() {
        return new Iterator<Page>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                return index < pages.length;
            }

            @Override
            public Page next() {
                return pages[index++];
            }
            
        };
    }

    public String getTitle() {
        return title;
    }
    public Stream<Page> stream(){
        return Arrays.stream(pages);
    }

    public void setTitle(String title) {
        Objects.requireNonNull(title, "title cannot be null");
        if(title.trim().isEmpty())
           throw new IllegalArgumentException("title cannot be empty string");
        if(!Objects.equals(this.title, title))
            modified = true;
        
        this.title = title;
    }

    /**
     * this will add {@link #volume} at the end of chapter {@link #title}
     */
    public void applyVolumePatch() {
        title = title == null  || title.trim().isEmpty() ? volume : volume + " - "+ title ;
    }

    public static String generateChapterName(double number, String title) {
        return StringUtils.doubleToString(number)+
                ((title == null || title.trim().isEmpty() || title.trim().equals("null"))? "": " "+title);
    }
}
