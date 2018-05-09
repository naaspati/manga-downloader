package sam.manga.downloader;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.manga.downloader.extra.Status;
import sam.manga.downloader.extra.Utils;

public class HelpBox extends VBox{
    Label version;
    
    public HelpBox(){
        Utils.fxml(this);
        
        version.setText("version: "+App.VERSION);

        add(Status.QUEUED);
        add(Status.RUNNING);
        add(Status.COMPLETED);
        add(Status.FAILED);

        Text termsText1 = new Text("\nTerms...");
        termsText1.getStyleClass().add("info-panel-header");

        Text termsText = new Text(
                " C : Completed Count\n"+
                        " F : Failed Count\n"+
                        " R : Remaining in Queue\n"+
                        " Q : Queued Count\n"+
                " T : Total Chapters\n");


        termsText.getStyleClass().add("info-panel-box");

        getChildren().addAll(termsText1, termsText);

        setPadding(new Insets(10));
        setId("help-box");
    }

    private void add(Status d) {
        Label l = new Label(d.getClassName());
        l.getStyleClass().add(d.getClassName());
        l.setPadding(new Insets(5));
        l.getStyleClass().add("info-panel-box");
        getChildren().add(l);
    }   
}
