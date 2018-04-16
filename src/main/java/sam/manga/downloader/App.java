package sam.manga.downloader;

import static sam.fx.alert.FxAlert.showConfirmDialog;
import static sam.fx.popup.FxPopupShop.showHidePopup;
import static sam.manga.downloader.extra.Utils.stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.sqlite.JDBC;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import sam.fx.alert.FxAlert;
import sam.fx.clipboard.FxClipboard;
import sam.fx.popup.FxPopupShop;
import sam.manga.downloader.chapter.ChapterPresenter;
import sam.manga.downloader.chapter.ChapterPresenter.DownloadableChapterCheckBoxListCell;
import sam.manga.downloader.data.CreateMangarockDatabase;
import sam.manga.downloader.data.DataManager;
import sam.manga.downloader.data.FillNotFoundImages;
import sam.manga.downloader.data.MoveChapterFolders;
import sam.manga.downloader.extra.SortingMethod;
import sam.manga.downloader.extra.Status;
import sam.manga.downloader.extra.Utils;
import sam.manga.downloader.manga.Manga;
import sam.manga.downloader.manga.MangaEvent;
import sam.manga.downloader.manga.MangaPresenter;

public class App extends Application {
    static final double VERSION = 8.31;

    public static void main(String[] args) throws ClassNotFoundException {
        if(args.length == 1 && Objects.equals(args[0], "version")){
            System.out.println(VERSION);
            System.exit(0);
        }
        Class.forName(JDBC.class.getName());
        launch(args);
    }

    private final CheckMenuItem exitAfterCompleteCMI = new CheckMenuItem("Exit After complete");
    private final CheckMenuItem shutdownAfterCompleteCMI = new CheckMenuItem("Shutdown After complete");

    private final ReadOnlyObjectWrapper<MangaPresenter>  currentManga = new ReadOnlyObjectWrapper<>(this, "current_manga", null);

    private final SimpleBooleanProperty dataUpdated = new SimpleBooleanProperty(this, "database_update", false);

    private final ReadOnlyIntegerWrapper mangasCount = new ReadOnlyIntegerWrapper (this, "mangas_count", 0);
    private final ReadOnlyIntegerWrapper chapterCount = new ReadOnlyIntegerWrapper (this, "chapter_count", 0);
    private final ReadOnlyIntegerWrapper pagesCount = new ReadOnlyIntegerWrapper (this, "pages_count", 0);

    private final ReadOnlyIntegerWrapper selectedCount = new ReadOnlyIntegerWrapper (this, "selected_count", 0);
    private final ReadOnlyIntegerWrapper queuedCount = new ReadOnlyIntegerWrapper (this, "queued_count", 0);
    private final ReadOnlyIntegerWrapper completedCount = new ReadOnlyIntegerWrapper (this, "completed_count", 0);
    private final ReadOnlyIntegerWrapper failedCount = new ReadOnlyIntegerWrapper (this, "failed_count", 0);
    private final ReadOnlyIntegerWrapper remainingCount = new ReadOnlyIntegerWrapper (this, "failed_count", 0);

    private final List<MangaPresenter> mangasList = new ArrayList<>();
    private final SimpleBooleanProperty mangasListModifiedWatcher = new SimpleBooleanProperty();

    private final CheckBox selectUnselectCB = new CheckBox("Select All");
    private SimpleBooleanProperty downloadIsActive = new SimpleBooleanProperty(this, "download_is_active", false);

