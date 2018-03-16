package sam.manga.downloader;

import static sam.fx.alert.FxAlert.showConfirmDialog;
import static sam.fx.alert.FxAlert.showErrorDialog;
import static sam.fx.alert.FxAlert.showMessageDialog;
import static sam.fx.popup.FxPopupShop.showHidePopup;

import java.awt.Dialog.ModalityType;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.sqlite.JDBC;

import javafx.application.Application;
import javafx.application.HostServices;
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
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import sam.console.ansi.ANSI;
import sam.fx.alert.FxAlert;
import sam.fx.clipboard.FxClipboard;
import sam.fx.popup.FxPopupShop;
import sam.manga.downloader.db.DbManager;
import sam.manga.downloader.extra.Status;
import sam.manga.downloader.parts.Manga;
import sam.manga.downloader.view.chapter.ChapterPresenter;
import sam.manga.downloader.view.chapter.ChapterPresenter.DownloadableChapterCheckBoxListCell;
import sam.manga.downloader.view.manga.MangaEvent;
import sam.manga.downloader.view.manga.MangaPresenter;
import sam.manga.newsamrock.mangas.MangasMeta;
import sam.myutils.fileutils.FilesUtils;
import sam.myutils.fileutils.FilesUtils.FileWalkResult;
import sam.myutils.myutils.MyUtils;
import sam.properties.myconfig.MyConfig;
import sam.properties.myproperties.MyProperties;
import sam.tsv.Row;
import sam.tsv.Tsv;

public class DownloaderApp extends Application {
    static final double VERSION = 8.31;
    static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(6);

    static final MyProperties CONFIG_MAP = new MyProperties();
    static final Path CURRENT_SESSION_FOLDER;
    static final Path HALTED_IMAGE_DIR;
    public static final Path DOWNLOAD_DIR;
    public static final Path LOGS_FOLDER;
    static final Path BACKUP_DIR;
    static final Path MISSING_CHAPTERS_PATH = Paths.get(MyConfig.MISSING_CHAPTERS_PATH);
    static final Path NEW_MANGAS_TSV_PATH = Paths.get(MyConfig.NEW_MANGAS_TSV_PATH);
    static final Path MANGAROCK_INPUT_DB = Paths.get(MyConfig.MANGAROCK_INPUT_DB);
    static final Path MANGAROCK_INPUT_FOLDER = Paths.get(MyConfig.MANGAROCK_INPUT_FOLDER);

    static {
        Path p = Paths.get("running");

        if(Files.exists(p)){
            FilesUtils.openFileLocationInExplorerNoError(new File("running"));
            JOptionPane.showMessageDialog(null, "Only One instance of downloader is allowed");
            System.exit(0);
        }
        else{
            try {
                Files.createFile(p);
                p.toFile().deleteOnExit();
            } catch (IOException e1) {
                showErrorJDialog("Failed to create file: running", null, e1);
                System.exit(0);
            }
        }

        try {
            CONFIG_MAP.load(Paths.get("config.properties"));
        } catch (IOException e1) {}

        CURRENT_SESSION_FOLDER = Paths.get(CONFIG_MAP.containsKey("session_1") ? CONFIG_MAP.get("session_1") : "current_session");
        HALTED_IMAGE_DIR = CURRENT_SESSION_FOLDER.resolve("mangafox_halted");
        DOWNLOAD_DIR = CURRENT_SESSION_FOLDER.resolve("mangafox_downloaded");
        LOGS_FOLDER = CURRENT_SESSION_FOLDER.resolve("logs");
        BACKUP_DIR = CURRENT_SESSION_FOLDER.resolve("backups");

        if(Files.notExists(CURRENT_SESSION_FOLDER))
            CONFIG_MAP.clear();

        if(!CONFIG_MAP.containsKey("session_1"))
            CONFIG_MAP.put("session_1","current_session");

        try {
            Files.createDirectories(HALTED_IMAGE_DIR);
            Files.createDirectories(DOWNLOAD_DIR);
        } catch (IOException e1) {
            showErrorJDialog("Error creating dir: \n\tHALTED_IMAGE_DIR: "+HALTED_IMAGE_DIR+"\n\tDOWNLOAD_DIR: "+DOWNLOAD_DIR, "App will not start", e1);			
            System.exit(0);
        }
    }

