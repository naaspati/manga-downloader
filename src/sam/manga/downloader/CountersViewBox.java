package sam.manga.downloader;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.scene.effect.Glow;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class CountersViewBox extends VBox {
	private static final Font font = Font.loadFont(ClassLoader.getSystemResourceAsStream("fonts/Nova_Mono/NovaMono.ttf"), 12);
	
	public CountersViewBox(
			ReadOnlyIntegerProperty mangasCount, 
			ReadOnlyIntegerProperty chapterCount, 
			ReadOnlyIntegerProperty pagesCount,

			ReadOnlyIntegerProperty selectedCount,
			ReadOnlyIntegerProperty queuedCount,
			ReadOnlyIntegerProperty completedCount,
			ReadOnlyIntegerProperty failedCount, 
			ReadOnlyIntegerProperty remainingCount
			) {
		super(5);
		getStyleClass().add("info-panel-box");

		Text l2 = new Text("Session Data...");
		l2.getStyleClass().add("info-panel-header");
		l2.setEffect(new Glow());

		Text sessionDataText = new Text();
		sessionDataText.textProperty().bind(Bindings.concat(
				"Manga Count   :  ", mangasCount,
				"\nChapter Count :  ", chapterCount,
				"\nPage Count    :  ", pagesCount
				));
		sessionDataText.setId("session-data-text");
		sessionDataText.setFont(font);
		
		getChildren().addAll(l2, sessionDataText, new Text(" "));
		
		Text selectedText = new Text();
		selectedText.setId("selected-count-text");
		selectedText.setFont(font);
		
		Text queuedText = new Text();
		queuedText.setId("queued-count-text");
		queuedText.setFont(font);
		
		Text completedText = new Text();
		completedText.setId("completed-count-text");
		completedText.setFont(font);
		
		Text failedText = new Text();
		failedText.setId("failed-count-text");
		failedText.setFont(font);
		
		Text remainingText = new Text();
		remainingText.setId("remaining-count-text");
		remainingText.setFont(font);
		
		l2 = new Text("Progress...");
		l2.getStyleClass().add("info-panel-header");
		l2.setEffect(new Glow());
		
		selectedText.textProperty().bind(Bindings.concat( "Selected  : ", selectedCount));
		queuedText.textProperty().bind(Bindings.concat(   "Queued    : ", queuedCount));
		completedText.textProperty().bind(Bindings.concat("Completed : ", completedCount));
		failedText.textProperty().bind(Bindings.concat(   "Failed    : ", failedCount));
		remainingText.textProperty().bind(Bindings.concat("Remaining : ", remainingCount));
		
		getChildren().addAll(l2,
				selectedText,				
				queuedText,
				remainingText,
				completedText,
				failedText
				);
		
	}
}
