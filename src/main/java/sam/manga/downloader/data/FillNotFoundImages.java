package sam.manga.downloader.data;

import static sam.fx.alert.FxAlert.showMessageDialog;
import static sam.fx.popup.FxPopupShop.showHidePopup;
import static sam.manga.downloader.extra.Utils.HALTED_IMAGE_DIR;
import static sam.manga.downloader.extra.Utils.stage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.fx.alert.FxAlert;
import sam.manga.downloader.TextStage;
import sam.manga.downloader.extra.Utils;

public class FillNotFoundImages {
    public FillNotFoundImages(){
        InputStream inputStream = Utils.getImageInputStream("not_found.jpg");
        Path tempDir = HALTED_IMAGE_DIR;

        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.initOwner(stage());

        alert.setHeaderText("Enter id(s)");

        TextArea input = new TextArea();
        input.setPrefColumnCount(10);
        input.setPrefRowCount(10);
        alert.getDialogPane().setContent(new VBox(10, new Text("Enter ID(s) of page(s) separated by non-numeric charactor"), input));

        Optional<ButtonType> result = alert.showAndWait();
        if(!result.isPresent() || result.get() != ButtonType.OK){
            showHidePopup("Cancelled", 1500);
            return;
        }

        String text = input.getText();
        int[] ids = Stream.of(text.split("\\D+"))
                .filter(s -> s.trim().matches("\\d+"))
                .mapToInt(s -> Integer.parseInt(s.trim()))
                .toArray();

        if(ids.length == 0){
            FxAlert.showMessageDialog(stage(), AlertType.ERROR, "Empty Input", "Fill Not Found Images Error", true);
            return;
        }
        else {
            TextStage logger = TextStage.open();
            logger.setTitle("Fill not found Images");

            int sucess = 0, total = 0;
            for (int i : ids) {
                Path out = tempDir.resolve(i+".jpg");
                try {
                    total++;
                    if(Files.exists(out))
                        logger.appendText("alreay exits: "+out+"\n");
                    else
                        Files.copy(inputStream, out);
                    sucess++;
                } catch (IOException e1) {
                    logger.appendText("Failed copy: "+out+"\n");
                }   
            }
            logger.close();
            showMessageDialog("Total: "+total+"\nSuccess: "+sucess+"\nFailed: "+(total- sucess), "Fill Not Found Images Completed");
        }
    }

}
