package sam.manga.downloader.chapter;

public interface ChapterPresenterListener extends ChapterServiceListener {
    void selectionChange(ChapterPresenter chapter, boolean newValue);
    void chapterNameChange(ChapterPresenter chapter, String chapterName);
}