    @Override
    public void start(Stage stage) throws Exception {
        FxAlert.setParent(stage);
        FxPopupShop.setParent(stage);
        Utils.mustNoError(() -> DataManager.init());

        stage.setTitle("Downloader: "+VERSION);
        stage.getIcons().add(new Image(ClassLoader.getSystemResource("icon.png").toExternalForm()));

        ListView<ChapterPresenter> chaptersListView = getChapterListView();

        addDownloadCompleteNotification();

        selectUnselectCB.textProperty().bind(Bindings.when(selectUnselectCB.selectedProperty()).then("Unselect All").otherwise("Select All"));
        selectUnselectCB.visibleProperty().bind(currentManga.isNotNull());
        selectUnselectCB.setPadding(new Insets(5));
        //why using setOnAction rather then Binding? because setOnAction will not fire if setSelected is called programeitically
        //this is necessary when when currentManga changes
        selectUnselectCB.setOnAction(e -> currentManga.get().setAllChapterSelected(selectUnselectCB.isSelected()));

        BorderPane listPane = new BorderPane(chaptersListView);
        listPane.setTop(selectUnselectCB);
        Tab selectionTab = new Tab("Selection view", listPane);
        Tab downloadingViewTab = new Tab("Download view");

        addCurrentMangaListener(downloadingViewTab, chaptersListView, selectUnselectCB);

        final TabPane tabPane = new TabPane(selectionTab, downloadingViewTab);
        tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);

        VBox westControlPane = new VBox(10);
        westControlPane.setId("west-panel");

        westControlPane.getChildren().add(new CountersViewBox(mangasCount, chapterCount, pagesCount, selectedCount, queuedCount, completedCount, failedCount, remainingCount));

        Node mangasNamesNode = getMangaNamesBox(westControlPane.widthProperty()); 
        Button downloadButton = getDownloadButton(tabPane.getSelectionModel());
        VBox.setMargin(downloadButton, new Insets(5, 2, 10, 2));

        VBox.setVgrow(mangasNamesNode, Priority.ALWAYS);
        westControlPane.getChildren().addAll(mangasNamesNode, downloadButton);

        BorderPane mainContentPane = new BorderPane(tabPane);
        mainContentPane.setTop(getMangaDetailsBox());

        Label text = new Label();
        text.textProperty().bind(Bindings.concat("  |   database requires commit? : ", dataUpdated));
        mainContentPane.setBottom(text);
        text.setMaxWidth(Double.MAX_VALUE);
        text.setId("status");

        BorderPane root = new BorderPane(mainContentPane);
        root.setLeft(westControlPane);

        MenuBar menuBar = getMenuBar();
        root.setTop(new BorderPane(new ToolBar2(mangasList, currentManga.getReadOnlyProperty(), failedCount.getReadOnlyProperty()), new HBox(10, menuBar, BytesCounter.COUNTER_VIEW), null, null, null));
        HBox.setHgrow(menuBar, Priority.ALWAYS);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(ClassLoader.getSystemResource("stylesheet/main_stylesheet.css").toExternalForm());

        stage.setScene(scene);
        stage.setOnCloseRequest(closeRequestAction);

        stage.setMaximized(true);
        stage.show();

