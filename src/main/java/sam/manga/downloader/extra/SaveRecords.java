package sam.manga.downloader.extra;

import static sam.manga.downloader.extra.Utils.LOGS_DIR;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import sam.fileutils.RemoveInValidCharFromString;
import sam.fx.alert.FxAlert;
import sam.manga.downloader.chapter.Chapter;
import sam.manga.downloader.chapter.ChapterPresenter;
import sam.manga.downloader.manga.Manga;
import sam.manga.downloader.manga.MangaPresenter;
import sam.manga.downloader.page.Page;

public class SaveRecords {
    private final StringBuilder sb = new StringBuilder();
    
    public SaveRecords(List<MangaPresenter> mangas) {
        for (MangaPresenter m : mangas)
            manga(m);
    }
    
    private void manga(MangaPresenter mangaP) {
        Path path  = LOGS_DIR.resolve(RemoveInValidCharFromString.removeInvalidCharsFromFileName(mangaP.getMangaName())+".txt");

        if(!mangaP.stream().flatMap(cp -> cp.getChapter().stream()).anyMatch(Page::hasError)){
            try {
                Files.createDirectories(path.getParent());
                Files.deleteIfExists(path);
            } catch (IOException e) {
                FxAlert.showErrorDialog("failed to delete\nFile: "+path, "Manga.saveRecords() Error", e, false);
            }
            return;
        }
        
        Manga manga = mangaP.getManga();

        StringBuilder main = new StringBuilder()
                .append(manga.getId()).append('\n')
                .append(manga.getMangaName()).append('\n')
                .append(manga.getUrl()).append('\n')
                .append("\n")
                .append(manga.getId()).append(" \n");


        StringBuilder id_urls = new StringBuilder();

        mangaP.stream()
        .filter(c -> c.getChapter().stream().anyMatch(Page::hasError))
        .peek(c -> {
            main.deleteCharAt(main.length() - 1);
            main.append(c.getChapter().getNumber()).append(" \n");
        }).forEach(c -> chapter(c, main, id_urls));

        main.append("\n\n------------------------------------------\n\n").append(id_urls);

        try {
            Files.write(path, main.toString().getBytes("utf-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {}
        
    }

    private void chapter(ChapterPresenter presenter, StringBuilder main, StringBuilder id_urls) {
        /** TODO
         * if(presenter.getDirectoryCreateFailedError() != null){
            main.append(presenter.getDirectoryCreateFailedError());
            return;
        }
         */
        
        Chapter chapter = presenter.getChapter();
        main
        .append("id: ")
        .append(chapter.chapterId)
        .append(", number: ")
        .append(chapter.getNumber())
        .append(", title: ")
        .append(chapter.getTitle())
        .append(", volume: ")
        .append(chapter.getVolume())
        .append("\nurl: ")
        .append(chapter.getUrl())
        .append("\nchapter_savePath: ")
        .append(chapter.getSavePath())
        .append("\n\n");

        int length = id_urls.length(); 
        chapter.stream()
        .filter(Page::hasError)
        .forEach(p -> {
            id_urls.append(p.pageId).append('\t')
            .append(p.getOrder()).append('\t')
            .append(p.getPageUrl()).append('\t')
            .append(p.getImageUrl()).append('\t')
            .append(p.getStatus()).append('-').append(p.getError()).append('\n');
        });
        while(length < id_urls.length()) main.append(id_urls.charAt(length++));
    }

    
    

}
