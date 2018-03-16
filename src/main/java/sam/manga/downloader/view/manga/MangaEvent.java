package sam.manga.downloader.view.manga;

@FunctionalInterface
public interface MangaEvent {
    public void clicked(MangaPresenter manga);

}
