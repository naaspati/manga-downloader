package sam.manga.downloader;

import java.util.Formatter;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sam.manga.downloader.extra.Utils;
import sam.manga.downloader.manga.Manga;
import sam.manga.downloader.manga.MangaPresenter;
import sam.weak.WeakKeep;

public class MangaInfoAction implements EventHandler<ActionEvent> {
    private final ReadOnlyObjectProperty<MangaPresenter> currentManga;

    public MangaInfoAction(ReadOnlyObjectProperty<MangaPresenter> currentManga) {
        this.currentManga = currentManga;
    }

    private WeakKeep<Object[]> weakFormatter = new WeakKeep<>(() -> {
        StringBuilder sb = new StringBuilder();
        return new Object[] {sb, new Formatter(sb)};
    });

    private WeakKeep<MangaInfoView> weakStg = new WeakKeep<>(MangaInfoView::new);

    @Override
    public void handle(ActionEvent event) {
        Object[] o =  weakFormatter.get();
        ((StringBuilder)o[0]).setLength(0);

        MangaPresenter mp = currentManga.get(); 
        Manga m = mp.getManga();
        
        String s = ((Formatter)o[1])
                .format("name: \nid: \nurl: \nstatus: \n\nchapters: \n\ttotal: \n\tqueued: \n\tcompleted: \n\tfailed: \n\n", 
                        m.getMangaName(),
                        m.getId(),
                        m.getUrl(),
                        m.getStatus(),
                        m.chaptersCount(),
                        mp.queuedCountProperty().get(),
                        mp.completedCountProperty().get(),
                        mp.failedCountProperty().get()
                        ).toString();

        MangaInfoView stg = weakStg.get();
        stg.set(m.getMangaName(), s);
        stg.sizeToScene();
        stg.show();
    };
    
    public class MangaInfoView extends Stage {
        @FXML
        private TextArea ta;
        
        public void set(String title, String content) {
            setTitle(title);
            ta.setText(content);
        }
        
        public MangaInfoView() {
            super(StageStyle.UTILITY);
            initOwner(Utils.stage());
            Utils.fxmlLoad("MangaInfoView.fxml", this, this);
        }

    }

    
}
