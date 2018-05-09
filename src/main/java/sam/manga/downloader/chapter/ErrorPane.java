package sam.manga.downloader.chapter;

import java.nio.file.Files;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.text.Text;
import sam.fx.clipboard.FxClipboard;
import sam.fx.popup.FxPopupShop;
import sam.manga.downloader.extra.Status;
import sam.manga.downloader.extra.Utils;
import sam.manga.downloader.page.Page;
import sam.myutils.MyUtils;
import sam.weak.WeakStore;

public class ErrorPane extends Accordion {
    /**
     * private final FailedPageTable errorTable;
    private final TitledPane errorTitledPane;
    private final Accordion errorAccordion;
    private final Button restartButton;
    private final Button openDirButton;
    private final Text errorText = FxText.ofClass("error-text");
     */
    @FXML private Button restartBtn;
    @FXML private Button openDirBtn;
    @FXML private Button copyBtn;
    @FXML private Text errorText;
    @FXML private FailedPageTable errorTable;
    @FXML private TitledPane errorTitledPane;
    private ChapterDownloadListCell chapter;

    private static final WeakStore<StringBuilder> WEAK_STORE = new WeakStore<>(StringBuilder::new);

    ErrorPane() {
        Utils.fxml(this);

        restartBtn.setOnAction(e -> chapter.restart());
        openDirBtn.setOnAction(e -> Utils.open(chapter.getChapter().getSavePath().toUri().toString()));
        copyBtn.setOnAction(this::copy);
    }
    public void set(ChapterDownloadListCell chapter) {
        this.chapter = chapter;
        errorTable.getItems().clear();

        ChapterService service = chapter.getChapterService();
        errorText.setText(MyUtils.exceptionToString(service.getException()));

        for (Page page : chapter.getChapter()) {
            if(page.hasError())
                errorTable.getItems().add(page);
        }
        errorTitledPane.setText(getCounts());
        openDirBtn.setDisable(Files.notExists(chapter.getChapter().getSavePath()));
    }

    private String getCounts() {
        return WEAK_STORE.pollWrap(sb -> {
            sb.setLength(0);

            int c = 0, f = 0, h = 0;
            EnumMap<Status, AtomicInteger> map = null;

            for (Page page : chapter.getChapter()) {
                Status s = page.getStatus();
                if(s == null)
                    continue;

                switch (page.getStatus()) {
                    case COMPLETED:
                        c++;
                        break;
                    case FAILED:
                        f++;
                        break;
                    case HALTED:
                        h++;
                        break;
                    default:
                        if(map == null)
                            map = new EnumMap<>(Status.class);
                        map.computeIfAbsent(s, a -> new AtomicInteger()).incrementAndGet();
                        break;
                }
            }

            sb.append("C: ").append(c).append(" | ")
            .append("F: ").append(f).append(" | ")
            .append("H: ").append(h).append(" | ");

            if(map != null)
                map.forEach((s,t) -> sb.append(String.valueOf(s).charAt(0)).append(": ").append(t.get()).append(" | "));                

            return sb.toString();
        });
    }

    public void copy(Object anything) {
        WEAK_STORE.pollWrap(sb1 -> {
            sb1.setLength(0);
            copyToClipboard(errorTable.getItems()
                    .stream()
                    .reduce(sb1, (sb, p) -> {
                        sb.append(p.pageId).append('\t')
                        .append(p.getOrder()).append('\t')
                        .append(p.getPageUrl()).append('\t')
                        .append(p.getImageUrl()).append('\n');
                        return sb;
                    }, StringBuilder::append).toString()
                    );            
        });
    }
    static void copyToClipboard(String content) {
        FxClipboard.copyToClipboard(content);
        FxPopupShop.showHidePopup("Copied", 1500);
    }

    public void restart(){
        chapter.restart();
        restartBtn.setDisable(true);
    }
}

