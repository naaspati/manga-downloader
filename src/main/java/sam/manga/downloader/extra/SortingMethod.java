package sam.manga.downloader.extra;

import java.util.Comparator;

import sam.manga.downloader.manga.MangaPresenter;

public enum SortingMethod {
    NAME(Comparator.comparing(m -> m.getManga().getMangaName())),
    CHAPTER_COUNT(Comparator.comparing(m -> m.getManga().chaptersCount()));

    public final Comparator<MangaPresenter> comparator;
    SortingMethod(Comparator<MangaPresenter> comparator){
        this.comparator = comparator;
    }
}