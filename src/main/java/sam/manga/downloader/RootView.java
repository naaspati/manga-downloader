package sam.manga.downloader;

import javafx.scene.Parent;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class RootView extends BorderPane {
    public RootView(ToolBar toolBar, MenuBar menuBar, Parent westpane, Parent centerView) {
        
        BorderPane top = new BorderPane(toolBar);
        top.setTop(new HBox(10, menuBar, BytesCounter.COUNTER_VIEW));
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        
        setTop(top);
        setLeft(westpane);
        setCenter(centerView);
    }

}
