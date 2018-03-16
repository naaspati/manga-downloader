package sam.manga.downloader.view.manga;

import static sam.fx.helpers.FxHelpers.removeClass;
import static sam.fx.helpers.FxHelpers.setClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.effect.Glow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.console.ansi.ANSI;
import sam.fx.alert.FxAlert;
import sam.manga.downloader.DownloaderApp;
import sam.manga.downloader.parts.Chapter;
import sam.manga.downloader.parts.Manga;
import sam.manga.downloader.parts.Page;
import sam.manga.downloader.view.chapter.ChapterPresenter;
import sam.myutils.fileutils.renamer.RemoveInValidCharFromString;

public class MangaPresenter  {
    private final VBox downloadingView = new VBox(3);
    private final ScrollPane downloadingViewScrollPane = new ScrollPane(downloadingView);

    private final VBox controlBox = new VBox(5);

    private final ReadOnlyIntegerWrapper completedCount = new ReadOnlyIntegerWrapper(this, "completed", 0); 
    private final ReadOnlyIntegerWrapper failedCount = new ReadOnlyIntegerWrapper(this, "failed", 0);
    private final ReadOnlyIntegerWrapper remainingCount = new ReadOnlyIntegerWrapper(this, "remaining", 0);
    private final ReadOnlyIntegerWrapper queuedCount = new ReadOnlyIntegerWrapper(this, "selected", 0);
    private final ReadOnlyIntegerWrapper selectedCount = new ReadOnlyIntegerWrapper(this, "selected", 0);

    private final ReadOnlyBooleanWrapper isSelectedButNotQueued = new ReadOnlyBooleanWrapper(this, "is_selected_but_not_queued", false);
    private final ReadOnlyBooleanWrapper dataUpdated = new ReadOnlyBooleanWrapper(this, "database_update", false);

    private final Manga manga;
    private final  ChapterPresenter[] chapters;
    private static volatile VBox clickOwner = null;

