package sam.manga.downloader.chapter;

import javafx.scene.control.CheckBox;
import javafx.scene.control.cell.CheckBoxListCell;

public class  ChapterSelectionListCell extends CheckBoxListCell<ChapterPresenter> {
    private CheckBox box;
    private ChapterPresenter chapter;

    @Override
    public void updateItem(ChapterPresenter item, boolean empty) {
        super.updateItem(item, empty);
        setText(null);
        setGraphic(null);
        this.chapter = item;
        
        if(item == null || empty) {
            setText(null);
            setGraphic(null);
            return;
        }
        if(box == null) {
            box = new CheckBox();
            box.setOnAction(e -> chapter.setSelected(box.isSelected()));
        }
        
        if(chapter.getSelected() == ChapterPresenter.NOT_SELECTABLE) {
            box.setIndeterminate(true);
            box.setDisable(true); 
        } else 
            box.setSelected(chapter.isSelected()); 

        box.setGraphicTextGap(10);
        box.setText(chapter.getChapterName());
        setGraphic(box);
    }
}