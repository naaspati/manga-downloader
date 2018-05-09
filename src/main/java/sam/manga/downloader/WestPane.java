package sam.manga.downloader;

import static sam.fx.helpers.FxClassHelper.setClass;
import static sam.fx.helpers.FxClassHelper.toggleClass;

import java.util.List;
import java.util.function.Consumer;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.effect.Glow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.collection.UnmodifiableArray;
import sam.fx.helpers.FxLabel;
import sam.manga.downloader.chapter.ChapterPresenter;
import sam.manga.downloader.chapter.ChapterPresenterListener;
import sam.manga.downloader.data.DataManager;
import sam.manga.downloader.extra.SaveRecords;
import sam.manga.downloader.extra.Utils;
import sam.manga.downloader.manga.Manga;
import sam.manga.downloader.manga.MangaPresenter;
import sam.manga.downloader.manga.MangaPresenterListener;
import sam.weak.WeakStore;

public class WestPane extends BorderPane {
    private static volatile WestPane instance;

    private static final WeakStore<StringBuilder> builders = new WeakStore<>(StringBuilder::new);

    @FXML private Text mangaCount;
    @FXML private Text chapterCount;
    @FXML private Text pageCount;
    @FXML private Text selectedCount;
    @FXML private Text queuedCount;
    @FXML private Text completedCount;
    @FXML private Text failedCount;
    @FXML private Text remainingCount;
    @FXML private ListView<MangaPresenter> listView;
    @FXML private Button downloadButton;

    private final int[] chapterCounts;
    private final int[] pageCounts;
    private final int[] selectedCounts;
    private final int[] queuedCounts;
    private final int[] completedCounts;
    private final int[] failedCounts;
    private final int[] remainingCounts;
    
    private final ReadOnlyIntegerWrapper failedCountPropery = new ReadOnlyIntegerWrapper();

    private CenterView centerView;
    private final ReadOnlyBooleanWrapper downloadIsActive = new ReadOnlyBooleanWrapper(null, "download_is_active",
            false);
    private final ReadOnlyObjectProperty<MangaPresenter> currentManga;
    private MangaPresenter[] mangaPresenters;

    public WestPane() {
        Utils.fxml(this);
        Utils.stylesheet(this);
        downloadButton.setOnAction(this::startDownload);

        currentManga = listView.getSelectionModel().selectedItemProperty();
    }
    
    public void init(List<Manga> mangas) {
        int size = mangas.size();
        mangaPresenters = new MangaPresenter[size];
        int n = 0;
        for (Manga m : mangas) {
            MangaPresenter mp = new MangaPresenter(m, n++);
            mp.addListener(listeners);
            mangaPresenters[n++] = mp;
        }

        chapterCounts = new int[size];
        pageCounts = new int[size];
        selectedCounts = new int[size];
        queuedCounts = new int[size];
        completedCounts = new int[size];
        failedCounts = new int[size];
        remainingCounts = new int[size];

        mangaCount.setText(String.valueOf(mangaPresenters.length));
    }

    public boolean isDownloadActive() {
        return downloadIsActive.get();
    }

    public ReadOnlyBooleanProperty downloadIsActiveProperty() {
        return downloadIsActive.getReadOnlyProperty();
    }

