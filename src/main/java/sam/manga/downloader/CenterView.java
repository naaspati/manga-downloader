package sam.manga.downloader;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectExpression;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.BorderPane;
import sam.manga.downloader.chapter.ChapterPresenter;
import sam.manga.downloader.chapter.ChapterSelectionListCell;
import sam.manga.downloader.extra.Utils;
import sam.manga.downloader.manga.MangaPresenter;

public class CenterView extends BorderPane {
    @FXML Label mangaName;
    @FXML Button menuButton;
    @FXML TabPane tabpane;
  //  @FXML Tab selectionTab;
    @FXML CheckBox selectUnselectCB;
    @FXML ListView<ChapterPresenter> selectionList;
//    @FXML Tab downloadingViewTab;
    @FXML ListView<ChapterPresenter> downloadingViewList;
    private final ObservableList<ChapterPresenter> chaptersList = FXCollections.observableArrayList();
    
    public CenterView(ObjectExpression<MangaPresenter> currentManga) {
        Utils.fxml(this);
        Utils.stylesheet(this);
        
        selectionList.setItems(chaptersList);
        downloadingViewList.setItems(chaptersList); //TODO
        
        selectionList.setCellFactory(call -> new ChapterSelectionListCell());
        
        // downloadingViewList.setCellFactory (callback -> new SelectionCell());
        tabpane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
        
        selectUnselectCB.textProperty().bind(Bindings.when(selectUnselectCB.selectedProperty()).then("Unselect All").otherwise("Select All"));
        selectUnselectCB.visibleProperty().bind(currentManga.isNotNull());
        selectUnselectCB.setPadding(new Insets(5));
        
        //why using setOnAction rather then Binding? because setOnAction will not fire if setSelected is called programeitically
        //this is necessary when when currentManga changes
        selectUnselectCB.setOnAction(e -> currentManga.get().setAllChapterSelected(selectUnselectCB.isSelected()));
        
        currentManga.addListener((p, o, n) -> reset(n));
    }

    private void reset(MangaPresenter manga) {
        chaptersList.clear();
        
        if(manga == null)
            return;
        
        if (manga.isDownloading())
            tabpane.getSelectionModel().select(1);
        
        boolean[] b = {true};
        manga.forEach(c -> {
            b[0] = b[0] && c.isSelected();
        });
        selectUnselectCB.setSelected(b[0]);
    }
}