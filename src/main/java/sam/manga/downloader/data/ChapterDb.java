package sam.manga.downloader.data;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import sam.manga.downloader.chapter.Chapter;

public class ChapterDb {
    public  void commitChapterTitleToDatabase(Chapter c, PreparedStatement ps) throws SQLException {
        ps.setString(1, c.getTitle());
        ps.setInt(2, c.chapterId);
        ps.addBatch();
    }
}
