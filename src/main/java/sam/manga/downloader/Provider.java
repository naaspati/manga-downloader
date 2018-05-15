package sam.manga.downloader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.function.Consumer;

import javafx.beans.binding.IntegerExpression;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.Parent;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ToolBar;
import sam.config.MyConfig;
import sam.fx.alert.FxAlert;
import sam.manga.downloader.data.CreateMangarockDatabase;
import sam.manga.downloader.data.DataManager;
import sam.manga.downloader.data.ProcessResult;
import sam.manga.downloader.data.TsvMangaLoader;
import sam.manga.downloader.manga.MangaPresenter;

public class Provider {
    private static volatile Provider instance;

    public static Provider init() {
        if (instance == null) {
            synchronized (Provider.class) {
                if (instance == null)
                    instance = new Provider();
            }
        }
        return instance;
    }

    private Provider() { }

    private WestPane westpane0;
    private MenuBar2 menuBar20;
    private DataManager dataManager0;
    private ToolsMenu toolsMenu0;
    private RootView rootView0;
    private CenterView centerView0;
    private ToolBar2 toolBar20;

    public Parent rootView() {
        if(rootView0 != null)
            return rootView0;

        return rootView0 = new RootView(toolBar2(), menuBar2(), westpane(), centerView());
    }

    private ToolBar toolBar2() {
        if(toolBar20 != null)
            return toolBar20;

        return toolBar20 = new ToolBar2(westpane().currentMangaProperty(), failedChaptersCountProperty(), this::forEachMangaPresenter);
    }

    private Parent centerView() {
        if(centerView0 != null)
            return centerView0;
        return centerView0 = new CenterView(westpane().currentMangaProperty());
    }
    private MenuBar2 menuBar2() {
        if(menuBar20 != null)
            return menuBar20;

        return menuBar20 = new MenuBar2(toolsmenu());
    }

    private ToolsMenu toolsmenu() {
        if(toolsMenu0 != null)
            return toolsMenu0 ;

        return  toolsMenu0 = new ToolsMenu(westpane().mangaPresenters(), westpane().downloadIsActiveProperty(), this::createMangarockDatabase); 
    }

    private void createMangarockDatabase(boolean moveFolders) {
        ProcessResult result = new CreateMangarockDatabase(moveFolders, datamanager()).result();
        if(result != null) {
            FxAlert.alertBuilder(AlertType.ERROR)
            .header(result.getErrorTitle())
            .content(result.getErrorBody())
            .exception(result.getException())
            .show();
        } 
    }
    private WestPane westpane() {
        if(westpane0 != null)
            return westpane0;

        westpane0 = new WestPane();
        westpane0.init(datamanager().getMangas());

        return westpane0;
    }
    private DataManager datamanager() {
        if(dataManager0 != null)
            return dataManager0;

        try {
            return dataManager0 = new DataManager(tsvMangaLoader());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException
                | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TsvMangaLoader tsvMangaLoader() throws IOException {
        Path p1 = Paths.get(MyConfig.NEW_MANGAS_TSV_FILE);
        Path p2 = Paths.get(MyConfig.UPDATED_MANGAS_TSV_FILE);

        if(Files.exists(p1) && Files.exists(p2))
            return new TsvMangaLoader(p1, p2);
        if(Files.exists(p1))
            return new TsvMangaLoader(p1);
        if(Files.exists(p2))
            return new TsvMangaLoader(p2);

        return null;
    }

    public IntegerExpression failedChaptersCountProperty() {
        return westpane().failedCountPropery();
    }
    public void forEachMangaPresenter(Consumer<MangaPresenter> action) {
        westpane().forEach(action);
    }
    public ReadOnlyObjectProperty<MangaPresenter> currentManga() {
        return westpane().currentMangaProperty();
    }
    public boolean isExitAfterComplete() {
        return menuBar2().exitAfterComplete();
    }
    public boolean isShutdownAfterComplete() {
        return menuBar2().shutdownAfterComplete();
    }
    public void setShutdownAfterComplete(boolean b) {
        menuBar2().setShutdownAfterComplete(b);
    }

    public boolean isDownloadIsActive() {
        return westpane().isDownloadActive();
    }

    public void stop() throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException {
        if(westpane0 != null)
            westpane0.stop();
        if(dataManager0 != null)
            dataManager0.stop();
    }
}
