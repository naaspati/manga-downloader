package sam.manga.downloader.chapter;

import static sam.manga.downloader.extra.Utils.DOWNLOAD_DIR;

import java.nio.file.Path;
import java.util.Objects;

import javafx.concurrent.Worker.State;
import sam.manga.downloader.data.DataManager;
import sam.manga.downloader.page.Page;
import sam.manga.scrapper.units.ChapterBase;
import sam.string.StringUtils;

public class Chapter extends ChapterBase<Page> {
    
    private volatile State state;
    private boolean modified;
    
    private Path savePath0;
    public final int chapterId;
    private String title2;  
    
    public Chapter(int mangaId, String volume, double number, String url, String title) {
        this(DataManager.newId(Chapter.class), mangaId, volume, number, url, title, null);
    }
    public Chapter(int chapterId, int mangaId, String volume, double number, String url, String title, State state) {
        super(mangaId, volume, number, title, url);
        
        this.title2 = title;
        this.chapterId = chapterId;
        this.state = state;
    }
    
    private String chapterName;
    public String getChapterName() {
        if(chapterName == null)
            chapterName = getChapterName0();
        
        return chapterName;
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
    private String getChapterName0()  {
        return StringUtils.doubleToString(number)+
                ((title2 == null || title2.trim().isEmpty() || title2.trim().equals("null"))? "": " "+title2);
    }
    boolean isModified() {
        return modified;
    }
    State getState() {
        return state;
    }
    void setState(State state) {
        this.state = state;
    }
    public Path getSavePath() {
        if(savePath0 != null)
            return savePath0;
        synchronized (this) {
            return savePath0 = generateChapterSavePath(mangaId, chapterId);
        }
    }
    int getChapterId() {
        return chapterId;
    }
    @Override
    public String getTitle() {
        return title2;
    }
    void setTitle(String title) {
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
        chapterName = getChapterName0();
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
    public Page newPage(int order, String pageUrl) {
        return new Page(chapterId, order, pageUrl, null);
    }
    boolean isCompleted() {
        return getState() == State.SUCCEEDED;
    }
    public static Path generateChapterSavePath(int manga_id, int chapter_id) {
        return DOWNLOAD_DIR.resolve(String.valueOf(manga_id)).resolve(String.valueOf(chapter_id));
    }
}
