package sam.manga.downloader.data;

import static sam.manga.downloader.data.ChapterMeta.STATUS;
import static sam.manga.downloader.data.PageMeta.ERRORS;
import static sam.manga.downloader.data.PageMeta.IMAGE_URL;
import static sam.manga.downloader.data.PageMeta.ORDER;
import static sam.manga.downloader.data.PageMeta.PAGE_ID;
import static sam.manga.downloader.data.PageMeta.PAGE_URL;
import static sam.manga.downloader.data.PageMeta.TABLE_NAME;
import static sam.manga.newsamrock.chapters.ChaptersMeta.CHAPTER_ID;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import sam.manga.downloader.extra.Status;
import sam.manga.downloader.manga.Manga;
import sam.manga.downloader.page.Page;
import sam.sql.sqlite.SQLiteManeger;

public class DbPages {
    public void read(SQLiteManeger db, List<Manga> mangas, Logger logger) throws SQLException {
        HashMap<Integer, List<Page>> collect = new HashMap<>();
        Function<Integer, List<Page>> func = c -> new ArrayList<>();

        db.iterate(DataManager.qm().selectAllFrom(TABLE_NAME).build(),
                rs -> {
                    Page p = new Page(
                            rs.getInt(PAGE_ID),
                            rs.getInt(CHAPTER_ID),
                            rs.getInt(ORDER),
                            rs.getString(IMAGE_URL),
                            rs.getString(PAGE_URL),
                            Status.parse(rs.getString(STATUS)),
                            rs.getString(ERRORS));

                    collect.computeIfAbsent(p.chapterId, func).add(p);
                });

        mangas.stream()
        .flatMap(Manga::stream)
        .forEach(c -> {
            List<Page> list = collect.remove(c.chapterId);
            if(list != null)
                c.setPages(list);
        });
        
        if(!collect.isEmpty())
            logger.warning(collect.keySet().stream().map(String::valueOf).collect(Collectors.joining(",", "chapter(s) not found for chapter-id(s): [", "]")));
    }
    
    public int resetPage(SQLiteManeger db, Collection<Page> pages) throws SQLException {
        String sql = DataManager.qm().update(PageMeta.TABLE_NAME).placeholders(PageMeta.STATUS, PageMeta.ERRORS).where(w -> w.eqPlaceholder(PageMeta.PAGE_ID)).build();

        try (PreparedStatement ps = db.prepareStatement(sql)){
            for (Page p : pages) {
                ps.setString(1, p.getStatus() == null ? null : p.getStatus().toString());
                ps.setString(2, p.getError());
                ps.setInt(3, p.pageId);
                ps.addBatch();    
            }
            return ps.executeBatch().length;
        }
    }

}
