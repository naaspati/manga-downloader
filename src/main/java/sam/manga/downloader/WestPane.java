package sam.manga.downloader;

import static sam.fx.helpers.FxClassHelper.setClass;
import static sam.fx.helpers.FxClassHelper.toggleClass;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
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
import sam.fx.helpers.FxLabel;
import sam.manga.downloader.chapter.ChapterPresenter;
import sam.manga.downloader.extra.SaveRecords;
import sam.manga.downloader.extra.Utils;
import sam.manga.downloader.manga.Manga;
import sam.manga.downloader.manga.MangaPresenter;
import sam.manga.downloader.manga.MangaPresenterListener;
import sam.weak.WeakStore;

public class WestPane extends BorderPane {
    private final WeakStore<StringBuilder> builders = new WeakStore<>(StringBuilder::new);

    @FXML private Text mangaCount;
    @FXML private CountText chapterCount;
    @FXML private CountText pageCount;
    @FXML private CountText selectedCount;
    @FXML private CountText queuedCount;
    @FXML private CountText completedCount;
    @FXML private CountText failedCount;
    @FXML private CountText remainingCount;
    
    @FXML private ListView<MangaPresenter> listView;
    @FXML private Button downloadButton;
    
    private final ReadOnlyIntegerWrapper failedCountPropery = new ReadOnlyIntegerWrapper();
    private MangaButton[] mangaButtons;

    // private CenterView centerView;
    private final ReadOnlyBooleanWrapper downloadIsActive = new ReadOnlyBooleanWrapper(null, "download_is_active", false);
    private final ReadOnlyObjectProperty<MangaPresenter> currentManga;
    private MangaPresenter[] mangaPresenters;

    public WestPane() {
        Utils.fxml(this);
        Utils.stylesheet(this);
        currentManga = listView.getSelectionModel().selectedItemProperty();
    }
    
    public void init(List<Manga> mangas) {
        int size = mangas.size();
        mangaPresenters = new MangaPresenter[size];
        int n = 0;
        
        for (Manga m : mangas) 
            mangaPresenters[n] = new MangaPresenter(m, n++, mangaListener);
        
        mangaButtons = new MangaButton[size];

        chapterCount.setSize(size);
        pageCount.setSize(size);
        selectedCount.setSize(size);
        queuedCount.setSize(size);
        completedCount.setSize(size);
        failedCount.setSize(size);
        remainingCount.setSize(size);

        mangaCount.setText(String.valueOf(mangaPresenters.length));
    }

    public boolean isDownloadActive() {
        return downloadIsActive.get();
    }

    public ReadOnlyBooleanProperty downloadIsActiveProperty() {
        return downloadIsActive.getReadOnlyProperty();
    }

    private final MangaPresenterListener mangaListener = new MangaPresenterListener() {
        private void update(int index) {
            if(mangaButtons[index] != null)
                mangaButtons[index].updateCount();
        }
        
        void updateAll(MangaPresenter manga) {
            int index = manga.getIndex();

            chapterCount.set(index,  manga.getChaptersCount());
            pageCount.set(index,  manga.getPageCount());
            selectedCount.set(index,  manga.getSelectedCount());
            queuedCount.set(index,  manga.getQueuedCount());
            completedCount.set(index,  manga.getCompletedCount());
            failedCount.set(index,  manga.getFailedCount());
            remainingCount.set(index,  manga.getRemainingCount());
        }
        @Override
        public void mangaEvent(MangaPresenter manga, MangaEvent event) {
            int index = manga.getIndex();
            
            switch (event) {
                case ALL:
                    updateAll(manga);
                    break;
                case CHAPTERS_DATA:
                    chapterCount.set(index, manga.getChaptersCount());
                    pageCount.set(index, manga.getPageCount());
                    break;                    
            }
            update(index);
        }

        @Override
        public void chapterStateChange(MangaPresenter manga, ChapterPresenter chapter, State oldValue, State newValue) {
            int index = manga.getIndex();
            
            if(newValue == State.CANCELLED || newValue == State.FAILED) {
//TODO                failedCounts
                //TODO
            }
            
            /**
             *             switch (newValue) {
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
             */
            
            update(index);
        }

        @Override
        public void chapterSelectionChange(MangaPresenter manga, ChapterPresenter chapter, boolean newValue) {
         
            //TODO
            // update(index);
        }
    };
    
    public ReadOnlyIntegerProperty failedCountPropery() {
        return failedCountPropery.getReadOnlyProperty();
    }
    @FXML
    public void startDownload(ActionEvent event) {
        downloadIsActive.set(true);
        forEach(MangaPresenter::startDownload);
    }

    public void forEach(Consumer<MangaPresenter> consumer) {
        for (MangaPresenter m : mangaPresenters)
            consumer.accept(m);
    }

    private static final String[] removeClassNameArray = { "running", "completed", "queued", "failed" };

    private class MangaButton extends ListCell<MangaPresenter> {
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
            //TODO
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
                if(manga != null)
                mangaButtons[manga.getIndex()] = null;
                manga = null;
                setGraphic(null);
            } else {
                this.manga = item;
                mangaName.setText(item.getMangaName());
                mangaButtons[item.getIndex()] = this; 
                setGraphic(box);
                updateCount();
            }
        }
        int index;
        StringBuilder sb;
        
        private void updateCount() {
            sb = builders.poll();
            index = manga.getIndex();
            
            set(completedCount, "C: ");
            set(failedCount, "F: ");
            set(remainingCount, "R: ");
            set(queuedCount, "Q: ");
            set(selectedCount, "S: ");
            
            sb.append("T: ").append(chapterCount.get(index));
            counts.setText(sb.toString());

            sb.setLength(0);
            builders.add(sb);
            sb = null;
        }
        public void set(CountText c, String s) {
            sb.append(s).append(c.get(index)).append(" | ");
        }
    }
    public ReadOnlyObjectProperty<MangaPresenter> currentMangaProperty() {
        return currentManga;
    }
    public void stop() {
        if (downloadIsActive.get())
            forEach(MangaPresenter::cancelAllDownload);
        new SaveRecords(getMangas());
    }
    public List<MangaPresenter> getMangas() {
        return Collections.unmodifiableList(listView.getItems());
    }
}
