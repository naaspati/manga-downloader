package sam.manga.downloader;

import static sam.manga.downloader.extra.Utils.stage;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class WaitIndicator extends Stage {
    public WaitIndicator() {
        super(StageStyle.TRANSPARENT);
        initOwner(stage());
        initModality(Modality.APPLICATION_MODAL);

        ProgressIndicator indicator = new ProgressIndicator();

        setScene(new Scene(indicator));;

        Platform.runLater(() -> {
            show();

            if(stage() != null && stage().isShowing()){
                setX(stage().getX() + stage().getWidth()/2 - getWidth()/2);
                setY(stage().getY() + stage().getHeight()/2 - getHeight()/2);
            }
        });
    }
}
