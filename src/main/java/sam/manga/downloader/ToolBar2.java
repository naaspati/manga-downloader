package sam.manga.downloader;

import java.util.List;
import java.util.function.Consumer;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import sam.fx.popup.FxPopupShop;
import sam.manga.downloader.extra.Utils;
import sam.manga.downloader.manga.MangaPresenter;

public class ToolBar2 extends ToolBar {
    private final List<MangaPresenter> mangasList;

    public ToolBar2(List<MangaPresenter> mangasList, ReadOnlyObjectProperty<MangaPresenter> currentManga, ReadOnlyIntegerProperty failedCount) {
        this.mangasList = mangasList;
        
        Button openWorkingDir = buttonMaker("Open Root Dir", e -> Utils.hostService().showDocument("."));
        Button selectAll = buttonMaker("Select all", e -> mangaForEach(m -> m.setAllChapterSelected(true)));
        Button unselectAll = buttonMaker("Unselect all", e -> mangaForEach(m -> m.setAllChapterSelected(false)));
        Button selectRange = buttonMaker("Select Range", e -> currentManga.get().selectRangeChapters());

        Button mangaInfo = buttonMaker("Current Manga Info", new MangaInfoAction(currentManga));
        mangaInfo.disableProperty().bind(currentManga.isNull());
        
        Button openErrorFile = buttonMaker("Open Error log", e -> FxPopupShop.showHidePopup("Not Working", 1500));
        Button retry_failed = buttonMaker("Retry Failed", e -> mangasList.forEach(MangaPresenter::retryFailedChapters));
        retry_failed.disableProperty().bind(failedCount.isEqualTo(0));
        

        getItems().addAll(openWorkingDir,
                selectAll,
                unselectAll,
                selectRange,
                mangaInfo,
                openErrorFile,
                retry_failed);
    }
    private void mangaForEach(Consumer<MangaPresenter> action) {
        mangasList.forEach(action);
    }

    private Button buttonMaker(String label, EventHandler<ActionEvent> handler) {
        Button b = new Button(null, new ImageView(new Image(Utils.getImageInputStream("toolbar/"+label.toLowerCase().replace(' ', '_')+".png"))));
        b.setTooltip(new Tooltip(label));

        if(handler != null)
            b.setOnAction(handler);

        return b;
    }
}
