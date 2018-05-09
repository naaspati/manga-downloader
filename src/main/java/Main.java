import java.util.Arrays;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import sam.manga.downloader.App;
import sam.manga.downloader.TextStage;
import sam.myutils.MyUtils;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        Button b = new Button("aa");
        
        b.setOnAction(e -> {
            TextStage s = TextStage.open();
            MyUtils.runOnDeamonThread(() -> {
                for (int i = 0; i < 100; i++) {
                    int j = i;
                    Platform.runLater(() -> s.appendText(String.valueOf(j) + "\n"));
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
                Platform.runLater(() -> s.close());
            });
        });
        
        stage.setScene(new Scene(b));
        stage.show();
    }
    
    public static void main2(String[] args) throws ClassNotFoundException {
        if(args.length == 1 &&  Arrays.asList("version","-v", "--version").contains(args[0])){
            System.out.println(App.VERSION);
            System.exit(0);
        }
        Application.launch(App.class, args);
    }
}
