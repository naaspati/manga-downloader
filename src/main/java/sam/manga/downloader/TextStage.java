package sam.manga.downloader;

import static sam.manga.downloader.extra.Utils.stage;

import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sam.manga.downloader.extra.Utils;
import sam.weak.WeakStore;

public class TextStage {
    @FXML private ProgressBar bar;
    @FXML private TextArea ta;
    
    private static final WeakStore<TextStage> STORE = new WeakStore<>(TextStage::new);
    public static TextStage open() {
        return open(true);
    }
    private static TextStage open(boolean show) {
        TextStage stg = STORE.poll(); 
        stg.ta.setText(null);
        stg.bar.setProgress(-1);
        stg.stage.setOnCloseRequest(e -> e.consume());
        
        if(show)
            stg.stage.show();
        
        return stg;
    }
    
    private final Stage stage; 

    private TextStage() {
        stage = new  Stage(StageStyle.UTILITY);
        stage.initOwner(stage());
        stage.initModality(Modality.WINDOW_MODAL);
        
        Utils.fxml(this, stage, this);
    }
    
    public void appendText(String string) {
        ta.appendText(string);
    }
    public void close() {
        STORE.add(this);
        bar.setProgress(1);
        stage.setOnCloseRequest(null);
    }
    public void setTitle(String title) {
        stage.setTitle(title);
    }
}
