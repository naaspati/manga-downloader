package sam.manga.downloader.data;

import static sam.config.MyConfig.MANGAROCK_INPUT_DB;
import static sam.fx.alert.FxAlert.showErrorDialog;
import static sam.fx.alert.FxAlert.showMessageDialog;
import static sam.manga.downloader.extra.Utils.path;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import javafx.scene.control.Alert.AlertType;
import sam.manga.downloader.chapter.Chapter;
import sam.sql.sqlite.SQLiteManeger;

public class CreateMangarockDatabase {
    
    public boolean result(){
        DataManager dataManager = DataManager.getInstance();
        
        String header = "Create Mangarock Database";

        if(Files.notExists(path(MANGAROCK_INPUT_DB))){
            showMessageDialog(AlertType.ERROR, "output mangarock.db not found\n"+MANGAROCK_INPUT_DB, header, true);
            return false;
        }
        if(Files.notExists(dataManager.dbFile)){
            showMessageDialog(AlertType.ERROR, "input mangaFox.db not found\n"+dataManager.dbFile, header, true);
            return false;
        }
        try {
            Files.copy(path(MANGAROCK_INPUT_DB), path(MANGAROCK_INPUT_DB).getParent().resolve(path(MANGAROCK_INPUT_DB).getFileName()+"_ "+LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)).replace(':', ' ')), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            showErrorDialog("Error while copying mangarock.db", header, e);
            return false;
        }

        try {
            result1();
        } catch (Exception e) {
            showErrorDialog("sql Error while creating mangarock.db", header, e);
            return false;
        }
        showMessageDialog(null, "mangarock.db created");
        return true;
    }

    public boolean result1() throws SQLException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        Path mip = Paths.get(MANGAROCK_INPUT_DB);
        DataManager dataManager = DataManager.getInstance();

        if(Files.notExists(mip))
            throw new IOException("output mangarock.db not found\n"+MANGAROCK_INPUT_DB);
        if(Files.notExists(dataManager.dbFile))
            throw new IOException("input mangaFox.db not found\n"+dataManager.dbFile);

        Files.copy(mip, mip.resolveSibling(mip.getFileName()+"_ "+LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)).replace(':', ' ')), StandardCopyOption.REPLACE_EXISTING);

        try (SQLiteManeger output = new SQLiteManeger(MANGAROCK_INPUT_DB);
                SQLiteManeger input = new SQLiteManeger(dataManager.dbFile);
                ) {
            output.executeUpdate("DELETE FROM DownloadTask");

            output.prepareStatementBlock(
                    DataManager.qm().insertInto("DownloadTask").placeholders("chapter_name", "dir_name", "chapter_id", "manga_id", "source_id"), 
                    ps -> {
                        input.iterate(DataManager.qm()
                                .select(ChapterMeta.CHAPTER_ID, ChapterMeta.MANGA_ID, ChapterMeta.NUMBER, ChapterMeta.TITLE, ChapterMeta.PAGE_COUNT)
                                .from(ChapterMeta.TABLE_NAME).build(),
                                rs -> {
                                    String chapter_id = rs.getString("id");
                                    String manga_id = rs.getString("manga_id");

                                    ps.setString(1, Chapter.generateChapterName(rs.getDouble("number"), rs.getString("title")));
                                    ps.setString(2, "custom/mangarock/folder/"+chapter_id);
                                    ps.setString(3, chapter_id);
                                    ps.setString(4, manga_id);
                                    ps.setString(5, rs.getString("page_count"));
                                    ps.addBatch();
                                });

                        return ps.executeBatch().length;
                    });

            output.commit();
        }
        return true;
    }
}
