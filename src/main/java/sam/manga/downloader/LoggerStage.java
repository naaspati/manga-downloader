package sam.manga.downloader;

import static sam.manga.downloader.extra.Utils.stage;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

public class LoggerStage extends Stage {
    public final ProgressBar progressBar;
    public final TextArea textArea; 
    
    public LoggerStage(boolean addProgressBar, EventHandler<WindowEvent> onClose) {
        super(StageStyle.UTILITY);
        initOwner(stage());
        initModality(Modality.WINDOW_MODAL);

        textArea = new TextArea();

        if(addProgressBar){
            progressBar = new ProgressBar(0);
            progressBar.setMaxWidth(Double.MAX_VALUE);
            setScene(new Scene(new BorderPane(textArea, null, null, progressBar, null), 500, 500));
        }
        else {
            setScene(new Scene(textArea, 500, 500));
            progressBar = null;
        }
        if(onClose != null)
            setOnCloseRequest(onClose);
    }
}
