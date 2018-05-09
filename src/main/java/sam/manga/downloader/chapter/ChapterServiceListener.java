package sam.manga.downloader.chapter;

import javafx.concurrent.Worker.State;

interface ChapterServiceListener {
    public void stateChange(ChapterPresenter chapter, State oldValue, State newValue);
}
