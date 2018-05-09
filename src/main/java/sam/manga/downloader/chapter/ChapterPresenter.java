package sam.manga.downloader.chapter;

import static javafx.concurrent.Worker.State.FAILED;
import static javafx.concurrent.Worker.State.READY;
import static javafx.concurrent.Worker.State.SCHEDULED;
import static javafx.concurrent.Worker.State.SUCCEEDED;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javafx.concurrent.Worker.State;
import sam.manga.downloader.extra.Listeners;
import sam.manga.downloader.page.Page;

public class ChapterPresenter {
    public static final short SELECTED = 0;
    public static final short UNSELECTED = 1;
    public static final short NOT_SELECTABLE = 2;

    private final int index = 0;
    private short selected = UNSELECTED;
    private final Chapter chapter;
    private final ChapterService service;
    private final Listeners<ChapterPresenterListener> listeners;

    public ChapterPresenter(Chapter chapter, int index) {
        this.chapter = chapter;
        this.listeners = new Listeners<>(ChapterPresenterListener.class);

        if(chapter.getState() == SUCCEEDED)
            service = null;
        else {
            service = new ChapterService(chapter);
            service.stateProperty()
            .addListener((p, o, n) -> {
                chapter.setState(n);
                listeners.apply(s -> s.stateChange(this, o, n));
            });
        }
    }
    public ChapterPresenter(ChapterPresenter cp, int index) {
        this.selected = cp.selected;
        this.chapter = cp.chapter;
        this.service = cp.service;
        this.listeners = cp.listeners;
    }
    public int getIndex() {
        return index;
    }
    short getSelected() {
        return selected;
    }
    public State getState() {
        return service == null ? SUCCEEDED : service.getState();
    }
    public boolean cancel() {
        if(service == null) return false;
        return service.cancel();
    }
    public void restart() {
        if(service != null && isSelected()) 
            service.restart();
    }
    public boolean isRunning() {
        return service != null && service.isRunning();
    }
    public void start() {
        service.start();
    }
    public Chapter getChapter() {
        return chapter;
    }
    private void setCompleted() {
        selected = NOT_SELECTABLE;
    }
    public boolean isSelected() {
        return selected == SELECTED;
    }
    public  void setSelected(boolean b) {
        if(chapter.isCompleted() && isSelected()){
            setCompleted();
            return;
        }
        if(!isSelected() && (service.isRunning() || service.getState() == State.SCHEDULED))
            cancel();

        short s = b ? SELECTED : UNSELECTED;
        if(selected != NOT_SELECTABLE && selected != s) {
            selected = s;
            listeners.apply(l -> l.selectionChange(this, selected == SELECTED));
        }

        /**
         * isSelectedButNotQueued.set(isSelectable() && b);
        if(isSelectable())
            this.selected = b;
         */
    }
    /**
     * public void fillSummery(List<Integer> completedChapters, List<Chapter> resetChapters, List<Page> resetPages) {
        if(!dataUpdated.get())
            return;
        if(chapter.isCompleted())
            completedChapters.add(chapter.chapterId);
        else{
            resetChapters.add(chapter);
            for (Page p : chapter) {
                if(p.isModified())
                resetPages.add(p);
            }
        }
        dataUpdated.set(false);
    }
     */

    public void startDownload() {
        if(!isSelected())
            return;

        if(chapter.isCompleted()){
            setCompleted();
            return;
        }
        if(service.getState() == SUCCEEDED || service.getState() == State.RUNNING)
            return;

        if(service.getState() == READY)
            service.start();
        else
            service.restart();
    }
    public void fillPageSavePaths(Set<Integer> pageIds, Map<Integer, Path> sink) {
        for (Page p : chapter) {
            if(pageIds.contains(p.pageId))
                sink.put(p.pageId, PageDownloader.getSavePath(chapter, p));
        }
    }
    //this will set completed to the pages whose ID is in successIds 
    public void updateIfHasPages(HashSet<Integer> successIds) {
        boolean update = false;
        for (Page p : chapter){ 
            if(successIds.contains(p.pageId)){
                p.setCompleted();
                update = true;
            }
        }

        if(update)
            service.restart();
    }
    public int pagesCount() {
        return chapter.size();
    }
    ChapterService getChapterService() {
        return service;
    }
    public boolean isCompleted() {
        return chapter.isCompleted();
    }
    private boolean is(State s) {
        return !chapter.isCompleted() && service.getState() == s; 
    }
    public boolean isFailed() {
        return is(FAILED);
    }
    public boolean isQueued() {
        return is(SCHEDULED);
    }
    public void removeListener(ChapterPresenterListener l) {
        listeners.removeListener(l);
    }
    public void addListener(ChapterPresenterListener t) {
        listeners.addListener(t);
    }
}