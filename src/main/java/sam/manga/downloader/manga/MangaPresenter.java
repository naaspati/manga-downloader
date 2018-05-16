package sam.manga.downloader.manga;

import static sam.manga.downloader.extra.Utils.DOWNLOAD_DIR;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javafx.concurrent.Worker.State;
import sam.collection.Iterables;
import sam.fx.alert.FxAlert;
import sam.manga.downloader.chapter.Chapter;
import sam.manga.downloader.chapter.ChapterPresenter;
import sam.manga.downloader.chapter.ChapterPresenterListener;
import sam.manga.downloader.extra.Listeners;
import sam.manga.downloader.extra.Status;
import sam.manga.downloader.manga.MangaPresenterListener.MangaEvent;

public class MangaPresenter {
    private final BitSet completedCount = new BitSet(); 
    private final BitSet failedCount = new BitSet();
    private final BitSet remainingCount = new BitSet();
    private final BitSet queuedCount = new BitSet();
    private final BitSet selectedCount = new BitSet();
    private Status status;
    
    private ChapterPresenter[] chapters;

    private final int index;
    private final Manga manga;
    private final Listeners<MangaPresenterListener> listeners = new Listeners<>(MangaPresenterListener.class);

    public MangaPresenter(Manga manga, int index, MangaPresenterListener listener) {
        this.manga = manga;
        this.index = index;
        updateChapters();
        listeners.addListener(listener);
    }
    private void updateChapters() {
        int size = manga.chaptersCount();
        this.chapters = new ChapterPresenter[size];

        IdentityHashMap<Chapter, ChapterPresenter> map = new  IdentityHashMap<>();
        forEach(c -> {
            c.removeListener(chapterListener);
            map.put(c.getChapter(), c);
        });
        
        int n = 0;
        for (Chapter c : manga) {
            if(c == null)
                continue;
            
            ChapterPresenter cp = map.get(c);
            if(cp == null)
                cp = new ChapterPresenter(c, n);
            else
                cp = new ChapterPresenter(cp, n);

            cp.addListener(chapterListener);
            this.chapters[n++] = cp;
        }
        if(n < chapters.length)
            this.chapters = Arrays.copyOf(chapters, n);
        
        refresh();
    }
    
    private final ChapterPresenterListener chapterListener = new ChapterPresenterListener() {
        @Override
        public void stateChange(ChapterPresenter chapter, State oldValue, State newValue) {
            int n = chapter.getIndex();
            //TODO
            
            switch (newValue) {
                case CANCELLED:
                    
                    break;
                case FAILED:
                    break;
                case READY:
                    break;
                case RUNNING:
                    break;
                case SCHEDULED:
                    break;
                case SUCCEEDED:
                    break;
                default:
                    break;
            }
            listeners.apply(l -> l.chapterStateChange(MangaPresenter.this, chapter, oldValue, newValue));
        }
        
        @Override public void selectionChange(ChapterPresenter chapter, boolean newValue) {
            listeners.apply(l -> l.chapterSelectionChange(MangaPresenter.this, chapter, newValue));
        }
        @Override public void chapterNameChange(ChapterPresenter chapter, String chapterName) {}
    };

    public void refresh() {
        selectedCount.clear();
        queuedCount.clear();
        remainingCount.clear();
        failedCount.clear();
        completedCount.clear();

        for (int i = 0; i < chapters.length; i++) {
            ChapterPresenter cp = chapters[i];

            if(cp.isSelected()) selectedCount.set(i);
            if(cp.isCompleted()) completedCount.set(i);
            if(cp.isFailed()) failedCount.set(i);
            if(cp.isQueued()) remainingCount.set(i);
        }

        queuedCount.or(completedCount);
        queuedCount.or(failedCount);
        queuedCount.or(remainingCount);
        
        listeners.apply(l -> l.mangaEvent(this, MangaEvent.ALL));
    }



    
    /**
     * 
     * @return true if manga has at-least one active chapter download
     */
    public boolean isDownloading(){
        return status == Status.RUNNING;  
    }
    public String getMangaPath() {
        return DOWNLOAD_DIR.resolve(String.valueOf(manga.getId())).toString();
    }
    public void cancelAllDownload() {
        forEach(ChapterPresenter::cancel);
    }
    public void setAllChapterSelected(boolean b) {
        for (ChapterPresenter c : chapters) c.setSelected(b);
    }
    public Manga getManga() {
        return manga;
    }
    public void selectRangeChapters() {
        FxAlert.showMessageDialog("Not implemented", "No Code", false);
    }
    public void startDownload() {
        for (ChapterPresenter d : chapters) d.startDownload();
    }
    /**
     * 
     * @param pageIds -> set contaning DownloadablePage.ID
     * @param sink  -> map in which DownloadablePage.ID -> DownloadablePage.savePath will be put
     */
    public void fillPageSavePaths(Set<Integer> pageIds, Map<Integer, Path> sink) {
        forEach(c -> c.fillPageSavePaths(pageIds, sink));
    }

    public void updateIfHasPages(HashSet<Integer> successIds) {
        for (ChapterPresenter d : chapters) 
            d.updateIfHasPages(successIds);
    }
    public void retryFailedChapters(){
        if(getFailedCount() == 0)
            return;

        for (ChapterPresenter c : chapters) c.restart();
    }
    
    private int count(BitSet b) {
        return b == null ? 0 : b.cardinality();
    }

    public int getChaptersCount() { return chapters.length; }
    public String getMangaName() { return manga.getMangaName(); }
    public int getCompletedCount() { return count(completedCount); }
    public int getFailedCount() { return count(failedCount); }
    public int getRemainingCount() { return count(remainingCount); }
    public int getQueuedCount() { return count(queuedCount); }
    public int getSelectedCount() { return count(selectedCount); }
    public int chaptersCount() { return manga.chaptersCount(); }
    
    public int getPageCount() {
        if(chapters.length == 0)
            return 0;

        int n = 0;
        for (ChapterPresenter c : chapters) {
            if(c == null) continue;
            n += c.pagesCount();
        }
        return n;
    }
    public int getIndex() {
        return index;
    }
    public void addListener(MangaPresenterListener listener) {
        this.listeners.addListener(listener);
    }
    public Iterable<ChapterPresenter> iterable(){
        return Iterables.of(chapters);
    }
    public void forEach(Consumer<ChapterPresenter> consumer) {
        if(chapters == null) return;

        for (ChapterPresenter c : chapters)
            if(c != null) consumer.accept(c);
    }
    public Stream<ChapterPresenter> stream() {
        return Arrays.stream(chapters);
    }
}
