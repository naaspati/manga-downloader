package sam.manga.downloader.chapter;

import static sam.manga.downloader.extra.Utils.getImageInputStream;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import sam.weak.WeakKeep;

public class ButtonIcon extends ImageView {
    private static final int ICON_SIZE = 30; 
    private static final WeakKeep<Image> refresh_icon = new WeakKeep<>(() -> img("repeat-1.png"));
    private static final WeakKeep<Image> open_dir_icon = new WeakKeep<>(() -> img("folder-11.png"));;
    private static final WeakKeep<Image> copy_text_icon = new WeakKeep<>(() -> img("send.png"));;
    
    private static Image img(String url) {
        return new Image(getImageInputStream(url), ICON_SIZE, 0, true, true);
    }
    
    public void setImagePath(String img) {
        switch (img) {
            case "refresh_icon":
                setImage(refresh_icon.get());
                break;
            case "open_dir_icon":
                setImage(open_dir_icon.get());
                break;
            case "copy_text_icon":
                setImage(copy_text_icon.get());
                break;
            default:
                throw new IllegalArgumentException(img);
        }
    }
}