    private final MangaPresenterListener listeners = new MangaPresenterListener() {
        void updateAll(MangaPresenter manga) {
            int index = manga.getIndex();

            chapterCounts[index] = manga.getChaptersCount();
            pageCounts[index] = manga.getPageCount();
            selectedCounts[index] = manga.getSelectedCount();
            queuedCounts[index] = manga.getQueuedCount();
            completedCounts[index] = manga.getCompletedCount();
            failedCounts[index] = manga.getFailedCount();
            remainingCounts[index] = manga.getRemainingCount();

            set(chapterCount, chapterCounts);
            set(pageCount, pageCounts);
            set(selectedCount, selectedCounts);
            set(queuedCount, queuedCounts);
            set(completedCount, completedCounts);
            set(failedCount, failedCounts);
            set(remainingCount, remainingCounts);
        }

        @Override
        public void mangaEvent(MangaPresenter manga, MangaEvent event) {
            int index = manga.getIndex();
            
            switch (event) {
                case ALL:
                    updateAll(manga);
                    break;
                case CHAPTERS_DATA:
                    chapterCounts[index] = manga.getChaptersCount();
                    pageCounts[index] = manga.getPageCount();
                    set(chapterCount, chapterCounts);
                    set(pageCount, pageCounts);
                    break;                    
                default:
                    break;
            }
        }

        @Override
        public void chapterStateChange(MangaPresenter manga, ChapterPresenter chapter, State oldValue, State newValue) {
            int index = manga.getIndex();
            
            if(newValue == State.CANCELLED || newValue == State.FAILED) {
//TODO                failedCounts
                //TODO
            }
                

            switch (newValue) {
                case CANCELLED:
                    setFailed(manga, chapter);
                    break;
                case FAILED:
                    setFailed(manga, chapter);
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
            switch (kind) {
                case ALL_COUNT:
                    break;
                case SELECTED_COUNT:
                    selectedCounts[index] = manga.getSelectedCount();
                    set(selectedCount, selectedCounts);
                    break;
                case QUEUED_COUNT:
                    queuedCounts[index] = manga.getQueuedCount();
                    set(queuedCount, queuedCounts);
                    break;
                case COMPLETED_COUNT:
                    completedCounts[index] = manga.getCompletedCount();
                    set(completedCount, completedCounts);
                    break;
                case FAILED_COUNT:
                    failedCounts[index] = manga.getFailedCount();
                    set(failedCount, failedCounts);
                    break;
                case REMAINING_COUNT:
                    remainingCounts[index] = manga.getRemainingCount();
                    set(remainingCount, remainingCounts);
                    break;

            }
        }

        @Override
        public void chapterSelectionChange(MangaPresenter manga, ChapterPresenter chapter, boolean newValue) {
        }
    };
    
    public ReadOnlyIntegerProperty failedCountPropery() {
        return failedCountPropery.getReadOnlyProperty();
    }
    private void set(Text t, int[] data) {
        if(data == failedCounts) {
            int n = Utils.sum(data);
            failedCountPropery.set(n);
            t.setText(String.valueOf(n));
        } else
            t.setText(String.valueOf(Utils.sum(data)));
    }

    public void startDownload(ActionEvent event) {
        downloadIsActive.set(true);
        forEach(MangaPresenter::startDownload);
    }

    public void forEach(Consumer<MangaPresenter> consumer) {
        for (MangaPresenter m : mangaPresenters)
            consumer.accept(m);
    }

    private static final String[] removeClassNameArray = { "running", "completed", "queued", "failed" };

    private class MangaButton extends ListCell<MangaPresenter> implements MangaPresenterListener {
        private final Label mangaName = FxLabel.label(null, "name-text");
        private final Label counts = FxLabel.label(null, "count-text");
        private final VBox box = new VBox(mangaName, counts);
        private MangaPresenter manga;
        {
            setClass(this, "manga-button");
            setClass(box, "vbox");
            box.setEffect(new Glow());
            box.setMaxWidth(Double.MAX_VALUE);
            box.setFillWidth(true);
        }

        private void addClass(String className) {
            if (!box.getStyleClass().contains(className)) {
                box.getStyleClass().removeAll(removeClassNameArray);
                box.getStyleClass().add(className);
            }
        }

        @Override
        public void updateSelected(boolean selected) {
            super.updateSelected(selected);
            toggleClass(this, "selected", selected);
        }

        @Override
        protected void updateItem(MangaPresenter item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty) {
                mangaName.setText(null);
                counts.setText(null);
                manga = null;
                setGraphic(null);
            } else {
                this.manga = item;
                mangaName.setText(item.getMangaName());
                item.setUpdateListener(this);
                setGraphic(box);
                updateCount();
            }
        }

        @Override
        public void updated(MangaPresenter manga, MangaPresenter.Kind kind) {
            if (this.manga != null)
                updateCount();

            listeners.updated(manga, kind);
        }

        private void updateCount() {
            StringBuilder sb = builders.poll();

            int n = manga.getCompletedCount();
            if (n != 0)
                sb.append("C: ").append(n).append(" | ");
            n = manga.getFailedCount();
            if (n != 0)
                sb.append("F: ").append(n).append(" | ");
            n = manga.getRemainingCount();
            if (n != 0)
                sb.append("R: ").append(n).append(" | ");
            n = manga.getQueuedCount();
            if (n != 0)
                sb.append("Q: ").append(n).append(" | ");
            n = manga.getSelectedCount();
            if (n != 0)
                sb.append("S: ").append(n).append(" | ");

            sb.append("T: ").append(manga.chaptersCount());
            counts.setText(sb.toString());

            sb.setLength(0);
            builders.add(sb);
        }
    }
    public ReadOnlyObjectProperty<MangaPresenter> currentMangaProperty() {
        return currentManga;
    }

    public void stop() {
        if (isDownloadIsActive())
            forEach(MangaPresenter::cancelAllDownload);

        forEach(m -> new SaveRecords(m)); // TODO
    }
}