        Utils.init(stage, getHostServices());
    }

    EventHandler<WindowEvent> closeRequestAction = e -> {
        if(downloadIsActive.get()){
            if(showConfirmDialog(Utils.stage(), "This will stop all current download and exit", "R U SURE ?")){
                if(shutdownAfterCompleteCMI.isSelected())
                    shutdownAfterCompleteCMI.setSelected(false);
            }
            else
                e.consume();
        }
    };

    @Override
    public void stop() throws Exception {
        DataManager.stop();
        mangaForEach(MangaPresenter::saveRecords);

        if(downloadIsActive.get())
            mangaForEach(MangaPresenter::cancelAllDownload);
        
        Utils.stop();
        System.exit(0);
    }
    private void addDownloadCompleteNotification() {
        remainingCount.addListener((prop, o, n) -> {
            if((int)n == 0 && downloadIsActive.get()){
                AudioClip clip = new AudioClip(ClassLoader.getSystemResource("sound/ALARM.WAV").toExternalForm());
                clip.setCycleCount(2);
                clip.play(1, 0, 1, 1, 0);
                downloadIsActive.set(false);

                if(shutdownAfterCompleteCMI.isSelected()){
                    stage().close();
                    if(shutdownAfterCompleteCMI.isSelected()){
                        try {
                            Runtime.getRuntime().exec("shutdown.exe -s -f -t 15");
                        } catch (Exception e) {}
                    }
                }
                else if(exitAfterCompleteCMI.isSelected())
                    stage().close();
            }
        });
    }

    private void addCurrentMangaListener(Tab downloadingViewTab, ListView<ChapterPresenter> chaptersListView, CheckBox selectUnselectCB) {
        currentManga.addListener((prop, old, _new) -> {
            if(_new == null){
                downloadingViewTab.setContent(null);
                chaptersListView.getItems().clear();
                return;
            }

            downloadingViewTab.setContent(_new.getDownloadingView());
            _new.fillChapterPresentersView(chaptersListView.getItems());
        });
    }

    private Node getMangaNamesBox(ReadOnlyDoubleProperty maxWidthProperty) {
        VBox root = new VBox(1);

        root.setMaxHeight(Double.MAX_VALUE);

        ScrollPane pane = new ScrollPane(root);
        pane.setFitToWidth(true);
        pane.setMaxHeight(Double.MAX_VALUE);

        InvalidationListener l1 = new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if(stage().isShowing()){
                    pane.setMaxWidth(maxWidthProperty.get());
                    maxWidthProperty.removeListener(this);
                }
            }
        };
        maxWidthProperty.addListener(l1);

        Runnable populateMangas = () -> root.getChildren().setAll(mangasStream().map(MangaPresenter::getControlBox).toArray(Node[]::new)); 

        ChoiceBox<SortingMethod> sortingChoices = new ChoiceBox<>();
        sortingChoices.getItems().addAll(SortingMethod .values());
        sortingChoices.getSelectionModel().select(SortingMethod.NAME);
        sortingChoices.setDisable(true);
        sortingChoices.setOnAction(e -> {
            Collections.sort(mangasList, sortingChoices.getValue().comparator);
            populateMangas.run();
        });

        mangasListModifiedWatcher.addListener(e -> {
            if(mangasList.size() > 1){
                sortingChoices.setDisable(false);
                Collections.sort(mangasList, sortingChoices.getValue().comparator);
                populateMangas.run();
            }
            else{
                sortingChoices.setDisable(true);
                populateMangas.run();
            }
            updateBindCountProperties();
        });

        Label label = new Label("sort by: ");
        label.setTextFill(Color.WHITE);
        return new BorderPane(pane, new HBox(5, label, sortingChoices), null, null, null);
    }

    private void updateBindCountProperties(){
        // {chapterCount,  pagesCount}
        int count[] = new int[2];
        mangaForEach(d -> {
            count[0] += d.getChaptersCount();
            count[1] += d.getPagesCount();
        }); 

        mangasCount.set(mangasList.size());		
        chapterCount.set(count[0]);
        pagesCount.set(count[1]);

        dataUpdated.unbind();
        selectedCount.unbind();
        queuedCount.unbind();
        completedCount.unbind();
        failedCount.unbind();
        remainingCount.unbind();

        final ReadOnlyBooleanProperty[] array = mangasStream().map(MangaPresenter::dataUpdatedProperty).toArray(ReadOnlyBooleanProperty[]::new);
        dataUpdated.bind(Bindings.createBooleanBinding(() -> Stream.of(array).anyMatch(ReadOnlyBooleanProperty::get), array));

        bindCounter(MangaPresenter::selectedCountProperty, selectedCount);
        bindCounter(MangaPresenter::queuedCountProperty, queuedCount);
        bindCounter(MangaPresenter::completedCountProperty, completedCount);
        bindCounter(MangaPresenter::failedCountProperty, failedCount);
        bindCounter(MangaPresenter::remainingCountProperty, remainingCount);
    }

    private void bindCounter(Function<? super MangaPresenter, ? extends ReadOnlyIntegerProperty> mapper, ReadOnlyIntegerWrapper bindTo){
        ReadOnlyIntegerProperty[] array = mangasStream().map(mapper).toArray(ReadOnlyIntegerProperty[]::new); 
        NumberBinding binding = Bindings.createIntegerBinding(() -> Stream.of(array).mapToInt(ReadOnlyIntegerProperty::get).sum(), array);
        bindTo.bind(binding);
    }

    private Button getDownloadButton(SingleSelectionModel<Tab> tabeSelectionModel) {
        Button downloadButton = new Button("Start Download");
        downloadButton.setId("download-button");
        downloadButton.setMaxWidth(Double.MAX_VALUE);
        downloadButton.setAlignment(Pos.BOTTOM_CENTER);
        downloadButton.setDisable(true);

        mangasListModifiedWatcher.addListener(e -> {
            downloadButton.disableProperty().unbind();
            if(mangasList.isEmpty())
                downloadButton.setDisable(true);

            ReadOnlyBooleanProperty[] array = mangasStream().map(MangaPresenter::isSelectedButNotQueuedProperty).toArray(ReadOnlyBooleanProperty[]::new);
            downloadButton.disableProperty().bind(Bindings.createBooleanBinding(() -> !Stream.of(array).anyMatch(ReadOnlyBooleanProperty::get), array)); 
        });

        downloadButton.setOnAction(e -> {
            downloadIsActive.set( true);
            mangaForEach(MangaPresenter::startDownload);

            Platform.runLater(() -> {
                if(currentManga.get() != null && currentManga.get().isDownloading())
                    tabeSelectionModel.select(1);
            });
        });

        return downloadButton;
    }

    private HBox getMangaDetailsBox() {
        Text text = new Text();
        HBox.setHgrow(text, Priority.ALWAYS);

        text.getStyleClass().addAll("manga-details", "text-label");
        Tooltip.install(text, new Tooltip("manga_id | chapter count | manga_name\ndouble click to copy"));
        text.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if(e.getClickCount() >  1){
                MangaPresenter mp = currentManga.get();
                Manga m = mp.getManga(); 
                if(m != null){
                    FxClipboard.copyToClipboard(m.getId()+"\n"+m.chaptersCount()+"\n"+m.getMangaName()+"\n"+m.getUrl());
                    showHidePopup("copied to clipboard", 1000);
                }
                e.consume();
            }
        });
        Button openFolder = new Button("Open Folder");
        openFolder.setAlignment(Pos.CENTER_RIGHT);
        openFolder.setOnAction(e -> getHostServices().showDocument(getCurrentManga().getMangaPath()));

        Button openUrl = new Button("brows MangaFox");
        openUrl.setAlignment(Pos.CENTER_RIGHT);
        openUrl.setOnAction(e -> getHostServices().showDocument(getCurrentManga().getManga().getUrl()));

        HBox box = new HBox(10, text, openFolder, openUrl);
        box.getStyleClass().addAll("manga-details", "v-box");

        text.wrappingWidthProperty().bind(box.widthProperty().multiply(3).divide(4));

        currentManga.addListener((prop, old, _new) -> {
            Manga manga = _new.getManga();
            if(manga == null)
                text.setText(null);
            else
                text.setText(manga.getId()+" || "+manga.chaptersCount()+" || "+manga.getMangaName());
        });

        openFolder.disableProperty().bind(currentManga.isNull());
        openUrl.disableProperty().bind(currentManga.isNull());

        return box;
    }

    private MangaPresenter getCurrentManga() {
        return currentManga.get();
    }

    private ListView<ChapterPresenter> getChapterListView() {
        ListView<ChapterPresenter> chaptersListView = new ListView<>();
        chaptersListView.setCellFactory (callback -> new DownloadableChapterCheckBoxListCell());
        return chaptersListView;
    }

    final MangaEvent onMangaClick = manga -> {
        currentManga.set(manga);
        selectUnselectCB.setSelected(manga.isAllChaptersSelected());
    };

    private MenuBar getMenuBar(){
        MenuItem exitMI = new MenuItem("Exit");
        exitMI.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));
        exitMI.setOnAction(e -> stage().close());

        Menu fileMenu = new Menu("File", null, 
                new SeparatorMenuItem(), 
                exitAfterCompleteCMI, 
                shutdownAfterCompleteCMI, 
                new SeparatorMenuItem(), 
                exitMI);

        MenuItem help = new MenuItem("Help");
        help.setOnAction(e -> openHelpDialog());
        Menu appMenu = new Menu("App", null, help);

        return new MenuBar(fileMenu, appMenu, getToolsMenu());
    } 

    private Menu getToolsMenu() {
        MenuItem createMangarockDatabaseMi = new MenuItem("Create Mangarock Database");
        createMangarockDatabaseMi.setOnAction(e -> createMangarock());

        MenuItem clearCompletedDownloadsMi = new MenuItem("Clear Completed Downloads");
        clearCompletedDownloadsMi.setOnAction(e -> mangaForEach(MangaPresenter::removeCompletedChaptersFromView));

        MenuItem moveInompletePagesMi = new MenuItem("Move Incomplete Pages");
        moveInompletePagesMi.setOnAction(e -> new MoveIncompletePages(mangasList));

        MenuItem fillNotFoundImagesMi = new MenuItem("Fill Not Found Images");
        fillNotFoundImagesMi.setOnAction(e -> new FillNotFoundImages());

        MenuItem moveChapterFoldersMi = new MenuItem("Move Chapter Folders");
        moveChapterFoldersMi.setOnAction(e -> new MoveChapterFolders());

        MenuItem createNmoveMi = new MenuItem("Create and Move");
        createNmoveMi.setOnAction(e -> {
            if(createMangarock())
                new MoveChapterFolders();
        });

        Menu toolsMenu = new Menu("Tools", null, clearCompletedDownloadsMi, createNmoveMi,
                moveInompletePagesMi,
                createMangarockDatabaseMi,
                moveChapterFoldersMi);

        toolsMenu.disableProperty().bind(downloadIsActive);

        return toolsMenu;
    }

    private boolean createMangarock() {
        try {
            DataManager.getInstance().commitDatabase();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | IOException
                | SQLException e) {
            Utils.showError("failed to create commit", "Error", e, false);
            return false;
        }
        return new CreateMangarockDatabase().result(); 
    }
    private void mangaForEach(Consumer<MangaPresenter> action) {
        mangasList.forEach(action);
    }

    //css is same as counterViewBox
    private VBox getHelpBox(){
        VBox helpBox = new VBox(5);
        Label vlabel = new Label("version: "+VERSION);
        vlabel.setId("version-label");

        vlabel.getStyleClass().add("info-panel-header");
        vlabel.setEffect(new Glow());

        Label l2 = new Label("Borders Colors Means...");
        l2.getStyleClass().add("info-panel-header");
        l2.setEffect(new Glow());

        helpBox.getChildren().addAll(vlabel, new Label(" "), l2);

        Consumer<Status> add = d -> {
            Label l = new Label(d.getClassName());
            l.getStyleClass().add(d.getClassName());
            l.setPadding(new Insets(5));
            l.getStyleClass().add("info-panel-box");
            helpBox.getChildren().add(l);
        };

        add.accept(Status.QUEUED);
        add.accept(Status.RUNNING);
        add.accept(Status.COMPLETED);
        add.accept(Status.FAILED);

        Text termsText1 = new Text("\nTerms...");
        termsText1.getStyleClass().add("info-panel-header");

        Text termsText = new Text(
                " C : Completed Count\n"+
                        " F : Failed Count\n"+
                        " R : Remaining in Queue\n"+
                        " Q : Queued Count\n"+
                " T : Total Chapters\n");


        termsText.getStyleClass().add("info-panel-box");

        helpBox.getChildren().addAll(termsText1, termsText);

        helpBox.setPadding(new Insets(10));
        helpBox.setId("help-box");

        return helpBox;
    }	

    void openHelpDialog(){
        Platform.runLater(() -> {
            Stage stage = new Stage(StageStyle.UTILITY);
            stage.initOwner(stage());
            stage.initModality(Modality.APPLICATION_MODAL);

            Scene scene = new Scene(getHelpBox());
            scene.getStylesheets().addAll(ClassLoader.getSystemResource("stylesheet/main_stylesheet.css").toExternalForm());
            stage.setScene(scene);

            stage.getScene().setOnKeyPressed(e -> {
                if(e.getCode() == KeyCode.ESCAPE)
                    stage.close();
            });
            stage.show();
        });
    }

    /**
     * @return Stream.of(mangas);
     */
    private Stream<MangaPresenter> mangasStream(){
        return mangasList.stream();
    }
}
