package mangaDowloader;

import static sam.fx.alert.FxAlert.showErrorDialog;
import static sam.fx.alert.FxAlert.showMessageDialog;
import static sam.fx.popup.FxPopupShop.showHidePopup;
import static mangaDowloader.DownloaderApp.*; 

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.sqlite.JDBC;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import mangaDowloader.DownloadableManga.DownloadableMangaPresenter;
import sam.fx.alert.FxAlert;
//
public interface DownloaderUtils {
	public static Object[] getLoggerStage(boolean addProgressBar){
		Stage stage = new Stage(StageStyle.UTILITY);
		stage.initOwner(DownloaderApp.getPrimaryStage());
		stage.initModality(Modality.WINDOW_MODAL);

		TextArea logText = new TextArea();
		ProgressBar bar = null;

		if(addProgressBar){
			bar = new ProgressBar(0);
			bar.setMaxWidth(Double.MAX_VALUE);
			stage.setScene(new Scene(new BorderPane(logText, null, null, bar, null), 500, 500));
		}
		else
			stage.setScene(new Scene(logText, 500, 500));

		stage.show();

		return new Object[]{stage, logText, bar};
	}

		/**
	 * this method converts mangafox.db or mangaHere.db to mangarock.db
	 * @param file 
	 */
	static boolean createMangarockDatabase(File dbFile){
		Path mangafox = dbFile.toPath();

		String header = "Create Mangarock Database";

		if(Files.notExists(MANGAROCK_INPUT_DB)){
			FxAlert.showMessageDialog(DownloaderApp.getPrimaryStage(), AlertType.ERROR, "output mangarock.db not found\n"+MANGAROCK_INPUT_DB, header, true);
			return false;
		}
		if(Files.notExists(mangafox)){
			FxAlert.showMessageDialog(DownloaderApp.getPrimaryStage(), AlertType.ERROR, "input mangaFox.db not found\n"+mangafox, header, true);
			return false;
		}
		try {
			Files.copy(MANGAROCK_INPUT_DB, MANGAROCK_INPUT_DB.getParent().resolve(MANGAROCK_INPUT_DB.getFileName()+"_ "+LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)).replace(':', ' ')), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			showErrorDialog("Error while copying mangarock.db", header, e);
			return false;
		}

