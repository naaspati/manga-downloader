package sam.manga.downloader.chapter;

import static javafx.concurrent.Worker.State.FAILED;
import static javafx.concurrent.Worker.State.READY;
import static javafx.concurrent.Worker.State.SCHEDULED;
import static javafx.concurrent.Worker.State.SUCCEEDED;
import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.setClass;

import javafx.concurrent.Service;
import javafx.concurrent.Worker.State;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.fx.helpers.FxText;
import sam.manga.downloader.extra.Utils;

public class ChapterDownloadListCell extends ListCell<ChapterPresenter>  {
    private ProgressBar bar;
    private Text completedT;
    private Text nameT;
    private VBox vbox;
    private HBox hbox;
    private ChapterPresenter chapter;
    private Service<Void> service;
    private ErrorPane errorPane;
    private State state;

    @Override
    protected void updateItem(ChapterPresenter item, boolean empty) {
        super.updateItem(item, empty);
        setText(null);
        setGraphic(null);
        setClass(this, "chapter-download-cell");
        this.state = null;

        if(chapter != null)
            chapter.removeListener(listener);

        if(bar != null) {
            bar.progressProperty().unbind();
            bar.setProgress(0);
        }
        this.chapter = empty ? null : item;
        this.service = chapter == null ? null : chapter.getChapterService();

        if(chapter == null)
            return;

        if(service == null) {
            setGraphic(completedBox());
            return;
        }
        set();
    }
    private void set() {
        state = service.getState();
        addClass(this, Utils.styleClass(state));

        if(state == null || state == READY || state == SCHEDULED)
            setText(chapter.getChapterName());
        else if(state == FAILED)
            setGraphic(vbox(nameText(), progressBar(-1), errorPane()));
        else
            setGraphic(vbox(nameText(), progressBar(state == SUCCEEDED ? 1 : -1)));
    }
    private Node errorPane() {
        if(errorPane == null)
            errorPane = new ErrorPane();
        errorPane.set(this);
        return errorPane;
    }
    private VBox vbox(Node...nodes) {
        if(vbox == null) vbox = new VBox(5, nodes);
        else  vbox.getChildren().setAll(nodes);

        return vbox;
    }
    private Node completedBox() {
        if(hbox == null)
            hbox = new HBox(5, nameText(), completedText()); 
        return null;
    }
    private Node nameText() {
        if(nameT == null)
            nameT = FxText.ofClass("name-txt");
        nameT.setText(chapter.getChapterName());
        return nameT;
    }
    private ProgressBar progressBar(double progress) {
        if(bar == null) {
            bar = new ProgressBar();
            bar.setMaxWidth(Double.MAX_VALUE);
            VBox.setVgrow(bar, Priority.ALWAYS);
        }
        if(progress < 0)
            bar.progressProperty().bind(service.progressProperty());
        else
            bar.setProgress(progress);
        return bar;
    }
    public void restart() {
        chapter.restart();
        if(vbox != null)
            vbox.getChildren().remove(errorPane);
    }
    public Chapter getChapter() {
        return chapter.getChapter();
    }
    public ChapterService getChapterService() {
        return chapter.getChapterService();
    }
    private Node completedText() {
        if(completedT == null)
            completedT = FxText.of("COMPLETED", "completed-txt");
        return completedT;
    }

    private final ChapterPresenterListener listener = new ChapterPresenterListener() {
        @Override
        public void stateChange(ChapterPresenter chapter, State oldValue, State state) {
            if(ChapterDownloadListCell.this.state == state  || state == READY || state == SCHEDULED)
                return;

            set();
            
            /**
             *             if(getGraphic() != vbox) {
                vbox(nameText(), progressBar(-1));
            }

            if(state != FAILED)
                getChildren().remove(errorPane);

            if(state == RUNNING){
                if(!getChildren().contains(bar))
                    getChildren().add(bar);
                bar.progressProperty().unbind();
                bar.progressProperty().bind(service.progressProperty());
            }
            else if(state == CANCELLED || state == SUCCEEDED || state == FAILED){
                double completed = getCompletedCount();
                bar.progressProperty().unbind();
                bar.setProgress(completed/getPagesCount());
                bar.getStyleClass().removeAll(removeClassesArray);

                if(state != CANCELLED)
                    bar.getStyleClass().add(chapter.getStatus().getClassName());

                if(state == FAILED){
                    bar.setProgress(1);
                    bar.getStyleClass().add(FAILED.getClassName());                     
                    if(restartBtn != null)
                        restartBtn.setDisable(false);
                    addErrorPane();
                }
                else if(chapter.isCompleted())
                    setCompleted();
            }
             */
        }

        @Override
        public void selectionChange(boolean newValue) {
            // TODO Auto-generated method stub

        }
        @Override
        public void chapterNameChange(String chapterName) {
            // TODO Auto-generated method stub
            
        }
    };
}
