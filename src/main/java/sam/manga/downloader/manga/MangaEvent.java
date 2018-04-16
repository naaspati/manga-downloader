package sam.manga.downloader.manga;

@FunctionalInterface
public interface MangaEvent {
    public void clicked(MangaPresenter manga);

}