		try (Connection cMangafox = DriverManager.getConnection(JDBC.PREFIX+dbFile);
				Connection cMangarock = DriverManager.getConnection(JDBC.PREFIX+MANGAROCK_INPUT_DB);
				PreparedStatement p_mangarock = cMangarock.prepareStatement("INSERT INTO DownloadTask(chapter_name, dir_name, chapter_id, manga_id, source_id) VALUES(?,?,?,?,?)");
				) {
			cMangafox.setAutoCommit(false);
			cMangarock.setAutoCommit(false);

			Statement stmntTemp = cMangarock.createStatement();
			stmntTemp.executeUpdate("DELETE FROM DownloadTask");
			stmntTemp.close();

			stmntTemp = cMangafox.createStatement();
			ResultSet rsChaps = stmntTemp.executeQuery("SELECT id, manga_id, number, title, page_count FROM Chapters");

			while(rsChaps.next()){
				String chapter_id = rsChaps.getString("id");
				String manga_id = rsChaps.getString("manga_id");

				p_mangarock.setString(1, DownloadableMangaPresenter.generateChapterName(rsChaps.getString("number"), rsChaps.getString("title")));
				p_mangarock.setString(2, "custom/mangarock/folder/"+chapter_id);
				p_mangarock.setString(3, chapter_id);
				p_mangarock.setString(4, manga_id);
				p_mangarock.setString(5, rsChaps.getString("page_count"));
				p_mangarock.addBatch();
			}

			stmntTemp.close();
			rsChaps.close();

			p_mangarock.executeBatch();
			cMangarock.commit();
		}
		catch (SQLException|NumberFormatException e) {
			showErrorDialog("sql Error while creating mangarock.db", header, e);
			return false;
		}
		showMessageDialog(null, "mangarock.db created");
		return true;
	}

	/** 
	 * this will create an image for those pages which failed to download
	 */
	static void fillNotFoundImages(){
		InputStream inputStream = DownloaderUtils.getImageInputStream("not_found.jpg");
		Path tempDir = DownloaderApp.HALTED_IMAGE_DIR;

		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.initOwner(DownloaderApp.getPrimaryStage());

		alert.setHeaderText("Enter id(s)");

		TextArea input = new TextArea();
		input.setPrefColumnCount(10);
		input.setPrefRowCount(10);
		alert.getDialogPane().setContent(new VBox(10, new Text("Enter ID(s) of page(s) separated by non-numeric charactor"), input));

		Optional<ButtonType> result = alert.showAndWait();
		if(!result.isPresent() || result.get() != ButtonType.OK){
			showHidePopup("Cancelled", 1500);
			return;
		}

		String text = input.getText();
		int[] ids = Stream.of(text.split("\\D+"))
				.filter(s -> s.trim().matches("\\d+"))
				.mapToInt(s -> Integer.parseInt(s.trim()))
				.toArray();

		if(ids.length == 0){
			FxAlert.showMessageDialog(DownloaderApp.getPrimaryStage(), AlertType.ERROR, "Empty Input", "Fill Not Found Images Error", true);
			return;
		}
		else {
			Object[] logger = getLoggerStage(false);
			Stage stage2 = (Stage)logger[0];
			stage2.setOnCloseRequest(e -> e.consume());
			TextArea logText = (TextArea)logger[1];
			stage2.setTitle("Fill not found Images");
			
			int sucess = 0, total = 0;
			for (int i : ids) {
				Path out = tempDir.resolve(i+".jpg");
				try {
					total++;
					if(Files.exists(out))
						logText.appendText("alreay exits: "+out+"\n");
					else
						Files.copy(inputStream, out);
					sucess++;
				} catch (IOException e1) {
					logText.appendText("Failed copy: "+out+"\n");
				}	
			}
			showMessageDialog("Total: "+total+"\nSuccess: "+sucess+"\nFailed: "+(total- sucess), "Fill Not Found Images Completed");
			stage2.setOnCloseRequest(null);
		}
	}

	/**
	 * this will move all chapter folder in manga folder of {@link #DOWNLOAD_DIR} which are listed in <b>mangarock.db</b> created by ->  {@link #createMangarockDatabase()}, and are completely downloaded (i.e. number of pages in folder == number of pages listed in {@link #DownloaderApp.DATABASE_PATH} -> Pages table to this chapter)
	 */
	static void moveChapterFolders(){
		if(Files.notExists(MANGAROCK_INPUT_DB)){
			FxAlert.showMessageDialog(DownloaderApp.getPrimaryStage(), AlertType.ERROR, "Mangarock database not found\n"+MANGAROCK_INPUT_DB, "Move Chapter Folders Failed", true);
			return;
		}

		if(Files.notExists(DownloaderApp.DOWNLOAD_DIR)){
			FxAlert.showMessageDialog(DownloaderApp.getPrimaryStage(), AlertType.ERROR, DownloaderApp.DOWNLOAD_DIR.getFileName() +" not found\n"+DownloaderApp.DOWNLOAD_DIR, "Move Chapter Folders Failed", true);
			return;
		}

		if(DownloaderApp.DOWNLOAD_DIR.toFile().list().length == 0){
			FxAlert.showMessageDialog(DownloaderApp.getPrimaryStage(), AlertType.ERROR, DownloaderApp.DOWNLOAD_DIR.getFileName() +" is empty\n"+DownloaderApp.DOWNLOAD_DIR, "Move Chapter Folders Failed", true);
			return;
		}

		List<File> chapterList = Stream.of(DownloaderApp.DOWNLOAD_DIR.toFile().listFiles(File::isDirectory)).flatMap(f -> Arrays.stream(f.listFiles(File::isDirectory))).collect(Collectors.toList());

		if(chapterList.isEmpty()){
			FxAlert.showMessageDialog(DownloaderApp.getPrimaryStage(), AlertType.ERROR, "No chpater folder found in "+DownloaderApp.DOWNLOAD_DIR.getFileName() +"\n"+DownloaderApp.DOWNLOAD_DIR, "Move Chapter Folders Failed", true);
			return;
		}
		
		try {
			Files.createDirectories(MANGAROCK_INPUT_FOLDER);
		} catch (IOException e) {
			showErrorDialog("Error while creating folder\n"+MANGAROCK_INPUT_FOLDER, "Move Chapter Folders Failed", e, false);
			return;
		}
		
		Object[] logger = getLoggerStage(false);
		Stage stage2 = (Stage)logger[0];
		stage2.setOnCloseRequest(e -> e.consume());
		TextArea logText = (TextArea)logger[1];

		logText.appendText("found chapter folders: "+chapterList.size()+"\n\n");

		//MangaFox.Chapters.page_count = MangaRock.DownloadTask.source_id
		try (Connection c = DriverManager.getConnection(JDBC.PREFIX+MANGAROCK_INPUT_DB);
				Statement s1 = c.createStatement();
				ResultSet rs = s1.executeQuery("SELECT chapter_id, manga_id, source_id FROM DownloadTask WHERE chapter_id IN"+chapterList.stream().map(File::getName).collect(Collectors.joining(",", "(", ")")));
				) {

			String haltedText = "Halted";
			String failedText =   "Failed";

			int success = 0, failed = 0, halted = 0;
			logText.appendText("S : files count should be\nF : files count found\n\n");

			String format = "%-20s%-12s%-15s%s\n";

			logText.appendText(String.format(format, haltedText.replace("Halted", "Result"), "manga_id", "chapter_id", "Reason")+"\n\n");

			while(rs.next()){
				String chapter_id = rs.getString("chapter_id");
				String manga_id = rs.getString("manga_id");
				int page_count = rs.getInt("source_id");
				Path src = DownloadableChapter.generateChapterSavePath(manga_id, chapter_id);

				if(Files.notExists(src))
					continue;

				String[] fileNames = src.toFile().list(); 
				
				int foundCount = fileNames.length;

				if(foundCount == page_count){
					try {
						Path target = MANGAROCK_INPUT_FOLDER.resolve(chapter_id);
						if(Files.notExists(target)){
							Files.move(src, target, StandardCopyOption.REPLACE_EXISTING);
							success++;
						}
						else{
							logText.appendText(String.format(format, haltedText, manga_id, chapter_id, "target already exits"));
							halted++;
						}
					} catch (IOException e) {
						logText.appendText(String.format(format, failedText, manga_id, chapter_id, e+""));
						failed++;
					}
				}
				else{
					Arrays.sort(fileNames);
					String missings = IntStream.range(0, page_count).mapToObj(String::valueOf).filter(s -> Arrays.binarySearch(fileNames, s) < 0).collect(Collectors.joining(", ", "  ( missing: ", " )"));
					logText.appendText(String.format(format, haltedText, manga_id, chapter_id, "S: "+(page_count < 10 ?"0":"")+page_count+" | F: "+(foundCount < 10 ?"0":"")+foundCount+missings));
					halted++;
				}
			}
			logText.appendText("success: "+success+"\r\nFailed: "+failed+"\r\nhalted: "+halted);
		}

		catch (SQLException e) {
			showErrorDialog("Error while moveChapterFolders", "Move Chapter Folders Failed", e);
			return;
		}

		Stream.of(DownloaderApp.DOWNLOAD_DIR.toFile().listFiles()).forEach(f -> f.delete());
		DownloaderApp.HALTED_IMAGE_DIR.toFile().delete();
		DownloaderApp.DOWNLOAD_DIR.toFile().delete();
		
		stage2.setOnCloseRequest(null);
		showMessageDialog(null, "Move Chapter Folders Completed");
	}

	static int calculateChapterId(String mangaId, String volume, double chapter_number, String chapter_title) {
		return new StringBuilder()
				.append(mangaId)
				.append(volume)
				.append(chapter_number)
				.append(chapter_title).hashCode();
	}
	public static InputStream getImageInputStream(String imageName) {
		return ClassLoader.getSystemResourceAsStream("imgs/"+imageName);
	}

}
