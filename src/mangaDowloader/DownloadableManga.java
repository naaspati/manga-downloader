package mangaDowloader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.effect.Glow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import mangaDowloader.DownloadableChapter.DownloadableChapterPresenter;
import sam.console.ansi.ANSI;
import sam.fx.alert.FxAlert;
import sam.myutils.renamer.RemoveInValidCharFromString;

class DownloadableManga {
	final String NAME;
	final int ID;
	final String URL_STRING;

	private DownloadStatus downloadStatus;

	private final DownloadableChapterPresenter[] chapters;

	private DownloadableManga(ResultSet rs, List<DownloadableChapterPresenter> chapterList) throws SQLException {
		NAME = rs.getString("name");
		ID = rs.getInt("id");
		URL_STRING = rs.getString("url");
		downloadStatus = DownloadStatus.valueOf(rs.getString("status"));

		if(chapterList != null)
		chapters = chapterList.stream().sorted(Comparator.comparing(DownloadableChapterPresenter::getNumber)).toArray(DownloadableChapterPresenter[]::new);
		else {
			FxAlert.showErrorDialog(NAME+"\n"+ID+"\n"+URL_STRING+"\n"+downloadStatus, "Error with manga", null, false);
			chapters = new DownloadableChapterPresenter[0];
		}
	}

	private Stream<DownloadableChapterPresenter> chaptersStream() {
		return Stream.of(chapters);
	}

	static DownloadableMangaPresenter getDownloadableMangaPresenter(ResultSet rs, List<DownloadableChapterPresenter> chapterList, Consumer<DownloadableMangaPresenter> onMangaClick) throws SQLException{
		return new DownloadableMangaPresenter(new DownloadableManga(rs, chapterList), onMangaClick);
	}

	private static VBox clickOwner = null;

	static class DownloadableMangaPresenter {
		private boolean duplicateChapterNameError = false;

		private final VBox downloadingView = new VBox(3);
		private final ScrollPane downloadingViewScrollPane = new ScrollPane(downloadingView);

		private final VBox controlBox = new VBox(5);

		private final ReadOnlyIntegerWrapper completedCount = new ReadOnlyIntegerWrapper(this, "completed", 0); 
		private final ReadOnlyIntegerWrapper failedCount = new ReadOnlyIntegerWrapper(this, "failed", 0);
		private final ReadOnlyIntegerWrapper remainingCount = new ReadOnlyIntegerWrapper(this, "remaining", 0);
		private final ReadOnlyIntegerWrapper queuedCount = new ReadOnlyIntegerWrapper(this, "selected", 0);
		private final ReadOnlyIntegerWrapper selectedCount = new ReadOnlyIntegerWrapper(this, "selected", 0);

		private final ReadOnlyBooleanWrapper isSelectedButNotQueued = new ReadOnlyBooleanWrapper(this, "is_selected_but_not_queued", false);
		private final ReadOnlyBooleanWrapper dataUpdated = new ReadOnlyBooleanWrapper(this, "database_update", false);

		private final DownloadableManga manga;

		private DownloadableMangaPresenter(DownloadableManga manga, Consumer<DownloadableMangaPresenter> onMangaClick) throws SQLException {
			this.manga = manga;
			downloadingViewScrollPane.setFitToWidth(true);

			controlBox.getStyleClass().addAll("downloadable-manga", "manga-control-box");
			controlBox.setEffect(new Glow());
			controlBox.setMaxWidth(Double.MAX_VALUE);

			controlBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
				if(clickOwner != null)
					clickOwner.getStyleClass().remove(MANGA_BOX_CLICKED);

				clickOwner = controlBox;
				clickOwner.getStyleClass().add(MANGA_BOX_CLICKED);

				onMangaClick.accept(this);
				fillDownloadingView();

				e.consume();
			});

			Label mangaNameText = new Label(manga.NAME);
			mangaNameText.getStyleClass().addAll("downloadable-manga", "manga-name-text");
			mangaNameText.setMaxWidth(Double.MAX_VALUE);

