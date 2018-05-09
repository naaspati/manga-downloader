package sam.manga.downloader;

import java.util.function.Consumer;

import javafx.beans.binding.IntegerExpression;
import javafx.beans.binding.ObjectExpression;
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

class ToolBar2 extends ToolBar {
    
    public ToolBar2(ObjectExpression<MangaPresenter> currentManga, IntegerExpression failedChaptersCountProperty, Consumer<Consumer<MangaPresenter>> forEachMangaPresenter) {
        Button openWorkingDir = buttonMaker("Open Root Dir", e -> Utils.showDocument("."));
        Button selectAll = buttonMaker("Select all", e -> forEachMangaPresenter.accept(m -> m.setAllChapterSelected(true)));
        Button unselectAll = buttonMaker("Unselect all", e -> forEachMangaPresenter.accept(m -> m.setAllChapterSelected(false)));
        Button selectRange = buttonMaker("Select Range", e -> currentManga.get().selectRangeChapters());

        Button mangaInfo = buttonMaker("Current Manga Info", new MangaInfoAction(currentManga));
        mangaInfo.disableProperty().bind(currentManga.isNull());
        
        Button openErrorFile = buttonMaker("Open Error log", e -> FxPopupShop.showHidePopup("Not Working", 1500));
        Button retry_failed = buttonMaker("Retry Failed", e -> forEachMangaPresenter.accept(MangaPresenter::retryFailedChapters));
        retry_failed.disableProperty().bind(failedChaptersCountProperty.isEqualTo(0));

        getItems().addAll(openWorkingDir,
                selectAll,
                unselectAll,
                selectRange,
                mangaInfo,
                openErrorFile,
                retry_failed);
    }

    private Button buttonMaker(String label, EventHandler<ActionEvent> handler) {
        Button b = new Button(null, new ImageView(new Image(Utils.getImageInputStream("toolbar/"+label.toLowerCase().replace(' ', '_')+".png"))));
        b.setTooltip(new Tooltip(label));

        if(handler != null)
            b.setOnAction(handler);

        return b;
    }
}
