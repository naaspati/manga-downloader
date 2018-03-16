package sam.manga.downloader.view.chapter;

import static sam.manga.downloader.extra.Status.COMPLETED;
import static sam.manga.downloader.extra.Status.FAILED;
import static sam.manga.downloader.extra.Status.UNTOUCHED;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.fx.clipboard.FxClipboard;
import sam.fx.helpers.FxHelpers;
import sam.fx.popup.FxPopupShop;
import sam.manga.downloader.DownloaderApp;
import sam.manga.downloader.DownloaderUtils;
import sam.manga.downloader.PageDownload;
import sam.manga.downloader.extra.Status;
import sam.manga.downloader.parts.Chapter;
import sam.manga.downloader.parts.Page;

public class ChapterPresenter extends VBox {
    private final ProgressBar progressbar;
    private final Text nameText;
    private final CheckBox isSelectedCheckBox;
    private final ReadOnlyBooleanWrapper isSelectedButNotQueued = new ReadOnlyBooleanWrapper(this, "is_selected_but_not_queued", false);

    private final Chapter chapter;
    private final ChapterService chapterService;
    private final ReadOnlyBooleanWrapper dataUpdated = new ReadOnlyBooleanWrapper(this, "database_update", false);

    public ChapterPresenter(Chapter chapter) {
        super(5);
        this.chapter = chapter;
        this.chapterService = new ChapterService(chapter, dataUpdated);
        FxHelpers.setClass(this, "downloadable-chapter", "v-box");

        nameText = new Text(getChapterDisplayName());
        getChildren().add(nameText);

        progressbar = new ProgressBar(getCompletedCount()/getPagesCount());
        progressbar.setMaxWidth(Double.MAX_VALUE);
        progressbar.getStyleClass().add("downloadable-chapter");

        isSelectedCheckBox = new CheckBox(getChapterDisplayName());
        isSelectedCheckBox.setOnAction(e -> setSelected(isSelectedCheckBox.isSelected()));

        addStateListener();

        if(!chapter.isUntouched()){
            getChildren().add(progressbar);

            if(!chapter.isQueued())
                progressbar.getStyleClass().add(chapter.getStatus().getClassName());

            if(chapter.isCompleted())
                setCompleted();
            else {
                setSelected(!chapter.isUntouched());
                double count = getCompletedCount();
                if(chapter.isFailed()){
                    progressbar.setProgress(count == 0 ? 1 : count/getPagesCount());
                    addErrorPane();
                }
                else if(count > 0)
                    progressbar.setProgress(count/getPagesCount());
                else
                    getChildren().remove(progressbar);
            }
        }
    }
    public Chapter getChapter() {
        return chapter;
    }
    public ReadOnlyBooleanProperty dataUpdatedProperty() {
        return dataUpdated.getReadOnlyProperty();
    }
    private double getPagesCount() {
        return chapter.getPageCount();
    }


    private int getCompletedCount() {
        int count = 0;
        for (Page p : chapter) 
            if(p.isCompleted())
                count++;

        return count;
    }

    public synchronized EnumMap<Status, Long> getCounts() {
        EnumMap<Status, Long> map = new EnumMap<>(Status.class);
        return chapter.stream().collect(Collectors.groupingBy(Page::getStatus, () -> map, Collectors.counting()));
    }

    private static final String[] removeClassesArray = {COMPLETED.getClassName(), FAILED.getClassName()};
    private void addStateListener() {
        chapterService.stateProperty().addListener((prop, old, _new) -> {
            if(_new == Worker.State.RUNNING){
                if(!getChildren().contains(progressbar))
                    getChildren().add(progressbar);
                if(!progressbar.progressProperty().isBound())
                    progressbar.progressProperty().bind(chapterService.progressProperty());
                if(errorAccordion != null)
                    getChildren().remove(errorAccordion);
            }
            else if(_new == State.CANCELLED || _new == State.SUCCEEDED || _new == State.FAILED){
                double completed = getCompletedCount();
                progressbar.progressProperty().unbind();
                progressbar.setProgress(completed/getPagesCount());
                progressbar.getStyleClass().removeAll(removeClassesArray);

                if(_new != State.CANCELLED)
                    progressbar.getStyleClass().add(chapter.getStatus().getClassName());

                if(chapter.isFailed()){
                    progressbar.setProgress(1);
                    progressbar.getStyleClass().add(FAILED.getClassName());                     
                    if(restartButton != null)
                        restartButton.setDisable(false);
                    addErrorPane();
                }
                else if(chapter.isCompleted())
                    setCompleted();
            }
        });
    }
    private void setCompleted() {
        isSelectedCheckBox.setIndeterminate(true);
        isSelectedCheckBox.setDisable(true);
        isSelectedButNotQueued.set(false);
        progressbar.progressProperty().unbind();
        progressbar.getStyleClass().removeAll(removeClassesArray);
        progressbar.getStyleClass().add(COMPLETED.getClassName());
        progressbar.setProgress(1);
    }

