package sam.manga.downloader.chapter;

import static sam.manga.downloader.extra.Utils.DOWNLOAD_DIR;

import java.nio.file.Path;
import java.util.Objects;

import sam.manga.downloader.data.DataManager;
import sam.manga.downloader.extra.Status;
import sam.manga.downloader.extra.StatusHelper;
import sam.manga.downloader.page.Page;
import sam.manga.scrapper.units.ChapterBase;
import sam.string.StringUtils;

public class Chapter extends ChapterBase<Page> implements StatusHelper {
    
    private Status status;
    private boolean modified;
    
    private final Path savePath;
    public final int chapterId;
    private String title2;  
    
    public Chapter(int mangaId, String volume, double number, String url, String title) {
        this(DataManager.newId(Chapter.class), mangaId, volume, number, url, title, Status.UNTOUCHED);
    }
    public Chapter(int chapterId, int mangaId, String volume, double number, String url, String title, Status status) {
        super(mangaId, volume, number, title, url);
        
        this.title2 = title;
        this.chapterId = chapterId;
        this.status = status;
 
        savePath = generateChapterSavePath();
    }
    /**
     * will create chapter name as MangaRock Converter will do for a chapter 
     * <br>
     * purpose of this is to check, if two or more chapter does not have same name after create Names 
     * 
     * @param number
     * @param title2
     * @return
     */
    public String getChapterName()  {
        return StringUtils.doubleToString(number)+
                ((title2 == null || title2.trim().isEmpty() || title2.trim().equals("null"))? "": " "+title2);
    }
    
    public Path generateChapterSavePath(){
        return DOWNLOAD_DIR.resolve(String.valueOf(mangaId)).resolve(String.valueOf(chapterId));
    }
    public static Path generateChapterSavePath(int manga_id, int chapter_id){
        return DOWNLOAD_DIR.resolve(String.valueOf(manga_id)).resolve(String.valueOf(chapter_id));
    }
    public boolean isModified() {
        return modified;
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
    public Path getSavePath() {
        return savePath;
    }
    public int getChapterId() {
        return chapterId;
    }
    @Override
    public String getTitle() {
        return title2;
    }
    public void setTitle(String title) {
        Objects.requireNonNull(title, "title2 cannot be null");
        if(title.trim().isEmpty())
           throw new IllegalArgumentException("title2 cannot be empty string");
        if(!Objects.equals(this.title2, title))
            modified = true;
        
        this.title2 = title;
    }

    /**
     * this will add {@link #volume} at the end of chapter {@link #title2}
     */
    public void applyVolumePatch() {
        title2 = title2 == null  || title2.trim().isEmpty() ? volume : volume + " - "+ title2 ;
    }
    public static String generateChapterName(double number, String title2) {
        return StringUtils.doubleToString(number)+
                ((title2 == null || title2.trim().isEmpty() || title2.trim().equals("null"))? "": " "+title2);
    }
    @Override
    protected Class<Page> pageClass() {
        return Page.class;
    }
    @Override
    protected Page newPage(int order, String pageUrl) {
        return new Page(chapterId, order, pageUrl, null);
    }
}
