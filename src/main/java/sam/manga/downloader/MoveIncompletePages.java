package sam.manga.downloader;

import static sam.fx.alert.FxAlert.showConfirmDialog;
import static sam.fx.alert.FxAlert.showMessageDialog;
import static sam.fx.popup.FxPopupShop.showHidePopup;
import static sam.manga.downloader.extra.Utils.HALTED_IMAGE_DIR;
import static sam.manga.downloader.extra.Utils.stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sam.config.Session;
import sam.console.ANSI;
import sam.manga.downloader.manga.MangaPresenter;
import sam.myutils.MyUtils;

class MoveIncompletePages {

    public MoveIncompletePages(List<MangaPresenter> mangas) {
        File initialFile;
        
        if(Session.has("moveIncompletePages"))
            initialFile = new File(Session.get("moveIncompletePages"));
        else if(Files.exists(HALTED_IMAGE_DIR))
            initialFile = HALTED_IMAGE_DIR.toFile();
        else 
            initialFile = new File(".");

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setInitialDirectory(initialFile);

        File file = chooser.showDialog(stage());

        if(file == null){
            showHidePopup("Cancelled", 1500);
            return;
        }

        Session.put("moveIncompletePages", file.toString().replace('\\', '/'));

        File[] files = file.listFiles(f -> f.isFile() && f.getName().matches("\\d+\\.jpe?g"));

        if(files.length == 0){
            showMessageDialog("Folder: "+file, "Folder is empty", true);
            return;
        }

        Map<Integer, File> folderContents = Stream.of(files).collect(Collectors.toMap(f -> Integer.parseInt(f.getName().replaceFirst("\\.jpe?g$",  "")), Function.identity(), (o, n) -> n));

        Map<Integer, Path> sink = new HashMap<>();
        for (MangaPresenter m : mangas) m.fillPageSavePaths(folderContents.keySet(), sink);

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

            TextStage logger = TextStage.open();
            ArrayList<File> successFiles = new ArrayList<>();

            inOutMap.forEach((File in, Path out) -> {
                try {
                    if(replaceConfirmation.containsKey(out)){
                        if(replaceConfirmation.get(out)){
                            Files.move(in.toPath(), out, StandardCopyOption.REPLACE_EXISTING);
                            logger.appendText("Success -> in: "+in+"\tout: "+out+"\n");
                            successFiles.add(in);
                        }
                        else
                            logger.appendText("Refused by User -> in: "+in+"\tout: "+out+"\n");
                    }
                    else{
                        Files.move(in.toPath(), out);
                        logger.appendText("Success -> in: "+in+"\tout: "+out+"\n");
                        successFiles.add(in);
                    }
                } catch (IOException e) {
                    logger.appendText("\nin: "+in+"\nout: "+out+"\nError: "+e+"\n\n");
                }
            });
            
            HashSet<Integer> successIds = new HashSet<>();
            folderContents.forEach((id, f) -> {
                if(successFiles.contains(f))
                    successIds.add(id);
            });

            mangas.forEach(m -> m.updateIfHasPages(successIds));
            logger.appendText(ANSI.createUnColoredBanner("Completed", 15, '#'));
            logger.close();
    }

    private final int CONFIRM_IMAGES_YES = 1;
    private final int CONFIRM_IMAGES_NO = 2;
    private final int CONFIRM_IMAGES_CANCEL_ALL = 3;

    private int confirmImages(File in, Path out) {
        SplitPane views = new SplitPane( createImageView(in.toPath(), "NEW"), createImageView(out, "OLD") );

        BorderPane root = new BorderPane(views);

        Stage stage  = new Stage(StageStyle.UTILITY);
        stage .initModality(Modality.APPLICATION_MODAL);
        stage .initOwner(stage);

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
}
