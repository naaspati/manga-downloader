package sam.manga.downloader.chapter;

import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.event.EventHandler;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import sam.fx.clipboard.FxClipboard;
import sam.fx.popup.FxPopupShop;
import sam.manga.downloader.page.Page;

class FailedPageTable extends TableView<Page> implements EventHandler<KeyEvent> {
    private final String[] colNames = {"page_id","order","page url", "Image url", "Error"};
    private final TableViewSelectionModel<Page> model;  

    FailedPageTable() {
        model = getSelectionModel();
        model.setCellSelectionEnabled(true);
        model.setSelectionMode(SelectionMode.MULTIPLE);

        addEventFilter(KeyEvent.KEY_RELEASED, this);
        SimpleStringProperty sspImageUrl = new SimpleStringProperty("Image url");
        SimpleStringProperty sspPageUrl = new SimpleStringProperty("Page url");

        for (int i = 0; i < colNames.length; i++) {
            TableColumn<Page, String> c = new TableColumn<>(colNames[i]);
            switch (i) {
                case 0:
                    c.setCellValueFactory(cc -> new SimpleStringProperty(String.valueOf(cc.getValue().pageId)));
                    break;
                case 1:
                    c.setCellValueFactory(cc -> new SimpleStringProperty(String.valueOf(cc.getValue().getOrder())));
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
            getColumns().add(c);
        };
    }
    @Override
    public void handle(KeyEvent e) {
        if(e.isControlDown() && e.getCode() == KeyCode.C) {
            @SuppressWarnings("rawtypes")
            List<TablePosition> list = model.getSelectedCells();
            if(list.isEmpty())
                return;

            StringBuilder sb = new StringBuilder();
            for (@SuppressWarnings("rawtypes") TablePosition tp : list) {
                Page p = getItems().get(tp.getRow());

                switch (tp.getColumn()) {
                    case 0:
                        sb.append(p.pageId);
                        break;
                    case 1:
                        sb.append(p.getOrder());
                        break;
                    case 2:
                        sb.append(p.getPageUrl());
                        break;
                    case 3:
                        sb.append(p.getImageUrl());
                        break;
                    case 4:
                        sb.append(p.getError());
                        break;
                }
                sb.append('\n');
            }

            FxClipboard.copyToClipboard(sb.toString());
            FxPopupShop.showHidePopup("copied", 1500);
        }
    }
}