    public MangaPresenter(Manga manga, MangaEvent onClick) {
        this.manga = manga;
        downloadingViewScrollPane.setFitToWidth(true);
        this.chapters = new ChapterPresenter[manga.chaptersCount()];

        int n = 0;
        for (Chapter c : manga)
            this.chapters[n++] = new ChapterPresenter(c);

        setClass(controlBox, "downloadable-manga", "manga-control-box");
        controlBox.setEffect(new Glow());
        controlBox.setMaxWidth(Double.MAX_VALUE);

        controlBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if(clickOwner != null)
                removeClass(clickOwner, MANGA_BOX_CLICKED);

            clickOwner = controlBox;
            addClass(MANGA_BOX_CLICKED);

            onClick.clicked(this);
            fillDownloadingView();

            e.consume();
        });

        Label mangaNameText = new Label(manga.getName());
        mangaNameText.getStyleClass().addAll("downloadable-manga", "manga-name-text");
        mangaNameText.setMaxWidth(Double.MAX_VALUE);

        final Text countsText = new Text();
        countsText.getStyleClass().addAll("downloadable-manga", "manga-counts-text");

        bindBindings(mangaNameText, countsText);
        addStateListeners();

        final ReadOnlyBooleanProperty[] array = stream().map(ChapterPresenter::dataUpdatedProperty).toArray(ReadOnlyBooleanProperty[]::new);
        dataUpdated.bind(Bindings.createBooleanBinding(() -> Stream.of(array).anyMatch(ReadOnlyBooleanProperty::get), array));
        downloadingView.getChildren().addAll(chapters);
    }

    public Stream<ChapterPresenter> stream(){
        return Arrays.stream(chapters);
    }

    private void bindBindings(Label mangaNameText, Text countsText) {
        //why i chose to updated selectedCount manually ? 
        //binding will unwanted create updates when c + f + r  is same after changes   
        //selectedCount.bind(completedCount.add(failedCount).add(remainingCount));

        StringExpression binding = Bindings.concat(
                Bindings.when(completedCount.isEqualTo(0)).then("").otherwise(Bindings.concat("C: ", completedCount)),
                Bindings.when(failedCount.isEqualTo(0)).then("").otherwise(Bindings.concat("| F: ", failedCount)),
                Bindings.when(remainingCount.isEqualTo(0)).then("").otherwise(Bindings.concat("| R: ", remainingCount)),
                Bindings.when(queuedCount.isEqualTo(0)).then("").otherwise(Bindings.concat("| Q: ", queuedCount)),
                Bindings.when(selectedCount.isEqualTo(0)).then("").otherwise(Bindings.concat("| S: ", selectedCount)),
                "| T: " + manga.chaptersCount());

        int c = 0, f = 0, r = 0;
        for (ChapterPresenter d : chapters) {
            Chapter chap = d.getChapter();
            if(chap.isCompleted()) c++;
            else if(chap.isFailed()) f++;
            else if(chap.isQueued()) r++;
        }

        completedCount.set(c);
        failedCount.set(f);
        queuedCount.set(c + f + r);
        remainingCount.set(r);

        countsText.textProperty().bind(binding);

        controlBox.getChildren().addAll(mangaNameText, countsText);

        ReadOnlyBooleanProperty[] booleanArray = stream().map(ChapterPresenter::isSelectedButNotQueuedProperty).toArray(ReadOnlyBooleanProperty[]::new);
        BooleanBinding isSelectedBinding = Bindings.createBooleanBinding(() -> Stream.of(booleanArray).anyMatch(ReadOnlyBooleanProperty::get), booleanArray);
        isSelectedButNotQueued.bind(isSelectedBinding);
        IntegerBinding selectedCountBinding = Bindings.createIntegerBinding(() -> (int)Stream.of(booleanArray).filter(ReadOnlyBooleanProperty::get).count(), booleanArray);
        selectedCount.bind(selectedCountBinding);
    }

    public ReadOnlyIntegerProperty selectedCountProperty() {
        return selectedCount.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty dataUpdatedProperty(){
        return dataUpdated.getReadOnlyProperty();
    }

    private final AtomicInteger cAtomic = new AtomicInteger(0);
    private final AtomicInteger fAtomic = new AtomicInteger(0);
    private final AtomicInteger rAtomic = new AtomicInteger(0);

    private static final String[] removeClassNameArray = {
            "running", 
            "completed",
            "queued",
    "failed"};

    private void addStateListeners() {
        InvalidationListener li = e -> {
            boolean running = false;
            boolean scheduled = false;

            int c = 0, f = 0, r = 0;

            for (ChapterPresenter dcp : chapters) {
                Chapter ds = dcp.getChapter();
                State state = dcp.getState();

                if(state == State.RUNNING)
                    running = true;
                if(state == State.SCHEDULED)
                    scheduled = true;

                if(ds.isCompleted())
                    c++;
                else if(ds.isFailed())
                    f++;

                if(running || scheduled)
                    r++;
            }

            if(running || scheduled || r == 0){
                if(running)
                    addClass("running");
                else if(scheduled)
                    addClass("queued");
                else if(r == 0)
                    addClass(f > 0 ? "failed" : "completed");
            }

            cAtomic.getAndSet(c);
            fAtomic.getAndSet(f);
            rAtomic.getAndSet(r);

            Platform.runLater(() -> {
                int c1 = cAtomic.get();
                int f1 = fAtomic.get();
                int r1 = rAtomic.get();
                int q = c1 + f1 + r1;

                if(completedCount.get() != c1)
                    completedCount.set(c1);
                if(failedCount.get() != f1)
                    failedCount.set(f1);
                if(remainingCount.get() != r1)
                    remainingCount.set(r1);
                if(queuedCount.get() != q)
                    queuedCount.set(q);
            });
        };

        stream().map(ChapterPresenter::stateProperty).forEach(s -> s.addListener(li));
    }

    private void addClass(String className) {
        if(!controlBox.getStyleClass().contains(className)){
            controlBox.getStyleClass().removeAll(removeClassNameArray);
            controlBox.getStyleClass().add(className);
        }
    }

    private void fillDownloadingView() {
        ObservableList<Node> list = downloadingView.getChildren();
        list.clear();

        for (ChapterPresenter d : chapters){
            if(d.getChapter().isQueued())
                list.add(d);
        }
        for (ChapterPresenter d : chapters){
            if(d.getChapter().isFailed())
                list.add(d);
        }
        for (ChapterPresenter d : chapters){
            if(d.getChapter().isCompleted()){
                list.remove(d);
                list.add(d);
            }
        }
    }

    /**
     * 
     * @return true if manga has at-least one active chapter download
     */
    public boolean isDownloading(){
        return controlBox.getStyleClass().contains("running");  
    }
    private static final String MANGA_BOX_CLICKED = "manga-box-clicked";

    public String getMangaPath() {
        return DownloaderApp.DOWNLOAD_DIR.resolve(String.valueOf(manga.getId())).toString();
    }

    public ReadOnlyIntegerProperty completedCountProperty(){
        return completedCount.getReadOnlyProperty();
    }

    public ReadOnlyIntegerProperty failedCountProperty(){
        return failedCount.getReadOnlyProperty();
    }

    public ReadOnlyIntegerProperty remainingCountProperty(){
        return remainingCount.getReadOnlyProperty();
    }

    public ReadOnlyIntegerProperty queuedCountProperty(){
        return queuedCount.getReadOnlyProperty();
    }

    public void fillChapterPresentersView(ObservableList<ChapterPresenter> list) {
        list.setAll(chapters);
    }

    public void cancelAllDownload() {
        for (ChapterPresenter d : chapters) d.cancel();
    }

    public void setAllChapterSelected(boolean b) {
        for (ChapterPresenter c : chapters) c.setSelected(b);
    }

    public void selectMissing(TreeMap<Double, String> chapters, TextArea logArea) {
        StringBuilder builder = new StringBuilder();
        ANSI.yellow(builder, manga.getId()).append('\t');
        ANSI.yellow(builder, manga.getName()).append('\n');
        ArrayList<Double> foundNumbers = new ArrayList<>();

        for (ChapterPresenter cp : this.chapters) {
            Chapter c = cp.getChapter();
            String name = chapters.get(c.getNumber()); 
            if(name != null){
                cp.setSelected(true);
                builder.append("    ")
                .append(c.getNumber())
                .append("   ")
                .append(name).append('\n');
                foundNumbers.add(c.getNumber());
            }
        }

        if(!foundNumbers.isEmpty()){
            logArea.appendText(builder.toString());
            chapters.keySet().removeAll(foundNumbers);
        }
    }
    public Manga getManga() {
        return manga;
    }
    public void saveRecords(){
        Path path  = DownloaderApp.LOGS_FOLDER.resolve(RemoveInValidCharFromString.removeInvalidCharsFromFileName(manga.getName())+".npp");

        if(!stream().flatMap(cp -> cp.getChapter().stream()).anyMatch(Page::hasError)){
            try {
                Files.createDirectories(path.getParent());
                Files.deleteIfExists(path);
            } catch (IOException e) {
                FxAlert.showErrorDialog("failed to delete\nFile: "+path, "Manga.saveRecords() Error", e, false);
            }
            return;
        }

        StringBuilder main = new StringBuilder()
                .append(manga.getId()).append('\n')
                .append(manga.getName()).append('\n')
                .append(manga.getUrl()).append('\n')
                .append("\n")
                .append(manga.getId()).append(" \n");


        StringBuilder id_urls = new StringBuilder();

        stream().filter(c -> c.getChapter().stream().anyMatch(Page::hasError))
        .peek(c -> {
            main.deleteCharAt(main.length() - 1);
            main.append(c.getChapter().getNumber()).append(" \n");
        }).forEach(c -> c.fillErrors(main, id_urls));

        main.append("\n\n------------------------------------------\n\n").append(id_urls);

        try {
            Files.write(path, main.toString().getBytes("utf-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {}
    }

    public void selectRangeChapters() {
        FxAlert.showMessageDialog("Not implemented", "No Code", false);
    }
 public   ReadOnlyBooleanProperty isSelectedButNotQueuedProperty() {
        return isSelectedButNotQueued.getReadOnlyProperty();
    }

    public boolean isSelected() {
        return isSelectedButNotQueued.get();
    }

    public void startDownload() {
        if(!isSelected())
            return;

        for (ChapterPresenter d : chapters) d.startDownload();
        queuedCount.set((int)Stream.of(manga).filter(c -> c.isQueued()).count());

        fillDownloadingView();
    }

    public int getPagesCount() {
        return stream().mapToInt(c -> c.getChapter().getPageCount()).sum();
    }

    public Node getDownloadingView() {
        return downloadingViewScrollPane;
    }

    public  Node getControlBox() {
        return controlBox;
    }

    public boolean hasClick(){
        return controlBox == clickOwner;
    }

    public boolean isAllChaptersSelected() {
        return stream().allMatch(ChapterPresenter::isSelected);
    }

    /**
     * 
     * @param pageIds -> set contaning DownloadablePage.ID
     * @param sink  -> map in which DownloadablePage.ID -> DownloadablePage.savePath will be put
     */
    public void fillPageSavePaths(Set<Integer> pageIds, Map<Integer, Path> sink) {
        stream().forEach(c -> c.fillPageSavePaths(pageIds, sink));
    }

    public void updateIfHasPages(HashSet<Integer> successIds) {
        for (ChapterPresenter d : chapters) 
            d.updateIfHasPages(successIds);
    }

    public void removeCompletedChaptersFromView() {
        downloadingView.getChildren().removeIf(d -> ((ChapterPresenter)d).getChapter().isCompleted());
    }

    public void retryFailedChapters(){
        if(failedCount.get() == 0)
            return;

        for (ChapterPresenter c : chapters) c.restart();
    }

    public int getChaptersCount() {
        return chapters.length;
    }
}
