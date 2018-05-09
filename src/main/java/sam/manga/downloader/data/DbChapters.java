package sam.manga.downloader.data;

import static sam.manga.downloader.data.ChapterMeta.STATE;
import static sam.manga.downloader.data.ChapterMeta.TITLE;
import static sam.manga.downloader.data.ChapterMeta.URL;
import static sam.manga.downloader.data.ChapterMeta.VOLUME;
import static sam.manga.newsamrock.chapters.ChaptersMeta.CHAPTER_ID;
import static sam.manga.newsamrock.chapters.ChaptersMeta.MANGA_ID;
import static sam.manga.newsamrock.chapters.ChaptersMeta.NUMBER;
import static sam.manga.newsamrock.chapters.ChaptersMeta.TABLE_NAME;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import sam.manga.downloader.chapter.Chapter;
import sam.manga.downloader.extra.Utils;
import sam.manga.downloader.manga.Manga;
import sam.sql.sqlite.SQLiteManeger;

class DbChapters {
    private Manga current;
    public void read(SQLiteManeger db, List<Manga> mangas, Logger logger) throws SQLException {
        Map<Integer, Manga> map = mangas.stream().collect(Collectors.toMap(m -> m.id, m -> m));

        db.iterate(DataManager.qm().selectAllFrom(TABLE_NAME).build(), rs -> {
            Chapter c = new Chapter(
                    rs.getInt(CHAPTER_ID),
                    rs.getInt(MANGA_ID),
                    rs.getString(VOLUME),
                    rs.getDouble(NUMBER),
                    rs.getString(URL),
                    rs.getString(TITLE),
                    Utils.parse(rs.getString(STATE)));

            if(current == null || current.id != c.mangaId)
                current = map.get(c.mangaId);

            if(current != null)
                current.addChapter(c);
            else
                logger.warning("no manga found for id: "+c.mangaId);
        });
    }
    
    Integer updateChapterTitles(SQLiteManeger db, List<Chapter> chapters, Logger logger) throws SQLException {
        Objects.requireNonNull(chapters, "chapters cannot be null");
        if(chapters.isEmpty()) {
            logger.warning("updateChapterTitles(db, chapters) -> chapters empty");
            return -1;
        }
        return db.prepareStatementBlock(DataManager.qm().update(TABLE_NAME).placeholders(TITLE).where(w -> w.eqPlaceholder(CHAPTER_ID)).build(), ps -> {
            for (Chapter c : chapters) {
                ps.setString(1, c.getTitle());
                ps.setInt(2, c.chapterId);
                ps.addBatch();
            }
            int ret = ps.executeBatch().length;
            db.commit();
            return ret;
        });
    }
}