    private TableView<Page> errorTable;
    private TitledPane errorTitledPane;
    private Accordion errorAccordion;
    private Button restartButton;
    private Button openDirButton;

    private static final int ICON_SIZE = 30; 
    private static Image REFRESH_ICON = new Image(DownloaderUtils.getImageInputStream("repeat-1.png"), ICON_SIZE, 0, true, true);
    private static Image OPEN_DIR_ICON = new Image(DownloaderUtils.getImageInputStream("folder-11.png"), ICON_SIZE, 0, true, true);
    private static Image COPY_TEXT_ICON = new Image(DownloaderUtils.getImageInputStream("send.png"), ICON_SIZE, 0, true, true);

    private void addErrorPane() {
        if(errorTable == null){
            errorTable = createTable();

            restartButton = new Button(null, new ImageView(REFRESH_ICON));
            restartButton.setTooltip(new Tooltip("Restart Download"));
            restartButton.getStyleClass().add("no-style-button");
            restartButton.setOnAction(e -> restart());

            openDirButton = new Button(null, new ImageView(OPEN_DIR_ICON));
            openDirButton.setTooltip(new Tooltip("Open Chapter Folder"));
            openDirButton.setOnAction(e -> DownloaderApp.showDocument(chapter.getSavePath().toUri().toString()));
            openDirButton.getStyleClass().add("no-style-button");

            Button copyButton = new Button(null, new ImageView(COPY_TEXT_ICON));
            copyButton.setTooltip(new Tooltip("Copy all text"));
            copyButton.setOnAction(e -> {
                copyToClipboard(errorTable.getItems()
                        .stream()
                        .reduce(new StringBuilder(), (sb, p) -> {
                            sb.append(p.getId()).append('\t')
                            .append(p.getOrder()).append('\t')
                            .append(p.getPageUrl()).append('\t')
                            .append(p.getImageUrl()).append('\n');
                            return sb;
                        }, StringBuilder::append).toString()
                        );
            });
            copyButton.getStyleClass().add("no-style-button");

            HBox buttons = new HBox(10, restartButton, openDirButton, copyButton);
            buttons.setPadding(new Insets(0, 0, 10, 0));

            errorTitledPane = new TitledPane("", new BorderPane(errorTable, buttons, null, null, null));
            errorAccordion = new Accordion(errorTitledPane);
        }

        errorTable.getItems().clear();
        if(chapterService.getDirectoryCreateFailedError() == null)
            chapter.stream().filter(Page::hasError).forEach(errorTable.getItems()::add);
        else
            errorTable.setPlaceholder(new Text(chapterService.getDirectoryCreateFailedError()));
        
        StringBuilder sb = new StringBuilder();
        getCounts().forEach((s,t) -> sb.append(String.valueOf(s).charAt(0)).append(": ").append(t).append(" | "));
        
        errorTitledPane.setText(sb.toString());
        openDirButton.setDisable(chapterService.getDirectoryCreateFailedError() != null);

        getChildren().remove(errorAccordion);
        getChildren().add(errorAccordion);
    }
    static void copyToClipboard(String content) {
       FxClipboard.copyToClipboard(content);
        FxPopupShop.showHidePopup("Copied", 1500);
    }

