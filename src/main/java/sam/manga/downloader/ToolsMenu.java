package sam.manga.downloader;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import sam.manga.downloader.data.FillNotFoundImages;
import sam.manga.downloader.data.MoveChapterFolders;
import sam.manga.downloader.extra.Utils;
import sam.manga.downloader.manga.MangaPresenter;

public class ToolsMenu extends Menu {
    public ToolsMenu(List<MangaPresenter> mangas, ReadOnlyBooleanProperty downloadIsActive, Consumer<Boolean> mangarockMaker) {
        super("Tools");
        MenuItem createMangarockDatabaseMi = menuitem("Create Mangarock Database", e -> mangarockMaker.accept(false));
        MenuItem clearCompletedDownloadsMi = menuitem("Clear Completed Downloads", null); // e -> forEachMangaPresenter.accept(m -> m.removeCompletedChaptersFromView)
        MenuItem moveInompletePagesMi = menuitem("Move Incomplete Pages", e -> new MoveIncompletePages(mangas));
        MenuItem fillNotFoundImagesMi = menuitem("Fill Not Found Images", e -> new FillNotFoundImages());
        MenuItem moveChapterFoldersMi = menuitem("Move Chapter Folders", e -> moveChapters());

        MenuItem createNmoveMi = menuitem("Create and Move", e -> mangarockMaker.accept(true));
        
        getItems().addAll(
                clearCompletedDownloadsMi, 
                createNmoveMi,
                moveInompletePagesMi,
                createMangarockDatabaseMi,
                moveChapterFoldersMi);
        
        disableProperty().bind(downloadIsActive);
    }

    private void moveChapters() {
            try {
                new MoveChapterFolders();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e1) {
                Utils.showError(null, "failed to move", e1, false);
            }
    }

    private MenuItem menuitem(String string, EventHandler<ActionEvent> action) {
        MenuItem item = new MenuItem(string);
        if(action != null) item.setOnAction(action);
        return item;
    }
}