    public static void main(String[] args) throws ClassNotFoundException {
        if(args.length == 1 && Objects.equals(args[0], "version")){
            System.out.println(VERSION);
            System.exit(0);
        }
        Class.forName(JDBC.class.getName());
        launch(args);
    }

    private static void showErrorJDialog(String msg, String title, Exception e1) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println(msg);
        pw.println();
        e1.printStackTrace(pw);

        JTextArea area = new JTextArea(sw.toString());
        area.setEditable(false);

        JDialog dialog = new JDialog(null, title, ModalityType.APPLICATION_MODAL);
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dialog.add(area, new JScrollPane(area));
        dialog.pack();

        dialog.setVisible(true);
    }

    private static Stage stage;
    private static HostServices hostServices;

    public static HostServices getHostService() {
        return hostServices;
    }
    static Stage getPrimaryStage(){
        return stage;
    }

    public static void showDocument(String uri){
        hostServices.showDocument(uri);
    }

    private final ReadOnlyObjectWrapper<DbManager> workingdb = new ReadOnlyObjectWrapper<>(this, "working_db", null);

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
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        hostServices = getHostServices();
        FxAlert.setParent(primaryStage);
        FxPopupShop.setParent(primaryStage);

        primaryStage.setTitle("Downloader: "+VERSION);
        primaryStage.getIcons().add(new Image(DownloaderUtils.getImageInputStream("icon.png")));

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
        text.textProperty().bind(Bindings.concat("database file: ", workingdb, "  |   database requires commit? : ", dataUpdated));
        mainContentPane.setBottom(text);
        text.setStyle("-fx-padding:2;-fx-font:10 Consolas;-fx-background-color:black;-fx-text-fill:white;");
        text.setMaxWidth(Double.MAX_VALUE);

        BorderPane root = new BorderPane(mainContentPane);
        root.setLeft(westControlPane);

        MenuBar menuBar = getMenuBar();
        root.setTop(new BorderPane(getToolBar(), new HBox(10, menuBar, BytesCounter.COUNTER_VIEW), null, null, null));
        HBox.setHgrow(menuBar, Priority.ALWAYS);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(ClassLoader.getSystemResource("stylesheet/main_stylesheet.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(closeRequestAction);

        primaryStage.setMaximized(true);
        primaryStage.show();
        checkResume();
    }

    EventHandler<WindowEvent> closeRequestAction = e -> {
        if(workingdb.get() == null)
            System.exit(0);

        if(downloadIsActive.get()){
            if(showConfirmDialog(stage, "This will stop all current download and exit", "R U SURE ?")){
                if(shutdownAfterCompleteCMI.isSelected())
                    shutdownAfterCompleteCMI.setSelected(false);
            }
            else
                e.consume();
        }
    };

    @Override
    public void stop() throws Exception {
        if(workingdb.get() == null)
            CONFIG_MAP.remove("resume_db");
        else
            CONFIG_MAP.put("resume_db", workingdb.get().toString().replace('\\', '/'));

        commitDatabase();
        mangasStream().forEach(MangaPresenter::saveRecords);

        if(downloadIsActive.get()){
            mangasStream().forEach(MangaPresenter::cancelAllDownload);
            EXECUTOR_SERVICE.shutdownNow();
        }

        try {
            CONFIG_MAP.storeIfModified(Paths.get("config.properties"));
            Files.deleteIfExists(BACKUP_DIR);
            Files.deleteIfExists(LOGS_FOLDER);
            Files.deleteIfExists(DOWNLOAD_DIR);
            Files.deleteIfExists(HALTED_IMAGE_DIR);
            Files.deleteIfExists(CURRENT_SESSION_FOLDER);
        } catch (IOException e1) {}

        System.exit(0);
    }

    private void checkResume() {
        String dbPath = CONFIG_MAP.get("resume_db");

        File dbFile = null;

        if(dbPath != null && (dbFile = new File(dbPath)).exists())
            readDatabase(dbFile);
        else {
            dbFile = null;
            if(Files.exists(CURRENT_SESSION_FOLDER)){
                File[] files = CURRENT_SESSION_FOLDER.toFile().listFiles((file, name) -> file.isFile() &&  name.endsWith("db"));

                if(files.length != 0){
                    if(files.length == 1){
                        if(showConfirmDialog(stage, "only one .db file: "+files[0]+"\n was found\ndo you width to load this? ", "No or Cancel will close the app"))
                            dbFile = files[0];
                        else
                            System.exit(0);
                    }
                    else{
                        showDocument(CURRENT_SESSION_FOLDER.toUri().toString());
                        showMessageDialog(stage, AlertType.ERROR, "more than one db found in\n"+CURRENT_SESSION_FOLDER, "App will not start", true);
                        System.exit(0);
                    }
                }
                else if(Files.exists(DOWNLOAD_DIR)){
                    try {
                        FileWalkResult map = FilesUtils.listDirsFiles(DOWNLOAD_DIR);
                        if(map.files != null && !map.files.isEmpty()){
                            TreeMap<Path, TreeMap<Path, TreeSet<Path>>> map2 = new TreeMap<>();

                            for (Path f : map.files) {
                                final Path f1 = f.getFileName();
                                final Path f2 = f.getParent().getFileName();
                                final Path f3 = f.getParent().getParent().getFileName();

                                map2.putIfAbsent(f3, new TreeMap<>());
                                map2.get(f3).putIfAbsent(f2, new TreeSet<>());
                                map2.get(f3).get(f2).add(f1);
                            }

                            StringBuilder builder = new StringBuilder();
                            map2.forEach((f3, map3) -> {
                                builder.append(f3).append('\n');

                                map3.forEach((f2, f1List) -> {
                                    builder.append("  ").append(f2).append('\n');
                                    f1List.forEach(f1 -> builder.append("     ").append(f1).append('\n'));
                                    builder.append('\n');
                                });
                                builder.append('\n');
                            });

                            Alert alert = new Alert(AlertType.WARNING, "no .db file was found but DOWNLOAD_DIR has Data\nDOWNLOAD_DIR: "+DOWNLOAD_DIR+"\ndo you wish delete these files and proceed? ", ButtonType.YES, ButtonType.NO);
                            alert.initModality(Modality.APPLICATION_MODAL);
                            alert.setHeaderText("App will not start on NO or Cancel");
                            alert.initOwner(stage);
                            alert.getDialogPane().setExpandableContent(new TextArea(builder.toString()));

                            Optional<ButtonType> result = alert.showAndWait();

                            if(!result.isPresent() || result.get() == ButtonType.NO)
                                System.exit(0);

                            map.files.forEach(p -> p.toFile().delete());
                            if(map.dirs != null)
                                map.dirs.stream().sorted(Comparator.comparing(Path::getNameCount).reversed()).forEach(p -> p.toFile().delete());
                        }
                        else if(map.dirs != null)
                            map.dirs.stream().sorted(Comparator.comparing(Path::getNameCount).reversed()).forEach(p -> p.toFile().delete());
                    } catch (IOException e) {
                        showErrorDialog("Error while checking for resume", "App will not start", e);
                    }
                }
            }
        }
        if(dbFile != null)
            readDatabase(dbFile);
    }

    private void addDownloadCompleteNotification() {
        remainingCount.addListener((prop, o, n) -> {
            if((int)n == 0 && downloadIsActive.get()){
                AudioClip clip = new AudioClip(ClassLoader.getSystemResource("sound/ALARM.WAV").toExternalForm());
                clip.setCycleCount(2);
                clip.play(1, 0, 1, 1, 0);
                downloadIsActive.set(false);

                if(shutdownAfterCompleteCMI.isSelected()){
                    stage.close();
                    if(shutdownAfterCompleteCMI.isSelected()){
                        try {
                            Runtime.getRuntime().exec("shutdown.exe -s -f -t 15");
                        } catch (Exception e) {}
                    }
                }
                else if(exitAfterCompleteCMI.isSelected())
                    stage.close();
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

    private enum SortingMethod {
        NAME(Comparator.comparing(m -> m.getManga().getName())),
        CHAPTER_COUNT(Comparator.comparing(m -> m.getManga().chaptersCount()));

        final Comparator<MangaPresenter> comparator;
        SortingMethod(Comparator<MangaPresenter> comparator){
            this.comparator = comparator;
        }
    };

    private Node getMangaNamesBox(ReadOnlyDoubleProperty maxWidthProperty) {
        VBox root = new VBox(1);

        root.setMaxHeight(Double.MAX_VALUE);

        ScrollPane pane = new ScrollPane(root);
        pane.setFitToWidth(true);
        pane.setMaxHeight(Double.MAX_VALUE);

        InvalidationListener l1 = new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if(stage.isShowing()){
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
        mangasStream().forEach(d -> {
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
            mangasStream().forEach(MangaPresenter::startDownload);

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
                    FxClipboard.copyToClipboard(m.getId()+"\n"+m.chaptersCount()+"\n"+m.getName()+"\n"+m.getUrl());
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
                text.setText(manga.getId()+" || "+manga.chaptersCount()+" || "+manga.getName());
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

    final EventHandler<ActionEvent> loadMissingChaptersAction = e -> {
        boolean shown[] = {false};

        Object[] logger = DownloaderUtils.getLoggerStage(false);
        Stage s1 = (Stage)logger[0];
        s1.setOnCloseRequest(e1 -> e1.consume());
        s1.setTitle("Select Missing Chapters");
        TextArea logText = (TextArea)logger[1];

        if(Files.exists(NEW_MANGAS_TSV_PATH)){
            try {
                Tsv newMangsTsv = Tsv.parse(NEW_MANGAS_TSV_PATH);
                HashSet<Integer> newIds = new HashSet<>();
                int idCol = newMangsTsv.columns().indexOf(MangasMeta.MANGA_ID);
                for (Row row : newMangsTsv) newIds.add(Integer.parseInt(row.get(idCol)));

                if(!newIds.isEmpty()){
                    if(!shown[0])
                        logText.appendText(ANSI.createUnColoredBanner("Success")+"\n\n");

                    shown[0] = true;
                    mangasStream()
                    .filter(m -> newIds.contains(m.getManga().getId()))
                    .peek(m -> logText.appendText("new Manga: "+m.getManga().getId()+"\t"+m.getManga().getName()+"\n"))
                    .forEach(m -> m.setAllChapterSelected(true));

                    logText.appendText("\n\n");
                }
                else 
                    logText.appendText("\nno new manga_id(s) found\n\n");

            } catch (IOException|NullPointerException e1) {
                showErrorDialog("Failed to load new mangas", "", e1, false);
            }
        }
        else{
            logText.appendText(NEW_MANGAS_TSV_PATH.getFileName()+" not found: "+NEW_MANGAS_TSV_PATH+"\n\n");
            showHidePopup(NEW_MANGAS_TSV_PATH.getFileName()+",  not found", 1500);
        }

        if(Files.notExists(MISSING_CHAPTERS_PATH)){
            logText.appendText(MISSING_CHAPTERS_PATH.getFileName()+" not found: "+MISSING_CHAPTERS_PATH+"\n\n");
            showHidePopup(MISSING_CHAPTERS_PATH.getFileName()+",  not found", 1500);
            s1.setOnCloseRequest(null);
            return;
        }

        HashMap<String, TreeMap<Double, String>> missingChapters;

        try {
            missingChapters = FilesUtils.readObjectFromFile(MISSING_CHAPTERS_PATH);
        } catch (ClassNotFoundException | IOException e1) {
            showErrorDialog("Failed to load missing chapters", "Error", e1, false);
            s1.setOnCloseRequest(null);
            return;
        }

        if(missingChapters == null || missingChapters.isEmpty()){
            logText.appendText("No Missing Chapters Found\n");
            s1.setOnCloseRequest(null);
            return;
        }

        Map<Integer, MangaPresenter> mangasIdMangaMap = mangasStream().collect(Collectors.toMap(m -> m.getManga().getId(), Function.identity()));
        StringBuilder notInSession = new StringBuilder("\n\n");

        StringBuilder stillMissingMangaSummery = new StringBuilder()
                .append('\n')
                .append(ANSI.createBanner("Failed to select Chapters"))
                .append('\n');
        stillMissingMangaSummery.append("---- Manga Summery ----").append('\n');
        StringBuilder stillMissingChapters = new StringBuilder();

        if(!shown[0])
            logText.appendText(ANSI.createUnColoredBanner("Success")+"\n\n");

        shown[0] = true;

        missingChapters.forEach((mangaId, chapters) -> {
            MangaPresenter manga = mangasIdMangaMap.get(Integer.parseInt(mangaId));

            if(manga == null)
                notInSession.append(mangaId).append("\n");
            else{
                manga.selectMissing(chapters, logText);
                if(!chapters.isEmpty()){
                    int l = stillMissingMangaSummery.length();
                    stillMissingMangaSummery.append(mangaId).append('\t').append(manga.getManga().getName()).append('\t');
                    stillMissingMangaSummery.append(String.valueOf(chapters.size())).append('\n');

                    stillMissingChapters.append(stillMissingMangaSummery.subSequence(l, stillMissingMangaSummery.length()));

                    chapters.forEach((number, chapterName) -> 
                    stillMissingChapters.append('\t')
                    .append(number).append("    ")
                    .append(chapterName).append('\n'));
                    stillMissingChapters.append('\n');
                }
            }
        });

        if(notInSession.length() != 2){
            logText.appendText(ANSI.createUnColoredBanner("Manga(s) Not in Session")+"\n\n");
            logText.appendText(notInSession.append("\n\n").toString());
            showHidePopup("InComplete Missing Selections", 1500);
        }
        if(stillMissingChapters.length() != 0){
            stillMissingMangaSummery.append('\n').append("---- Chapters Summery ----\n");
            logText.appendText(stillMissingMangaSummery.append(stillMissingChapters).toString());
            showHidePopup("InComplete Missing Selections", 1500);
        }

        s1.setOnCloseRequest(null);
    };

    final EventHandler<ActionEvent>  mangaInfoAction = e -> {
        Stage fm = new Stage(StageStyle.UTILITY);
        fm.initOwner(stage);

        MangaPresenter mp = getCurrentManga(); 
        Manga m = mp.getManga();

        fm.setTitle(m.getName());

        StringBuilder b = new StringBuilder()
                .append("name: ")
                .append(m.getName())
                .append("\nid: ")
                .append(m.getId())
                .append("\nurl: ")
                .append(m.getUrl())
                .append("\nstatus: ")
                .append(m.getStatus())
                .append("\n\nchapters: ")
                .append("\n\ttotal: ")
                .append(m.chaptersCount())
                .append("\n\tqueued: ")
                .append(mp.queuedCountProperty().get())
                .append("\n\tcompleted: ")
                .append(mp.completedCountProperty().get())
                .append("\n\tfailed: ")
                .append(mp.failedCountProperty().get())
                .append("\n\n");

        TextArea t = new TextArea(b.toString());
        t.setEditable(false);

        fm.setScene(new Scene(new ScrollPane(t)));
        fm.show();
        fm.sizeToScene();
    };

    final MangaEvent onMangaClick = manga -> {
        currentManga.set(manga);
        selectUnselectCB.setSelected(manga.isAllChaptersSelected());
    };

    private ToolBar  getToolBar(){
        BiFunction<String, EventHandler<ActionEvent>, Button> buttonMaker = (label, handler) -> {
            Button b = new Button(null, new ImageView(new Image(DownloaderUtils.getImageInputStream("toolbar/"+label.toLowerCase().replace(' ', '_')+".png"))));
            b.setTooltip(new Tooltip(label));

            if(!label.equals("Current Manga Info") && !label.equals("Open Error log")) 
                b.disableProperty().bind(workingdb.isNull());

            if(handler != null)
                b.setOnAction(handler);

            return b;
        };

        Button openWorkingDir = buttonMaker.apply("Open Root Dir", e -> getHostServices().showDocument("."));
        Button selectAll = buttonMaker.apply("Select all", e -> mangasStream().forEach(m -> m.setAllChapterSelected(true)));
        Button unselectAll = buttonMaker.apply("Unselect all", e -> mangasStream().forEach(m -> m.setAllChapterSelected(false)));
        Button selectRange = buttonMaker.apply("Select Range", e -> getCurrentManga().selectRangeChapters());
        Button selectMissings = buttonMaker.apply("Select missings", loadMissingChaptersAction);

        selectMissings.setTooltip(new Tooltip("Select missing chapter in updated manga(s)\nand all chapter in new manga(s)"));

        Button mangaInfo = buttonMaker.apply("Current Manga Info", mangaInfoAction);
        mangaInfo.disableProperty().bind(currentManga.isNull());

        Button openErrorFile = buttonMaker.apply("Open Error log", e -> showHidePopup("Not Working", 1500));

        Button retry_failed = buttonMaker.apply("Retry Failed", e -> mangasList.forEach(MangaPresenter::retryFailedChapters));

        retry_failed.disableProperty().bind(failedCount.isEqualTo(0));

        return new ToolBar(
                openWorkingDir,
                selectAll,
                unselectAll,
                selectRange,
                selectMissings,
                mangaInfo,
                openErrorFile,
                retry_failed
                );
    }

    final EventHandler<ActionEvent>  importDbAction = e -> {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select mangarock.db");
        File initDir = new File(!CONFIG_MAP.containsKey("import_directory") ? "." : CONFIG_MAP.get("import_directory"));
        chooser.setInitialDirectory(initDir);
        chooser.getExtensionFilters().add(new ExtensionFilter("sql database", "*.db"));

        File file = chooser.showOpenDialog(stage);

        if(file == null)
            showHidePopup("No File selected", 1500);

        CONFIG_MAP.put("import_directory", file.getParent().replace('\\', '/'));

        Path src = file.toPath();
        Path target = CURRENT_SESSION_FOLDER.resolve(file.getName());

        String header = "Error while check if both same file";
        try {
            if(Files.exists(target) && Files.isSameFile(src, target))
                readDatabase(file);
            else{
                if(Files.exists(target)){
                    showMessageDialog(stage, AlertType.ERROR, target+ "\nalready exits hence importing is aborted", "Importing db failed", false);
                    return;
                }
                else{
                    header = "Error while copying db";
                    Files.copy(src, target);
                    readDatabase(target.toFile());
                }
            }
        } catch (IOException e1) {
            showErrorDialog(header, "Importing db failed", e1, false);
        }
    };

    private MenuBar getMenuBar(){
        MenuItem importDatabaseMI = new MenuItem("Import Database");
        importDatabaseMI.disableProperty().bind(workingdb.isNotNull());

        importDatabaseMI.setOnAction(importDbAction);

        MenuItem exitMI = new MenuItem("Exit");
        exitMI.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));
        exitMI.setOnAction(e -> stage.close());

        Menu fileMenu = new Menu("File", null, 
                importDatabaseMI, 
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
        createMangarockDatabaseMi.setOnAction(e -> {
            commitDatabase();
            DownloaderUtils.createMangarockDatabase(workingdb.get());
        });

        MenuItem clearCompletedDownloadsMi = new MenuItem("Clear Completed Downloads");
        clearCompletedDownloadsMi.setOnAction(e -> mangasStream().forEach(MangaPresenter::removeCompletedChaptersFromView));

        MenuItem moveInompletePagesMi = new MenuItem("Move Incomplete Pages");
        moveInompletePagesMi.setOnAction(e -> moveIncompletePages());

        MenuItem fillNotFoundImagesMi = new MenuItem("Fill Not Found Images");
        fillNotFoundImagesMi.setOnAction(e -> DownloaderUtils.fillNotFoundImages());

        MenuItem moveChapterFoldersMi = new MenuItem("Move Chapter Folders");
        moveChapterFoldersMi.setOnAction(e -> DownloaderUtils.moveChapterFolders());

        MenuItem createNmoveMi = new MenuItem("Create and Move");
        createNmoveMi.setOnAction(e -> {
            commitDatabase();
            if(DownloaderUtils.createMangarockDatabase(workingdb.get()))
                DownloaderUtils.moveChapterFolders();
        });

        Menu toolsMenu = new Menu("Tools", null, clearCompletedDownloadsMi, createNmoveMi,
                moveInompletePagesMi,
                createMangarockDatabaseMi,
                moveChapterFoldersMi);

        toolsMenu.disableProperty().bind(workingdb.isNull().or(downloadIsActive));

        return toolsMenu;
    }

    //css is same as counterViewBox
    private VBox getHelpBox(){
        VBox helpBox = new VBox(5);
        Label vlabel = new Label("version: "+DownloaderApp.VERSION);
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
            stage.initOwner(DownloaderApp.getPrimaryStage());
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
    private void readDatabase(File file){
        if(!file.exists()){
            showMessageDialog("file: "+file+"\nnot found", "database file not found");
            return;
        }

        Stage wait =  showWaitIndicator();
        String errorPoint = null;

        try {
            DbManager maneger = new DbManager(file);
            mangasList.clear();
            maneger.getMangas().stream().map(m -> new MangaPresenter(m, onMangaClick)).forEach(mangasList::add);
            workingdb.set(maneger);

            BytesCounter.reset();
        } catch (Exception e) {
            showErrorDialog("Sql Error readDatabase()", "errorPoint:" +errorPoint, e, false);
        }

        Platform.runLater(() -> wait.close());
    }

    AtomicBoolean databaseUpdating = new AtomicBoolean(false);

    private synchronized void commitDatabase() {
        if(!dataUpdated.get())
            return;

        if(databaseUpdating.get())
            return;

        databaseUpdating.getAndSet(true);

        Stage stage = showWaitIndicator();

        try {
            workingdb.get().commitDatabase();
        } catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException | IOException e) {
            showErrorDialog("error while SaveANDExit(2)", "Error", e);
        }
        databaseUpdating.getAndSet(false);
        Platform.runLater(() -> stage.close());
    }

    /**
     * @return Stream.of(mangas);
     */
    private Stream<MangaPresenter> mangasStream(){
        return mangasList.stream();
    }
    private void moveIncompletePages(){
        File initialFile;
        if(CONFIG_MAP.containsKey("moveIncompletePages"))
            initialFile = new File(CONFIG_MAP.get("moveIncompletePages"));
        else if(Files.exists(HALTED_IMAGE_DIR))
            initialFile = HALTED_IMAGE_DIR.toFile();
        else 
            initialFile = new File(".");

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setInitialDirectory(initialFile);

        File file = chooser.showDialog(stage);

        if(file == null){
            showHidePopup("Cancelled", 1500);
            return;
        }

        CONFIG_MAP.put("moveIncompletePages", file.toString().replace('\\', '/'));

        File[] files = file.listFiles(f -> f.isFile() && f.getName().matches("\\d+\\.jpe?g"));

        if(files.length == 0){
            showMessageDialog("Folder: "+file, "Folder is empty", true);
            return;
        }

        Map<Integer, File> folderContents = Stream.of(files).collect(Collectors.toMap(f -> Integer.parseInt(f.getName().replaceFirst("\\.jpe?g$",  "")), Function.identity(), (o, n) -> n));

        Map<Integer, Path> sink = new HashMap<>();
        mangasStream().forEach(m -> m.fillPageSavePaths(folderContents.keySet(), sink));

        if(files.length == 0){
            showMessageDialog("no relative data found", "Move Failed", true);
            return;
        }

        HashMap<File, Path> inOutMap = new HashMap<>();
        sink.forEach((id, path) -> inOutMap.put(folderContents.get(id), path));

        HashMap<Path, Boolean> replaceConfirmation = new HashMap<>();
        boolean[] cancelEntireMove = {false};

        //confirmations for existing
        inOutMap.forEach((File in, Path out) -> {
            if(cancelEntireMove[0])
                return;

            if(Files.exists(out)){
                int choice = confirmImages(in , out);
                if(choice == CONFIRM_IMAGES_CANCEL_ALL){
                    cancelEntireMove[0] = true;
                    return;
                }
                else replaceConfirmation.put(out, choice == CONFIRM_IMAGES_YES);
            }
        });

        if(cancelEntireMove[0]){
            showHidePopup("Entire move cancelled", 2000);
            return;
        }

        Object[] logger = DownloaderUtils.getLoggerStage(false);
        Stage s1 = (Stage)logger[0];
        s1.setOnCloseRequest(e -> e.consume());
        TextArea logText = (TextArea)logger[1];

        ArrayList<File> successFiles = new ArrayList<>();

        inOutMap.forEach((File in, Path out) -> {
            try {
                if(replaceConfirmation.containsKey(out)){
                    if(replaceConfirmation.get(out)){
                        Files.move(in.toPath(), out, StandardCopyOption.REPLACE_EXISTING);
                        logText.appendText("Success -> in: "+in+"\tout: "+out+"\n");
                        successFiles.add(in);
                    }
                    else
                        logText.appendText("Refused by User -> in: "+in+"\tout: "+out+"\n");
                }
                else{
                    Files.move(in.toPath(), out);
                    logText.appendText("Success -> in: "+in+"\tout: "+out+"\n");
                    successFiles.add(in);
                }
            } catch (IOException e) {
                logText.appendText("\nin: "+in+"\nout: "+out+"\nError: "+e+"\n\n");
            }
        });

        HashSet<Integer> successIds = new HashSet<>();
        folderContents.forEach((id, f) -> {
            if(successFiles.contains(f))
                successIds.add(id);
        });

        mangasStream().forEach(m -> m.updateIfHasPages(successIds));
        logText.appendText(ANSI.createUnColoredBanner("Completed", 15, '#'));
        s1.setOnCloseRequest(null);
    }

    private final int CONFIRM_IMAGES_YES = 1;
    private final int CONFIRM_IMAGES_NO = 2;
    private final int CONFIRM_IMAGES_CANCEL_ALL = 3;

    private int confirmImages(File in, Path out) {
        SplitPane views = new SplitPane( createImageView(in.toPath(), "NEW"), createImageView(out, "OLD") );

        BorderPane root = new BorderPane(views);

        Stage stage  = new Stage(StageStyle.UTILITY);
        stage .initModality(Modality.APPLICATION_MODAL);
        stage .initOwner(DownloaderApp.stage);

        int[] returnValue = {-1};

        Button yesButton = new Button("Yes");
        Button noButton = new Button("No");
        Button cancelAllButton = new Button("Cancel Entire Move");

        EventHandler<ActionEvent> handler = e -> {
            Object src = e.getSource();

            if(src == yesButton)
                returnValue[0] = CONFIRM_IMAGES_YES;
            else if(src == noButton)
                returnValue[0] = CONFIRM_IMAGES_NO;
            else if(src == cancelAllButton){
                if(showConfirmDialog(stage, "this will cancel entire move.\nwant to continue?", "Cancel Entire Move"))
                    returnValue[0] = CONFIRM_IMAGES_CANCEL_ALL;
                else
                    return;
            }
            stage.close();
        };

        yesButton.setOnAction(handler);
        noButton.setOnAction(handler);
        cancelAllButton.setOnAction(handler);

        stage.setOnCloseRequest(e -> {
            if(returnValue[0] == -1)
                cancelAllButton.fire();

            if(returnValue[0] == -1)
                e.consume();
        });

        HBox controls = new HBox(15, new Label("Move? "), yesButton, noButton, cancelAllButton);
        root.setBottom(controls);
        controls.setPadding(new Insets(10));
        HBox.setHgrow(cancelAllButton, Priority.ALWAYS);
        HBox.setMargin(cancelAllButton, new Insets(0, 0, 0, 100));

        stage.setScene(new Scene(root, 800, 500));
        stage.showAndWait();

        return returnValue[0];
    }

    private Node createImageView(Path path, String label){
        Text text = new Text(label+"   ");
        text.setFont(Font.font(20));
        text.setTextAlignment(TextAlignment.CENTER);

        HBox detailsBox = new HBox(10, text);
        VBox root = new VBox(10, detailsBox);

        try {
            Image img = new Image(Files.newInputStream(path));
            ImageView iv = new ImageView(img);

            if(!img.isError()){
                detailsBox.getChildren().add(new Text("file size: "+MyUtils.bytesToHumanReadableUnits(Files.size(path), false, new StringBuilder())+",  W: "+img.getWidth()+",  H: "+img.getHeight()));
                iv.setSmooth(true);
                iv.setPreserveRatio(true);

                TextField wfield = new TextField();
                TextField hfield = new TextField("200");
                wfield.setPrefColumnCount(3);
                hfield.setPrefColumnCount(3);

                EventHandler<ActionEvent> handler = e -> {
                    TextField f = (TextField)e.getSource();
                    String inputS = f.getText();

                    if(inputS == null || (inputS = inputS.trim()).isEmpty() || !inputS.matches("\\d+")){
                        showHidePopup("Invalid input", 1500);
                        return;
                    }
                    int size = Integer.parseInt(inputS);

                    if(f == wfield)
                        iv.setFitWidth(size);
                    else
                        iv.setFitHeight(size);

                    wfield.setText(iv.getFitWidth()+"");
                    hfield.setText(iv.getFitHeight()+"");
                };
                wfield.setOnAction(handler);
                hfield.setOnAction(handler);

                if(img.getHeight() > 400)
                    iv.setFitHeight(400);

                root.getChildren().addAll(new HBox(10, new Text("fit width: "), wfield, new Text("fit height: "), hfield), new ScrollPane(iv));
            }
            else
                root.getChildren().add(new Text(img.getException()+""));
        } catch (Exception e) {
            detailsBox.getChildren().add(new Text(e+""));
        }

        root.setPadding(new Insets(10));
        Border border = new Border(new BorderStroke(Color.DARKBLUE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(2)));
        root.setBorder(border);
        return root;
    }

    private static Stage showWaitIndicator(){
        Stage popup = new Stage(StageStyle.TRANSPARENT);
        popup.initOwner(stage);
        popup.initModality(Modality.APPLICATION_MODAL);

        ProgressIndicator indicator = new ProgressIndicator();

        popup.setScene(new Scene(indicator));;

        Platform.runLater(() -> {
            popup.show();

            if(stage != null && stage.isShowing()){
                popup.setX(stage.getX() + stage.getWidth()/2 - popup.getWidth()/2);
                popup.setY(stage.getY() + stage.getHeight()/2 - popup.getHeight()/2);
            }
        });

        return popup;
    }


}
