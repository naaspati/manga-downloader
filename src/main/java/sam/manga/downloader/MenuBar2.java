package sam.manga.downloader;

import static sam.manga.downloader.extra.Utils.stage;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sam.fx.helpers.FxMenu;
import sam.weak.WeakKeep;

public class MenuBar2 extends MenuBar  {
    private final CheckMenuItem exitAfterCompleteCMI = new CheckMenuItem("Exit After complete");
    private final CheckMenuItem shutdownAfterCompleteCMI = new CheckMenuItem("Shutdown After complete");
    
    public MenuBar2(Menu toolsMenu) {
        MenuItem exitMI = FxMenu.menuitem("Exit", e -> stage().close());
        exitMI.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));

        Menu fileMenu = new Menu("File", null, 
                new SeparatorMenuItem(), 
                exitAfterCompleteCMI, 
                shutdownAfterCompleteCMI, 
                new SeparatorMenuItem(), 
                exitMI);

        MenuItem help = FxMenu.menuitem("Help", e -> Platform.runLater(() -> stage.get().show()));
        Menu appMenu = new Menu("App", null, help);
        getMenus().addAll(fileMenu, appMenu, toolsMenu);
    }
    
    private WeakKeep<Stage> stage = new WeakKeep<>(() -> {
        Stage stage = new Stage(StageStyle.UTILITY);
        stage.initOwner(stage());
        stage.initModality(Modality.APPLICATION_MODAL);

        Scene scene = new Scene(new HelpBox());
        scene.getStylesheets().addAll(ClassLoader.getSystemResource("stylesheet/main_stylesheet.css").toExternalForm());
        stage.setScene(scene);

        stage.getScene().setOnKeyPressed(e -> {
            if(e.getCode() == KeyCode.ESCAPE)
                stage.close();
        });
        return stage;
    });

    public boolean exitAfterComplete() {
        return exitAfterCompleteCMI.isSelected();
    }
    public boolean shutdownAfterComplete() {
        return shutdownAfterCompleteCMI.isSelected();
    }
    public void setShutdownAfterComplete(boolean b) {
        shutdownAfterCompleteCMI.setSelected(b);
    }
}