			final Text countsText = new Text();
			countsText.getStyleClass().addAll("downloadable-manga", "manga-counts-text");

			bindBindings(mangaNameText, countsText);
			addStateListeners();
			checkDuplicateChapterNames();

			final ReadOnlyBooleanProperty[] array = manga.chaptersStream().map(DownloadableChapterPresenter::dataUpdatedProperty).toArray(ReadOnlyBooleanProperty[]::new);
			dataUpdated.bind(Bindings.createBooleanBinding(() -> Stream.of(array).anyMatch(ReadOnlyBooleanProperty::get), array));
			downloadingView.getChildren().addAll(manga.chapters);
		}
		
		private void bindBindings(Label mangaNameText, Text countsText) {
			//why i chose to updated selectedCount manually ? 
			//binding will unwanted create updates when c + f + r  is same after changes   
			//selectedCount.bind(completedCount.add(failedCount).add(remainingCount));

			StringExpression binding = Bindings.concat(
					Bindings.when(completedCount.isEqualTo(0)).then("").otherwise(Bindings.concat("C: ", completedCount)),
					Bindings.when(failedCount.isEqualTo(0)).then("").otherwise(Bindings.concat("| F: ", failedCount)),
					Bindings.when(remainingCount.isEqualTo(0)).then("").otherwise(Bindings.concat("| R: ", remainingCount)),
					Bindings.when(queuedCount.isEqualTo(0)).then("").otherwise(Bindings.concat("| Q: ", queuedCount)),
					Bindings.when(selectedCount.isEqualTo(0)).then("").otherwise(Bindings.concat("| S: ", selectedCount)),
					"| T: " + manga.chapters.length);
			
			int c = 0, f = 0, r = 0;
			for (DownloadableChapterPresenter d : manga.chapters) {
				DownloadStatus ds = d.getDownloadStatus();
				if(ds.isCompleted())
					c++;
				else if(ds.isFailed())
					f++;
				else if(ds.isQueued())
					r++;
			}
			
			completedCount.set(c);
			failedCount.set(f);
			queuedCount.set(c + f + r);
			remainingCount.set(r);

			countsText.textProperty().bind(binding);

			controlBox.getChildren().addAll(mangaNameText, countsText);

			ReadOnlyBooleanProperty[] booleanArray = Stream.of(manga.chapters).map(DownloadableChapterPresenter::isSelectedButNotQueuedProperty).toArray(ReadOnlyBooleanProperty[]::new);
			BooleanBinding isSelectedBinding = Bindings.createBooleanBinding(() -> Stream.of(booleanArray).anyMatch(ReadOnlyBooleanProperty::get), booleanArray);
			isSelectedButNotQueued.bind(isSelectedBinding);
			IntegerBinding selectedCountBinding = Bindings.createIntegerBinding(() -> (int)Stream.of(booleanArray).filter(ReadOnlyBooleanProperty::get).count(), booleanArray);
			selectedCount.bind(selectedCountBinding);
		}

		public ReadOnlyIntegerProperty selectedCountProperty() {
			return selectedCount.getReadOnlyProperty();
		}

		ReadOnlyBooleanProperty dataUpdatedProperty(){
			return dataUpdated.getReadOnlyProperty();
		}

		private final AtomicInteger cAtomic = new AtomicInteger(0);
		private final AtomicInteger fAtomic = new AtomicInteger(0);
		private final AtomicInteger rAtomic = new AtomicInteger(0);

		private static final String[] removeClassNameArray = {
				"running", 
				"completed",
				"queued",
		"failed"};

		private void addStateListeners() {
			InvalidationListener li = e -> {
				boolean running = false;
				boolean scheduled = false;

				int c = 0, f = 0, r = 0;

				for (DownloadableChapterPresenter dcp : manga.chapters) {
					DownloadStatus ds = dcp.getDownloadStatus();
					State state = dcp.getState();

					if(state == State.RUNNING)
						running = true;
					if(state == State.SCHEDULED)
						scheduled = true;

					if(ds.isCompleted())
						c++;
					else if(ds.isFailed())
						f++;

					if(running || scheduled)
						r++;
				}

				if(running || scheduled || r == 0){
					if(running)
						addClass("running");
					else if(scheduled)
						addClass("queued");
					else if(r == 0)
						addClass(f > 0 ? "failed" : "completed");
				}

				cAtomic.getAndSet(c);
				fAtomic.getAndSet(f);
				rAtomic.getAndSet(r);

				Platform.runLater(() -> {
					int c1 = cAtomic.get();
					int f1 = fAtomic.get();
					int r1 = rAtomic.get();
					int q = c1 + f1 + r1;

					if(completedCount.get() != c1)
						completedCount.set(c1);
					if(failedCount.get() != f1)
						failedCount.set(f1);
					if(remainingCount.get() != r1)
						remainingCount.set(r1);
					if(queuedCount.get() != q)
						queuedCount.set(q);
				});
			};

			manga.chaptersStream().map(DownloadableChapterPresenter::stateProperty).forEach(s -> s.addListener(li));
		}

		private void addClass(String className) {
			if(!controlBox.getStyleClass().contains(className)){
				controlBox.getStyleClass().removeAll(removeClassNameArray);
				controlBox.getStyleClass().add(className);
			}
		}

		private void fillDownloadingView() {
			ObservableList<Node> list = downloadingView.getChildren();
			list.clear();

			for (DownloadableChapterPresenter d : manga.chapters){ 
				if(d.getDownloadStatus().isQueued())
					list.add(d);
			}
			for (DownloadableChapterPresenter d : manga.chapters){ 
				if(d.getDownloadStatus().isFailed())
					list.add(d);
			}
			for (DownloadableChapterPresenter d : manga.chapters){ 
				if(d.getDownloadStatus().isCompleted()){
					list.remove(d);
					list.add(d);
				}
			}
		}

		/**
		 * 
		 * @return true if manga has at-least one active chapter download
		 */
		boolean isDownloading(){
			return controlBox.getStyleClass().contains("running");	
		}

		/**
		 * will create chapter name as MangaRock Converter will do for a chapter 
		 * <br>
		 * purpose of this is to check, if two or more chapter does not have same name after create Names 
		 * 
		 * @param chapterNumber
		 * @param chapterTitle
		 * @return
		 */
		static String generateChapterName(String chapterNumber, String chapterTitle){
			if(chapterNumber == null || chapterNumber.trim().isEmpty()){
				FxAlert.showErrorDialog(DownloaderApp.getPrimaryStage(), "Failed to create chapter name", "generateChapterName Error", "chapterNumber: "+chapterNumber+"\nchapterTitle: "+chapterTitle);
				return "createChapterName failed - "+System.currentTimeMillis()+" - chapterNumber = "+chapterNumber; 
			}

			return chapterNumber.replaceFirst("\\.0$", "").trim()+((chapterTitle == null || chapterTitle.trim().isEmpty() || chapterTitle.trim().equals("null"))? "": " "+chapterTitle);
		}

		/**
		 * this check should be performed before add to selection listView so the name change can be reflected in List
		 */
		private void checkDuplicateChapterNames() {

			//checking same file chapter_name with different chapter id (this happens when two different volumes have same chapter_name)
			//first check
			Map<String, List<DownloadableChapterPresenter>> map = Stream.of(manga.chapters).collect(Collectors.groupingBy(c -> generateChapterName(String.valueOf(c.getNumber()), c.getTitle())));

			if(map.values().stream().anyMatch(l -> l.size() > 1)){
				StringBuilder b1 = new StringBuilder("Double chapter name Error\r\n\r\n");

				map.forEach((s,t) -> {
					if(t.size() < 2)
						return;

					b1.append("\t").append(s).append("\r\n");
					t.forEach(c -> b1.append("\t\t").append(c.toString()).append("\r\n"));
				});

				//volume patch up
				manga.chaptersStream().forEach(DownloadableChapterPresenter::applyVolumePatch);

				//second check
				map = manga.chaptersStream().collect(Collectors.groupingBy(c -> generateChapterName(String.valueOf(c.getNumber()), c.getTitle())));

				String result = map.values().stream().anyMatch(l -> l.size() > 1) ? "FAILED": "SUCESS"; 
				b1.append("\r\n").append("Volume Patch: "+result);

				duplicateChapterNameError = true;

				Alert alert = new Alert(AlertType.WARNING, "Volume Patch: "+result, ButtonType.OK);
				alert.setHeaderText("Duplicate Chapter Name Error");
				alert.initOwner(DownloaderApp.getPrimaryStage());
				alert.initModality(Modality.NONE);

				alert.getDialogPane().setExpandableContent(new TextArea(b1.toString()));
				alert.show();
			}
		}

		public boolean hasDuplicateChapterNameError() {
			return duplicateChapterNameError;
		}

		static final String DUPLICATE_CHAPTER_NAME_RESOLVE_SQL = DownloadableChapterPresenter.DUPLICATE_CHAPTER_NAME_RESOLVE_SQL;  
		void resolveDuplicateChapterNameError(PreparedStatement ps) throws SQLException {
			for (DownloadableChapterPresenter d : manga.chapters) 
				d.commitChapterTitleToDatabase(ps);
		}

		private static final String MANGA_BOX_CLICKED = "manga-box-clicked";

		String getMangaUrl() {
			return manga.URL_STRING;
		}

		int getMangaId() {
			return manga.ID;
		}
		String getMangaName() {
			return manga.NAME;
		}

		String getMangaPath() {
			return DownloaderApp.DOWNLOAD_DIR.resolve(String.valueOf(manga.ID)).toString();
		}

		ReadOnlyIntegerProperty completedCountProperty(){
			return completedCount.getReadOnlyProperty();
		}

		ReadOnlyIntegerProperty failedCountProperty(){
			return failedCount.getReadOnlyProperty();
		}

		ReadOnlyIntegerProperty remainingCountProperty(){
			return remainingCount.getReadOnlyProperty();
		}

		ReadOnlyIntegerProperty queuedCountProperty(){
			return queuedCount.getReadOnlyProperty();
		}

		void fillChapterPresentersView(ObservableList<DownloadableChapterPresenter> list) {
			list.setAll(manga.chapters);
		}

		void cancelAllDownload() {
			for (DownloadableChapterPresenter d : manga.chapters) d.cancel();
		}

		void setAllChapterSelected(boolean b) {
			for (DownloadableChapterPresenter c : manga.chapters) c.setSelected(b);
		}

		void selectMissing(TreeMap<Double, String> chapters, TextArea logArea) {
			StringBuilder builder = new StringBuilder();
			ANSI.yellow(builder, manga.ID).append('\t');
			ANSI.yellow(builder, manga.NAME).append('\n');
			ArrayList<Double> foundNumbers = new ArrayList<>();

			for (DownloadableChapterPresenter c : manga.chapters) {
				String name = chapters.get(c.getNumber()); 
				if(name != null){
					c.setSelected(true);
					builder.append("    ")
					.append(c.getNumber())
					.append("   ")
					.append(name).append('\n');
					foundNumbers.add(c.getNumber());
				}
			}

			if(!foundNumbers.isEmpty()){
				logArea.appendText(builder.toString());
				chapters.keySet().removeAll(foundNumbers);
			}
		}

		void saveRecords(){
			Path path  = DownloaderApp.LOGS_FOLDER.resolve(RemoveInValidCharFromString.removeInvalidCharsFromFileName(manga.NAME)+".npp");

			if(!manga.chaptersStream().anyMatch(DownloadableChapterPresenter::hasError)){
				try {
					Files.createDirectories(path.getParent());
					Files.deleteIfExists(path);
				} catch (IOException e) {
					FxAlert.showErrorDialog("failed to delete\nFile: "+path, "Manga.saveRecords() Error", e, false);
				}
				return;
			}

			StringBuilder main = new StringBuilder()
					.append(manga.ID).append('\n')
					.append(manga.NAME).append('\n')
					.append(manga.URL_STRING).append('\n')
					.append("\n")
					.append(manga.ID).append(" \n");
			

			StringBuilder id_urls = new StringBuilder();

			manga.chaptersStream().filter(DownloadableChapterPresenter::hasError).peek(c -> {
				main.deleteCharAt(main.length() - 1);
				main.append(c.getNumber()).append(" \n");
			}).forEach(c -> c.fillErrors(main, id_urls));

			main.append("\n\n------------------------------------------\n\n").append(id_urls);

			try {
				Files.write(path, main.toString().getBytes("utf-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			} catch (IOException e) {}
		}

		void databaseCommit(Statement generalStmnt, PreparedStatement resetChapter, PreparedStatement resetPage) throws SQLException {
			if(!dataUpdated.get())
				return;

			if(completedCount.get() == getChaptersCount()){
				generalStmnt.addBatch("UPDATE Mangas SET status = 'COMPLETED' WHERE id = "+manga.ID);
				generalStmnt.addBatch("UPDATE Chapters SET status = 'COMPLETED' WHERE manga_id = "+manga.ID);
				generalStmnt.addBatch("UPDATE Pages SET status = 'COMPLETED', errors = NULL WHERE manga_id = "+manga.ID);
			}
			else {
				DownloadStatus status = null;
				if(remainingCount.get() > 1)
					status = DownloadStatus.QUEUED;
				else if(failedCount.get() > 1)
					status = DownloadStatus.FAILED;

				if(status != null)
					generalStmnt.addBatch("UPDATE Mangas SET status = '"+status+"' WHERE id = "+manga.ID);

				for (DownloadableChapterPresenter c : manga.chapters) 
					c.databaseCommit(generalStmnt, resetChapter, resetPage);					
			}
		}

		void selectRangeChapters() {
			FxAlert.showMessageDialog("Not implemented", "No Code", false);
		}

		DownloadStatus getDownloadStatus() {
			return manga.downloadStatus;
		}

		ReadOnlyBooleanProperty isSelectedButNotQueuedProperty() {
			return isSelectedButNotQueued.getReadOnlyProperty();
		}

		boolean isSelected() {
			return isSelectedButNotQueued.get();
		}

		void startDownload() {
			if(!isSelected())
				return;

			for (DownloadableChapterPresenter d : manga.chapters) d.startDownload();
			queuedCount.set((int)Stream.of(manga.chapters).filter(c -> c.getDownloadStatus().isQueued()).count());

			fillDownloadingView();
		}

		int getChaptersCount() {
			return manga.chapters.length;
		}

		int getPagesCount() {
			return manga.chaptersStream().mapToInt(DownloadableChapterPresenter::getPagesCount).sum();
		}

		Node getDownloadingView() {
			return downloadingViewScrollPane;
		}

		Node getControlBox() {
			return controlBox;
		}

		boolean hasClick(){
			return controlBox == clickOwner;
		}

		public boolean isAllChaptersSelected() {
			return Stream.of(manga.chapters).allMatch(DownloadableChapterPresenter::isSelected);
		}

		/**
		 * 
		 * @param pageIds -> set contaning DownloadablePage.ID
		 * @param sink  -> map in which DownloadablePage.ID -> DownloadablePage.savePath will be put
		 */
		public void fillPageSavePaths(Set<Integer> pageIds, Map<Integer, Path> sink) {
			manga.chaptersStream().forEach(c -> c.fillPageSavePaths(pageIds, sink));
		}

		public void updateIfHasPages(HashSet<Integer> successIds) {
			for (DownloadableChapterPresenter d : manga.chapters) 
				d.updateIfHasPages(successIds);
		}

		public void removeCompletedChaptersFromView() {
			downloadingView.getChildren().removeIf(d -> ((DownloadableChapterPresenter)d).getDownloadStatus().isCompleted());
		}
	
		public void retryFailedChapters(){
			if(failedCount.get() == 0)
				return;
			
			for (DownloadableChapterPresenter c : manga.chapters) c.restart();
		}
		
	}
}