    private TableView<Page> createTable() {
        TableView<Page> tb = new TableView<>();
        tb.getSelectionModel().setCellSelectionEnabled(true);
        tb.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        tb.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            if(e.isControlDown() && e.getCode() == KeyCode.C) {
                @SuppressWarnings("rawtypes")
                List<TablePosition> list = tb.getSelectionModel().getSelectedCells();
                if(list.isEmpty())
                    return;

                StringBuilder sb = new StringBuilder();
                for (@SuppressWarnings("rawtypes") TablePosition tp : list) {
                    Page p = tb.getItems().get(tp.getRow());

                    switch (tp.getColumn()) {
                        case 0:
                            sb.append(p.getId());
                            break;
                        case 1:
                            sb.append(p.getOrder());
                            break;
                        case 2:
                            sb.append(p.getPageUrl());
                            break;
                        case 3:
                            sb.append(p.getImageUrl());
                            break;
                        case 4:
                            sb.append(p.getError());
                            break;
                    }
                    sb.append('\n');
                }

                copyToClipboard(sb.toString());
                FxPopupShop.showHidePopup("copied", 1500);
            }
        });
        SimpleStringProperty sspImageUrl = new SimpleStringProperty("Image url");
        SimpleStringProperty sspPageUrl = new SimpleStringProperty("Page url");

        String[] colNames = {"page_id","order","page url", "Image url", "Error"};
        for (int i = 0; i < colNames.length; i++) {
            TableColumn<Page, String> c = new TableColumn<>(colNames[i]);
            switch (i) {
                case 0:
                    c.setCellValueFactory(cc -> new SimpleStringProperty(String.valueOf(cc.getValue().getId())));
                    break;
                case 1:
                    c.setCellValueFactory(cc -> new SimpleStringProperty(String.valueOf(cc.getValue().getOrder())));
                    break;
                case 2:
                    c.setCellValueFactory(cc -> sspPageUrl);
                    break;
                case 3:
                    c.setCellValueFactory(cc -> sspImageUrl);
                    break;
                case 4:
                    c.setCellValueFactory(cc -> new SimpleStringProperty(cc.getValue().getError()));
                    break;
            }
            c.setEditable(false);
            tb.getColumns().add(c);
        };
        return tb;
    }
    
    public boolean cancel() {
        boolean b = chapterService.cancel();
        if(b && getCompletedCount() != chapter.getPageCount())
            chapter.setStatus(UNTOUCHED);
        
        return b;
    }
    public void restart(){
        if(!chapter.isFailed() || (restartButton != null && restartButton.isDisabled()))
            return;

        chapterService.restart();
        progressbar.getStyleClass().removeAll(removeClassesArray);

        if(restartButton != null)
            restartButton.setDisable(true);
    }
    public boolean isSelected(){
        return !isSelectedCheckBox.isIndeterminate() && isSelectedCheckBox.isSelected();
    }
    public ReadOnlyBooleanProperty isSelectedButNotQueuedProperty() {
        return isSelectedButNotQueued.getReadOnlyProperty();
    }
    String displayName;
    public String getChapterDisplayName() {
        if(displayName != null)
            return displayName;

        displayName = chapter.getChapterName();

        return displayName;
    }
    public void fillErrors(StringBuilder main, StringBuilder id_urls){
        if(chapterService.getDirectoryCreateFailedError() != null){
            main.append(chapterService.getDirectoryCreateFailedError());
            return;
        }

        main
        .append("id: ")
        .append(chapter.getId())
        .append(", number: ")
        .append(chapter.getNumber())
        .append(", title: ")
        .append(chapter.getTitle())
        .append(", volume: ")
        .append(chapter.getVolume())
        .append("\nurl: ")
        .append(chapter.getUrl())
        .append("\nchapter_savePath: ")
        .append(chapter.getSavePath())
        .append("\n\n");

        int length = id_urls.length(); 
        chapter.stream()
        .filter(Page::hasError)
        .forEach(p -> {
            id_urls.append(p.getId()).append('\t')
            .append(p.getOrder()).append('\t')
            .append(p.getPageUrl()).append('\t')
            .append(p.getImageUrl()).append('\t')
            .append(p.getStatus()).append('-').append(p.getError()).append('\n');
        });
        while(length < id_urls.length()) main.append(id_urls.charAt(length++));
    }       
   public  void setSelected(boolean b) {
        if(chapter.isCompleted() && isSelected()){
            setCompleted();
            return;
        }

        if(!isSelected() && (chapter.isRunning() || chapterService.getState() == State.SCHEDULED))
            cancel();

        isSelectedButNotQueued.set(!isSelectedCheckBox.isIndeterminate() && b);
        if(!isSelectedCheckBox.isIndeterminate())
            isSelectedCheckBox.setSelected(b);
    }

   public  ReadOnlyObjectProperty<Worker.State> stateProperty(){
        return chapterService.stateProperty();
    }       
    public State getState() {
        return chapterService.getState();
    }
    public  void commitChapterTitleToDatabase(PreparedStatement ps) throws SQLException {
        ps.setString(1, chapter.getTitle());
        ps.setInt(2, chapter.getId());
        ps.addBatch();
    }

    public void fillSummery(List<Integer> completedChapters, List<Chapter> resetChapters, List<Page> resetPages) throws SQLException {
        if(!dataUpdated.get())
            return;
        if(chapter.isCompleted())
            completedChapters.add(chapter.getId());
        else{
            resetChapters.add(chapter);
            for (Page p : chapter) {
                if(p.isModified())
                resetPages.add(p);
            }
        }
        dataUpdated.set(false);
    }

    public void startDownload() {
        if(chapter.isCompleted()){
            setCompleted();
            return;
        }
        if(isSelectedButNotQueued.get()){
            isSelectedButNotQueued.set(false);
            chapter.setQueued();
            progressbar.getStyleClass().removeAll(removeClassesArray);

            if(chapter.isQueued()){
                if(chapterService.getState() == State.READY)
                    chapterService.start();
                else
                    chapterService.restart();
            }
        }
    }

    public void fillPageSavePaths(Set<Integer> pageIds, Map<Integer, Path> sink) {
        for (Page p : chapter) {
            if(pageIds.contains(p.getId()))
                sink.put(p.getId(), PageDownload.getSavePath(chapter, p));
        }
    }
    //this will set completed to the pages whose ID is in successIds 
    public void updateIfHasPages(HashSet<Integer> successIds) {
        boolean update = false;
        for (Page p : chapter){ 
            if(successIds.contains(p.getId())){
                p.setCompleted();
                update = true;
            }
        }

        if(update)
            chapterService.restart();
    }
    
    public static class  DownloadableChapterCheckBoxListCell extends ListCell<ChapterPresenter> {

        @Override
        protected void updateItem(ChapterPresenter item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);

            if(empty)
                setGraphic(null);
            else 
                setGraphic(item.isSelectedCheckBox);
        }
    }
}



