package mangaDowloader;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;




import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.fx.popup.FxPopupShop;

class DownloadableChapter extends Service<Void> {
	final int ID;
	//final int MANGA_ID;
	final String VOLUME;
	final double NUMBER;
	final String URL_STRING;
	final int pagesCount; //number of pages 

	private String title;
	private final ReadOnlyBooleanWrapper dataUpdated = new ReadOnlyBooleanWrapper(this, "database_update", false);

	private final DownloadablePage[] pages;

	private final Path chapSavePath;
	private AtomicReference<DownloadStatus> downloadStatus;

	private volatile String directoryCreateFailedError;

	private DownloadableChapter(ResultSet rs, List<DownloadablePage> pageList) throws SQLException{
		setExecutor(EXECUTOR_SERVICE);

		ID = rs.getInt("id");
		title = rs.getString("title");
		VOLUME = rs.getString("volume");
		//MANGA_ID = rs.getInt("manga_id");
		NUMBER = rs.getDouble("number");
		URL_STRING = rs.getString("url");

		downloadStatus = new AtomicReference<DownloadStatus>(DownloadStatus.valueOf(rs.getString("status")));
		chapSavePath = generateChapterSavePath(rs.getString("manga_id"), String.valueOf(ID)); 

		pages = pageList.stream().sorted(Comparator.comparing(p -> p.ORDER)).toArray(DownloadablePage[]::new);
		pagesCount = pages.length;
	}

	static Path generateChapterSavePath(String manga_id, String chapter_id){
		return DOWNLOAD_DIR.resolve(manga_id).resolve(chapter_id);
	}

	@Override
	protected Task<Void> createTask() {
		return new Task<Void>() {
			@Override
			public Void call() throws IOException {
				if(getDownloadStatus().isCompleted()){
					updateProgress(pagesCount, pagesCount);
					return null;
				}
				String[] files = Files.exists(chapSavePath) ? chapSavePath.toFile().list() : null;

				if(files != null && files.length == pagesCount){
					updateProgress(pagesCount, pagesCount);
					setDownloadStatus(DownloadStatus.COMPLETED);
					return null;
				}
				else if(files == null) {
					try {
						Files.createDirectories(chapSavePath);
					} catch (IOException e) {
						addCreateDirectoryError(e);
						updateProgress(0, pagesCount);
						setDownloadStatus(DownloadStatus.FAILED);
					}
				}

				if(isCancelled())
					return null;

				int[] numbers = files == null ? null : Stream.of(files).mapToInt(Integer::parseInt).sorted().toArray();
				int progress = 0;
				updateProgress(progress, pagesCount);

				for (int i = 0; i < pages.length; i++) {
					DownloadablePage page = pages[i];

					if(isCancelled())
						return null;

					if(page.getDownloadStatus().isCompleted() || (numbers != null && Arrays.binarySearch(numbers, page.ORDER) >= 0)){
						updateProgress(++progress, pagesCount);
						page.setCompleted();
						continue;
					}
					if(page.getDownloadStatus().isHalted())
						continue;

					page.downloadPage(chapSavePath);

					if(page.getDownloadStatus().isCompleted())
						updateProgress(++progress, pagesCount);
				}

				setDownloadStatus(progress == pagesCount ? DownloadStatus.COMPLETED : DownloadStatus.FAILED);
				return null;
			}
		};
	}

	@Override
	public boolean cancel() {
		boolean b = super.cancel();
		if(b && getCompletedCount() != pagesCount)
			setDownloadStatus(DownloadStatus.UNTOUCHED);

		return b;

	}

	private int getCompletedCount() {
		int count = 0;
		for (DownloadablePage p : pages) 
			if(p.getDownloadStatus().isCompleted())
				count++;

		return count;
	}

	public synchronized int[] getCounts() {
		int c = 0, f = 0, h = 0;
		for (DownloadablePage p : pages) {
			DownloadStatus d = p.getDownloadStatus();
			if(d.isCompleted())
				c++;
			else if(d.isFailed())
				f++;
			else if(d.isHalted())
				h++;
		}

		return new int[]{c,f,h};
	}

