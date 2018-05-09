package sam.manga.downloader;

import static sam.fx.alert.FxAlert.showConfirmDialog;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import sam.fx.alert.FxAlert;
import sam.fx.popup.FxPopupShop;
import sam.manga.downloader.extra.Utils;

public class App extends Application {
    public static final double VERSION = 8.31;
    
    private Provider provider = Provider.init();

    @Override
    public void start(Stage stage) throws Exception {
        FxAlert.setParent(stage);
        FxPopupShop.setParent(stage);

        stage.setTitle("Downloader: "+VERSION);
        stage.getIcons().add(new Image(ClassLoader.getSystemResource("icon.png").toExternalForm()));
        
        Scene scene = new Scene(provider.rootView());
        scene.getStylesheets().add(ClassLoader.getSystemResource("stylesheet/main_stylesheet.css").toExternalForm());

        stage.setScene(scene);
        stage.setOnCloseRequest(closeRequestAction);

        stage.setMaximized(true);
        stage.show();

        Utils.init(stage, getHostServices());
    }

    EventHandler<WindowEvent> closeRequestAction = e -> {
        if(provider.isDownloadIsActive()){
            if(showConfirmDialog(Utils.stage(), "This will stop all current download and exit", "R U SURE ?")){
                if(provider.isShutdownAfterComplete())
                    provider.setShutdownAfterComplete(false);
            } else
                e.consume();
        }
    };

    @Override
    public void stop() throws Exception {
        provider.stop();
        Utils.stop();
        
        System.exit(0);
    }
}
