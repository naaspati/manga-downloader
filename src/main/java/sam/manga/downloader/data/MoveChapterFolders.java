package sam.manga.downloader.data;

import static sam.config.MyConfig.MANGAROCK_INPUT_DB;
import static sam.config.MyConfig.MANGAROCK_INPUT_DIR;
import static sam.fx.alert.FxAlert.showErrorDialog;
import static sam.fx.alert.FxAlert.showMessageDialog;
import static sam.manga.downloader.extra.Utils.DOWNLOAD_DIR;
import static sam.manga.downloader.extra.Utils.HALTED_IMAGE_DIR;
import static sam.manga.downloader.extra.Utils.stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javafx.scene.control.Alert.AlertType;
import sam.fx.alert.FxAlert;
import sam.manga.downloader.TextStage;
import sam.manga.downloader.chapter.Chapter;
import sam.sql.sqlite.SQLiteManeger;

public class MoveChapterFolders {
    
    private final String haltedText = "Halted";
    private final String failedText =   "Failed";
    
    private int success = 0, failed = 0, halted = 0;
    private final String format = "%-20s%-12s%-15s%s\n";

    /**
     * this will move all chapter folder in manga folder of {@link #DOWNLOAD_DIR} which are listed in <b>mangarock.db</b> created by ->  {@link #createMangarockDatabase()}, and are completely downloaded (i.e. number of pages in folder == number of pages listed in {@link #DATABASE_FILE} -> Pages table to this chapter)
     * @throws SQLException 
     * @throws ClassNotFoundException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    public MoveChapterFolders() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
        if(Files.notExists(Paths.get(MANGAROCK_INPUT_DB))){
            FxAlert.showMessageDialog(stage(), AlertType.ERROR, "Mangarock database not found\n"+MANGAROCK_INPUT_DB, "Move Chapter Folders Failed", true);
            return;
        }

        if(Files.notExists(DOWNLOAD_DIR)){
            FxAlert.showMessageDialog(stage(), AlertType.ERROR, DOWNLOAD_DIR.getFileName() +" not found\n"+DOWNLOAD_DIR, "Move Chapter Folders Failed", true);
            return;
        }

        if(DOWNLOAD_DIR.toFile().list().length == 0){
            FxAlert.showMessageDialog(stage(), AlertType.ERROR, DOWNLOAD_DIR.getFileName() +" is empty\n"+DOWNLOAD_DIR, "Move Chapter Folders Failed", true);
            return;
        }

        List<File> chapterList = Stream.of(DOWNLOAD_DIR.toFile().listFiles(File::isDirectory)).flatMap(f -> Arrays.stream(f.listFiles(File::isDirectory))).collect(Collectors.toList());

        if(chapterList.isEmpty()){
            FxAlert.showMessageDialog(stage(), AlertType.ERROR, "No chpater folder found in "+DOWNLOAD_DIR.getFileName() +"\n"+DOWNLOAD_DIR, "Move Chapter Folders Failed", true);
            return;
        }
        
        try {
            Files.createDirectories(Paths.get(MANGAROCK_INPUT_DIR));
        } catch (IOException e) {
            showErrorDialog("Error while creating folder\n"+MANGAROCK_INPUT_DIR, "Move Chapter Folders Failed", e, false);
            return;
        }
        
            TextStage logger = TextStage.open();
            logger.appendText("found chapter folders: "+chapterList.size()+"\n\n");

            //MangaFox.Chapters.page_count = MangaRock.DownloadTask.source_id
            try (SQLiteManeger c = new SQLiteManeger(MANGAROCK_INPUT_DB);) {
                logger.appendText("S : files count should be\nF : files count found\n\n");
                logger.appendText(String.format(format, haltedText.replace("Halted", "Result"), "manga_id", "chapter_id", "Reason")+"\n\n");
                
                c.iterate("SELECT chapter_id, manga_id, source_id FROM DownloadTask WHERE chapter_id IN"+chapterList.stream().map(File::getName).collect(Collectors.joining(",", "(", ")")), rs -> next(rs, logger));
                
                logger.appendText("success: "+success+"\r\nFailed: "+failed+"\r\nhalted: "+halted);
            }

            Stream.of(DOWNLOAD_DIR.toFile().listFiles()).forEach(f -> f.delete());
            HALTED_IMAGE_DIR.toFile().delete();
            DOWNLOAD_DIR.toFile().delete();
            
            logger.close();
            showMessageDialog(null, "Move Chapter Folders Completed");
        

    }
    
    private void next(ResultSet rs, TextStage logger) throws SQLException {
        int chapter_id = rs.getInt("chapter_id");
        int manga_id = rs.getInt("manga_id");
        int page_count = rs.getInt("source_id");
        Path src = Chapter.generateChapterSavePath(manga_id, chapter_id);

        if(Files.notExists(src))
            return;

        String[] fileNames = src.toFile().list(); 
        
        int foundCount = fileNames.length;

        if(foundCount == page_count){
            try {
                Path target = Paths.get(MANGAROCK_INPUT_DIR, String.valueOf(chapter_id));
                if(Files.notExists(target)){
                    Files.move(src, target, StandardCopyOption.REPLACE_EXISTING);
                    success++;
                }
                else{
                    logger.appendText(String.format(format, haltedText, manga_id, chapter_id, "target already exits"));
                    halted++;
                }
            } catch (IOException e) {
                logger.appendText(String.format(format, failedText, manga_id, chapter_id, e+""));
                failed++;
            }
        }
        else{
            Arrays.sort(fileNames);
            String missings = IntStream.range(0, page_count).mapToObj(String::valueOf).filter(s -> Arrays.binarySearch(fileNames, s) < 0).collect(Collectors.joining(", ", "  ( missing: ", " )"));
            logger.appendText(String.format(format, haltedText, manga_id, chapter_id, "S: "+(page_count < 10 ?"0":"")+page_count+" | F: "+(foundCount < 10 ?"0":"")+foundCount+missings));
            halted++;
        }
    }
}
