package sam.manga.downloader.manga;

import javafx.concurrent.Worker.State;
import sam.manga.downloader.chapter.ChapterPresenter;

public interface MangaPresenterListener {
    public static enum MangaEvent {
        ALL, CHAPTERS_DATA
        
    }
    void mangaEvent(MangaPresenter mangaPresenter, MangaEvent event);
    void chapterStateChange(MangaPresenter mangaPresenter, ChapterPresenter chapterPresenterListener, State oldValue, State newValue);
    void chapterSelectionChange(MangaPresenter mangaPresenter, ChapterPresenter chapterPresenterListener, boolean newValue);
}
