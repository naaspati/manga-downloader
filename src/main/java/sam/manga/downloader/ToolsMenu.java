package sam.manga.downloader;

import java.sql.SQLException;
import java.util.function.Consumer;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import sam.manga.downloader.data.FillNotFoundImages;
import sam.manga.downloader.data.MoveChapterFolders;
import sam.manga.downloader.extra.Utils;
import sam.manga.downloader.manga.MangaPresenter;

public class ToolsMenu extends Menu {
    public ToolsMenu(Consumer<Consumer<MangaPresenter>> forEachMangaPresenter, ReadOnlyBooleanProperty downloadIsActive, Consumer<Boolean> mangarockMaker) {
        super("Tools");
        MenuItem createMangarockDatabaseMi = new MenuItem("Create Mangarock Database");
        createMangarockDatabaseMi.setOnAction(e -> mangarockMaker.accept(false));

        MenuItem clearCompletedDownloadsMi = new MenuItem("Clear Completed Downloads");
        //TODO clearCompletedDownloadsMi.setOnAction(e -> forEachMangaPresenter.accept(m -> m.removeCompletedChaptersFromView));

        MenuItem moveInompletePagesMi = new MenuItem("Move Incomplete Pages");
        moveInompletePagesMi.setOnAction(e -> new MoveIncompletePages());

        MenuItem fillNotFoundImagesMi = new MenuItem("Fill Not Found Images");
        fillNotFoundImagesMi.setOnAction(e -> new FillNotFoundImages());

        MenuItem moveChapterFoldersMi = new MenuItem("Move Chapter Folders");
        moveChapterFoldersMi.setOnAction(e -> {
            try {
                new MoveChapterFolders();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e1) {
                Utils.showError(null, "failed to move", e1, false);
            }
        });

        MenuItem createNmoveMi = new MenuItem("Create and Move");
        createNmoveMi.setOnAction(e -> mangarockMaker.accept(true));
        
        getItems().addAll(clearCompletedDownloadsMi, createNmoveMi,
                moveInompletePagesMi,
                createMangarockDatabaseMi,
                moveChapterFoldersMi);
        
        disableProperty().bind(downloadIsActive);
    }
}