	private void addCreateDirectoryError(Exception e) {
		try(StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);) {

			pw.print("Failed, to create chapter_dir");
			e.printStackTrace(pw);
			directoryCreateFailedError = sw.toString();
			setDownloadStatus(DownloadStatus.FAILED);
		}
		catch (IOException e2) {}
	}
	public String getDirectoryCreateFailedError() {
		return directoryCreateFailedError;
	}
	public boolean hasError() {
		return directoryCreateFailedError != null || Stream.of(pages).anyMatch(DownloadablePage::hasError);
	}
	/**
	 * this will add {@link #VOLUME} at the end of chapter {@link #title}
	 */
	private void applyVolumePatch() {
		title = title == null  || title.trim().isEmpty() ? VOLUME : VOLUME + " - "+ title ;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append("Chapter[id: ")
				.append(ID)
				.append(", number: ")
				.append(NUMBER)
				.append(", title: ")
				.append(title)
				.append(", volume: ")
				.append(VOLUME)
				.append(", url: ")
				.append(URL_STRING)
				.append(", chapter_savePath: ")
				.append(chapSavePath)
				.append("]")
				.toString();
	}

	private DownloadStatus getDownloadStatus() {
		return downloadStatus.get();
	}

	private void setDownloadStatus(DownloadStatus downloadStatusValue) {
		if(directoryCreateFailedError != null){
			downloadStatus.getAndSet(DownloadStatus.FAILED);
			return;
		}

		if(downloadStatusValue != downloadStatus.get())
			Platform.runLater(() -> dataUpdated.set(true));

		downloadStatus.getAndSet(downloadStatusValue);
	}

	static DownloadableChapterPresenter getChapterPresenter(ResultSet rs, List<DownloadablePage> pageList) throws SQLException{
		DownloadableChapter chapter = new DownloadableChapter(rs, pageList);
		return new DownloadableChapterPresenter(chapter);
	} 

	static class DownloadableChapterPresenter extends VBox {
		private final ProgressBar progressbar;
		private final Text nameText;
		private final CheckBox isSelectedCheckBox;
		private final ReadOnlyBooleanWrapper isSelectedButNotQueued = new ReadOnlyBooleanWrapper(this, "is_selected_but_not_queued", false);

		private final DownloadableChapter chapter;

		private DownloadableChapterPresenter(final DownloadableChapter chapter) {
			super(5);
			this.chapter = chapter;
			getStyleClass().addAll("downloadable-chapter", "v-box");

			nameText = new Text(getChapterDisplayName());
			getChildren().add(nameText);

			progressbar = new ProgressBar(chapter.getCompletedCount()/getPagesCount());
			progressbar.setMaxWidth(Double.MAX_VALUE);
			progressbar.getStyleClass().add("downloadable-chapter");

			isSelectedCheckBox = new CheckBox(getChapterDisplayName());
			isSelectedCheckBox.setOnAction(e -> setSelected(isSelectedCheckBox.isSelected()));

			addStateListener();

			DownloadStatus d = getDownloadStatus();
			if(!d.isUntouched()){
				getChildren().add(progressbar);

				if(!d.isQueued())
					progressbar.getStyleClass().add(d.getClassName());

				if(d.isCompleted())
					setCompleted();
				else {
					setSelected(getDownloadStatus() != DownloadStatus.UNTOUCHED);
					double count = chapter.getCompletedCount();
					if(d.isFailed()){
						progressbar.setProgress(count == 0 ? 1 : count/getPagesCount());
						addErrorPane();
					}
					else if(count > 0)
						progressbar.setProgress(count/getPagesCount());
					else
						getChildren().remove(progressbar);
				}
			}
		}

		private static final String[] removeClassesArray = {DownloadStatus.COMPLETED.getClassName(), DownloadStatus.FAILED.getClassName()};
		private void addStateListener() {
			chapter.stateProperty().addListener((prop, old, _new) -> {
				if(_new == Worker.State.RUNNING){
					if(!getChildren().contains(progressbar))
						getChildren().add(progressbar);
					if(!progressbar.progressProperty().isBound())
						progressbar.progressProperty().bind(chapter.progressProperty());
					if(errorAccordion != null)
						getChildren().remove(errorAccordion);
				}
				else if(_new == State.CANCELLED || _new == State.SUCCEEDED || _new == State.FAILED){
					double completed = chapter.getCompletedCount();
					progressbar.progressProperty().unbind();
					progressbar.setProgress(completed/getPagesCount());
					progressbar.getStyleClass().removeAll(removeClassesArray);

					if(_new != State.CANCELLED)
						progressbar.getStyleClass().add(getDownloadStatus().getClassName());

					if(getDownloadStatus().isFailed()){
						progressbar.setProgress(1);
						progressbar.getStyleClass().add(DownloadStatus.FAILED.getClassName());						
						if(restartButton != null)
							restartButton.setDisable(false);
						addErrorPane();
					}
					else if(getDownloadStatus().isCompleted())
						setCompleted();
				}
			});
		}
		private void setCompleted() {
			isSelectedCheckBox.setIndeterminate(true);
			isSelectedCheckBox.setDisable(true);
			isSelectedButNotQueued.set(false);
			progressbar.progressProperty().unbind();
			progressbar.getStyleClass().removeAll(removeClassesArray);
			progressbar.getStyleClass().add(DownloadStatus.COMPLETED.getClassName());
			progressbar.setProgress(1);
		}

		private TableView<DownloadablePage> errorTable;
		private TitledPane errorTitledPane;
		private Accordion errorAccordion;
		private Button restartButton;
		private Button openDirButton;

		private static final int ICON_SIZE = 30; 
		private static Image REFRESH_ICON = new Image(DownloaderUtils.getImageInputStream("repeat-1.png"), ICON_SIZE, 0, true, true);
		private static Image OPEN_DIR_ICON = new Image(DownloaderUtils.getImageInputStream("folder-11.png"), ICON_SIZE, 0, true, true);
		private static Image COPY_TEXT_ICON = new Image(DownloaderUtils.getImageInputStream("send.png"), ICON_SIZE, 0, true, true);

		private void addErrorPane() {
			if(errorTable == null){
				errorTable = createTable();

				restartButton = new Button(null, new ImageView(REFRESH_ICON));
				restartButton.setTooltip(new Tooltip("Restart Download"));
				restartButton.getStyleClass().add("no-style-button");
				restartButton.setOnAction(e -> restart());

				openDirButton = new Button(null, new ImageView(OPEN_DIR_ICON));
				openDirButton.setTooltip(new Tooltip("Open Chapter Folder"));
				openDirButton.setOnAction(e -> showDocument(chapter.chapSavePath.toUri().toString()));
				openDirButton.getStyleClass().add("no-style-button");

				Button copyButton = new Button(null, new ImageView(COPY_TEXT_ICON));
				copyButton.setTooltip(new Tooltip("Copy all text"));
				copyButton.setOnAction(e -> {
					copyToClipboard(errorTable.getItems()
							.stream()
							.reduce(new StringBuilder(), (sb, p) -> {
								sb.append(p.ID).append('\t')
								.append(p.ORDER).append('\t')
								.append(p.PAGE_URL).append('\t')
								.append(p.IMAGE_URL).append('\n');
								return sb;
							}, StringBuilder::append).toString()
							);
				});
				copyButton.getStyleClass().add("no-style-button");

				HBox buttons = new HBox(10, restartButton, openDirButton, copyButton);
				buttons.setPadding(new Insets(0, 0, 10, 0));

				errorTitledPane = new TitledPane("", new BorderPane(errorTable, buttons, null, null, null));
				errorAccordion = new Accordion(errorTitledPane);
			}

			int[] counts = chapter.getCounts();

			BiFunction<String, Integer, String> cs = (string, i) -> counts[i] == 0 ? "" : string + counts[i];

			errorTable.getItems().clear();
			if(chapter.getDirectoryCreateFailedError() == null)
				Stream.of(chapter.pages).filter(DownloadablePage::hasError).forEach(errorTable.getItems()::add);
			else
				errorTable.setPlaceholder(new Text(chapter.getDirectoryCreateFailedError()));

			errorTitledPane.setText(
					cs.apply("C: ", 0)+
					cs.apply(" | F: ", 1)+
					cs.apply(" | H: ", 2)+
					" | T: "+getPagesCount()
					);
			openDirButton.setDisable(chapter.directoryCreateFailedError != null);

			getChildren().remove(errorAccordion);
			getChildren().add(errorAccordion);
		}

		static void copyToClipboard(String content) {
			ClipboardContent c = new ClipboardContent();
			c.putString(content);
			Clipboard.getSystemClipboard().setContent(c);
			FxPopupShop.showHidePopup("Copied", 1500);
		}

		private TableView<DownloadablePage> createTable() {
			TableView<DownloadablePage> tb = new TableView<>();
			tb.getSelectionModel().setCellSelectionEnabled(true);
			tb.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

			tb.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
				if(e.isControlDown() && e.getCode() == KeyCode.C) {
					@SuppressWarnings("rawtypes")
					List<TablePosition> list = tb.getSelectionModel().getSelectedCells();
					if(list.isEmpty())
						return;
					
					StringBuilder sb = new StringBuilder();
					for (@SuppressWarnings("rawtypes") TablePosition tp : list) {
						DownloadablePage p = tb.getItems().get(tp.getRow());
						
						switch (tp.getColumn()) {
						case 0:
							sb.append(p.ID);
							break;
						case 1:
							sb.append(p.ORDER);
							break;
						case 2:
							sb.append(p.PAGE_URL);
							break;
						case 3:
							sb.append(p.IMAGE_URL);
							break;
						case 4:
							sb.append(p.getError());
							break;
						}
						sb.append('\n');
					}

					copyToClipboard(sb.toString());
					FxPopupShop.showHidePopup("copied", 1500);
				}
			});
			SimpleStringProperty sspImageUrl = new SimpleStringProperty("Image url");
			SimpleStringProperty sspPageUrl = new SimpleStringProperty("Page url");
			
			String[] colNames = {"page_id","order","page url", "Image url", "Error"};
			for (int i = 0; i < colNames.length; i++) {
				TableColumn<DownloadablePage, String> c = new TableColumn<>(colNames[i]);
				switch (i) {
				case 0:
					c.setCellValueFactory(cc -> new SimpleStringProperty(String.valueOf(cc.getValue().ID)));
					break;
				case 1:
					c.setCellValueFactory(cc -> new SimpleStringProperty(String.valueOf(cc.getValue().ORDER)));
					break;
				case 2:
					c.setCellValueFactory(cc -> sspPageUrl);
					break;
				case 3:
					c.setCellValueFactory(cc -> sspImageUrl);
					break;
				case 4:
					c.setCellValueFactory(cc -> new SimpleStringProperty(cc.getValue().getError()));
					break;
				}
				c.setEditable(false);
				tb.getColumns().add(c);
			};
			return tb;
		}
		public void restart(){
			if(!getDownloadStatus().isFailed() || (restartButton != null && restartButton.isDisabled()))
				return;

			chapter.restart();
			progressbar.getStyleClass().removeAll(removeClassesArray);

			if(restartButton != null)
				restartButton.setDisable(true);
		}

		ReadOnlyBooleanProperty dataUpdatedProperty(){
			return chapter.dataUpdated.getReadOnlyProperty();
		}
		boolean isSelected(){
			return !isSelectedCheckBox.isIndeterminate() && isSelectedCheckBox.isSelected();
		}
		ReadOnlyBooleanProperty isSelectedButNotQueuedProperty() {
			return isSelectedButNotQueued.getReadOnlyProperty();
		}
		DownloadStatus getDownloadStatus() {
			return chapter.getDownloadStatus();
		}
		String displayName;
		String getChapterDisplayName() {
			if(displayName != null)
				return displayName;

			String title = getTitle();
			displayName = String.valueOf(chapter.NUMBER).replaceFirst("\\.0$", "")+((title == null || title.trim().isEmpty()) ? "" : " - "+ title);

			return displayName;
		}
		void applyVolumePatch() {
			chapter.applyVolumePatch();
			displayName = null;
			nameText.setText(getChapterDisplayName());
		}
		double getNumber(){
			return chapter.NUMBER;
		}
		int getPagesCount(){
			return chapter.pagesCount;
		}
		String getTitle(){
			return chapter.title;
		}
		boolean hasError(){
			return chapter.hasError();
		}
		void fillErrors(StringBuilder main, StringBuilder id_urls){
			if(chapter.directoryCreateFailedError != null){
				main.append(chapter.directoryCreateFailedError);
				return;
			}

			main
			.append("id: ")
			.append(chapter.ID)
			.append(", number: ")
			.append(chapter.NUMBER)
			.append(", title: ")
			.append(chapter.title)
			.append(", volume: ")
			.append(chapter.VOLUME)
			.append("\nurl: ")
			.append(chapter.URL_STRING)
			.append("\nchapter_savePath: ")
			.append(chapter.chapSavePath)
			.append("\n\n");

			int length = id_urls.length(); 
			Stream.of(chapter.pages)
			.filter(DownloadablePage::hasError)
			.forEach(p -> {
				id_urls.append(p.ID).append('\t')
				.append(p.ORDER).append('\t')
				.append(p.PAGE_URL).append('\t')
				.append(p.IMAGE_URL).append('\t')
				.append(p.getDownloadStatus()).append('-').append(p.getError()).append('\n');

			});
			while(length < id_urls.length()) main.append(id_urls.charAt(length++));
		}		
		void setSelected(boolean b) {
			if(getDownloadStatus() == DownloadStatus.COMPLETED && isSelected()){
				setCompleted();
				return;
			}

			if(!isSelected() && (chapter.isRunning() || chapter.getState() == State.SCHEDULED))
				chapter.cancel();

			isSelectedButNotQueued.set(!isSelectedCheckBox.isIndeterminate() && b);
			if(!isSelectedCheckBox.isIndeterminate())
				isSelectedCheckBox.setSelected(b);
		}

		ReadOnlyObjectProperty<Worker.State> stateProperty(){
			return chapter.stateProperty();
		}		
		State getState() {
			return chapter.getState();
		}
		static final String DUPLICATE_CHAPTER_NAME_RESOLVE_SQL = "UPDATE Chapters SET title = ? WHERE id = ?";
		void commitChapterTitleToDatabase(PreparedStatement ps) throws SQLException {
			ps.setString(1, chapter.title);
			ps.setInt(2, chapter.ID);
			ps.addBatch();
		}

		public void databaseCommit(Statement generalStmnt, PreparedStatement resetChapter, PreparedStatement resetPage) throws SQLException {
			if(!chapter.dataUpdated.get())
				return;
			if(getDownloadStatus().isCompleted()){
				generalStmnt.addBatch("UPDATE Chapters SET status = 'COMPLETED' WHERE manga_id = "+chapter.ID);
				generalStmnt.addBatch("UPDATE Pages SET status = 'COMPLETED', errors = NULL WHERE manga_id = "+chapter.ID);
			}
			else{
				resetChapter.setString(1, getDownloadStatus().toString());
				resetChapter.setInt(2, chapter.ID);
				resetChapter.addBatch();

				for (DownloadablePage p : chapter.pages)
					p.databaseCommit(resetPage);
			}

			chapter.dataUpdated.set(false);
		}

		void cancel() {
			chapter.cancel();
		}

		public void startDownload() {
			if(getDownloadStatus().isCompleted()){
				setCompleted();
				return;
			}
			if(isSelectedButNotQueued.get()){
				isSelectedButNotQueued.set(false);
				chapter.setDownloadStatus(DownloadStatus.QUEUED);
				progressbar.getStyleClass().removeAll(removeClassesArray);

				if(getDownloadStatus().isQueued()){
					if(chapter.getState() == State.READY)
						chapter.start();
					else
						chapter.restart();
				}
			}
		}

		public void fillPageSavePaths(Set<Integer> pageIds, Map<Integer, Path> sink) {
			for (DownloadablePage p : chapter.pages) {
				if(pageIds.contains(p.ID))
					sink.put(p.ID, p.getSavePath(chapter.chapSavePath));
			}
		}

		//this will set completed to the pages whose ID is in successIds 
		public void updateIfHasPages(HashSet<Integer> successIds) {
			boolean update = false;
			for (DownloadablePage p : chapter.pages){ 
				if(successIds.contains(p.ID)){
					p.setCompleted();
					update = true;
				}
			}

			if(update)
				chapter.restart();
		}
	}

	static class  DownloadableChapterCheckBoxListCell extends ListCell<DownloadableChapterPresenter> {

		@Override
		protected void updateItem(DownloadableChapterPresenter item, boolean empty) {
			super.updateItem(item, empty);
			setText(null);

			if(empty)
				setGraphic(null);
			else 
				setGraphic(item.isSelectedCheckBox);
		}
	}


}


