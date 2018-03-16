package sam.manga.downloader.db;

import static sam.properties.myconfig.MyConfig.MANGAROCK_INPUT_DB;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.sqlite.JDBC;

import javafx.scene.control.Alert.AlertType;
import sam.fx.alert.FxAlert;
import sam.manga.downloader.DownloaderApp;
import sam.manga.downloader.extra.Status;
import sam.manga.downloader.parts.Chapter;
import sam.manga.downloader.parts.Manga;
import sam.manga.downloader.parts.Page;
import sam.manga.downloader.view.chapter.ChapterPresenter;
import sam.manga.downloader.view.manga.MangaPresenter;
import sam.sql.sqlite.SqliteManeger;
import sam.sql.sqlite.querymaker.QueryMaker;

public class DbManager {
    private final List<Manga> mangas;
    private final File dbFile;

    public DbManager(File dbFile) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IOException {
        this.dbFile = dbFile;

        try(SqliteManeger db = new SqliteManeger(dbFile, true)) {
            HashMap<Integer, List<Page>> pagesMap = readPages(db);
            HashMap<Integer, List<Chapter>> chaptersMap = readChapters(db, pagesMap);
            mangas = readMangas(db, chaptersMap);
        }
    }
    public List<Manga> getMangas() {
        return Collections.unmodifiableList(mangas);
    }
    public File getDbFile() {
        return dbFile;
    }
    private Integer updateChapterTitles(SqliteManeger db, List<Chapter> chapters) throws SQLException {
        Objects.requireNonNull(chapters, "chapters cannot be null");
        if(chapters.isEmpty()) {
            Logger.getLogger(getClass().getName()).warning("updateChapterTitles(db, chapters) -> chapters empty");
            return -1;
        }
        return db.prepareStatementBlock(qm().update(ChapterMeta.TABLE_NAME).placeholders(ChapterMeta.title).where(w -> w.eqPlaceholder(ChapterMeta.id)).build(), ps -> {
            for (Chapter c : chapters) {
                ps.setString(1, c.getTitle());
                ps.setInt(2, c.getId());
                ps.addBatch();
            }
            int ret = ps.executeBatch().length;
            db.commit();
            return ret;
        });

    }

    private List<Manga> readMangas(SqliteManeger db, HashMap<Integer, List<Chapter>> chaptersMap) throws SQLException {
        List<Manga> mangas = new ArrayList<>();

        List<Manga> withDuplicateChapterTitleErrors = new ArrayList<>();

        db.executeQueryIterate("SELECT * FROM ".concat(MangaMeta.TABLE_NAME), 
                rs -> {
                    Manga m = readManga(rs, chaptersMap.get(rs.getInt(MangaMeta.id)));

                    if(m.hasDuplicateChapterNames())
                        withDuplicateChapterTitleErrors.add(m);

                    mangas.add(m);
                });

        if(!withDuplicateChapterTitleErrors.isEmpty())
            updateChapterTitles(db, withDuplicateChapterTitleErrors.stream().flatMap(Manga::stream).collect(Collectors.toList()));

        return mangas;
    }

    private HashMap<Integer, List<Chapter>> readChapters(SqliteManeger db, HashMap<Integer, List<Page>> pagesMap) throws SQLException {
        HashMap<Integer, List<Chapter>> map = new HashMap<>(pagesMap.size());
        Function<Integer, List<Chapter>> computer = i -> new ArrayList<Chapter>();

        db.executeQueryIterate("SELECT * FROM ".concat(ChapterMeta.TABLE_NAME), 
                rs -> map.computeIfAbsent(rs.getInt(ChapterMeta.manga_id), computer).add(readChapter(rs, pagesMap.get(rs.getInt(PageMeta.chapter_id)))) );

        return map;
    }

    private HashMap<Integer, List<Page>> readPages(SqliteManeger db) throws SQLException {
        HashMap<Integer, List<Page>> pagesMap = new HashMap<>(1000);

        Function<Integer, List<Page>> computer = i -> new ArrayList<Page>();

        db.executeQueryIterate("SELECT * FROM ".concat(PageMeta.TABLE_NAME), 
                rs -> pagesMap.computeIfAbsent(rs.getInt(PageMeta.chapter_id), computer).add(readPage(rs)) );

        return pagesMap;
    }

    private QueryMaker qm() {
        return QueryMaker.getInstance();
    }

    private int resetPage(SqliteManeger db, Collection<Page> pages) throws SQLException {
        String sql = qm().update(PageMeta.TABLE_NAME).placeholders(PageMeta.status, PageMeta.errors).where(w -> w.eqPlaceholder(PageMeta.id)).build();

        try (PreparedStatement ps = db.prepareStatement(sql)){
            for (Page p : pages) {
                ps.setString(1, p.getStatus() == null ? null : p.getStatus().toString());
                ps.setString(2, p.getError());
                ps.setInt(3, p.getId());
                ps.addBatch();    
            }
            return ps.executeBatch().length;
        }
        //TODO
    }

    private Manga readManga(ResultSet rs, List<Chapter> chapters) throws SQLException {
        return new Manga(
                rs.getString(MangaMeta.manga_name), 
                rs.getInt(MangaMeta.id), 
                rs.getString(MangaMeta.url), 
                toStatus(rs.getString(MangaMeta.status)), 
                chapters);
    }

    private Chapter readChapter(ResultSet rs, List<Page> pages) throws SQLException {
        return new Chapter(
                rs.getInt(ChapterMeta.id),
                rs.getInt(ChapterMeta.manga_id),
                rs.getString(ChapterMeta.volume),
                rs.getDouble(ChapterMeta.number),
                rs.getString(ChapterMeta.url),
                rs.getString(ChapterMeta.title),
                toStatus(rs.getString(ChapterMeta.status)),
                pages);
    }

    private Page readPage(ResultSet rs) throws SQLException {
        return new Page(
                rs.getInt(PageMeta.id),
                rs.getInt(PageMeta.order),
                rs.getString(PageMeta.url),
                rs.getString(PageMeta.page_url),
                toStatus(rs.getString(PageMeta.status)),
                rs.getString(PageMeta.errors));
    }
    private Status toStatus(String s) {
        if(s == null)
            return null;
        return Status.valueOf(s);
    }
    public boolean createMangarockDatabase() throws SQLException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException{
        Path mip = Paths.get(MANGAROCK_INPUT_DB);

        if(Files.notExists(mip))
            throw new IOException("output mangarock.db not found\n"+MANGAROCK_INPUT_DB);
        if(!dbFile.exists())
            throw new IOException("input mangaFox.db not found\n"+dbFile);

        Files.copy(mip, mip.resolveSibling(mip.getFileName()+"_ "+LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)).replace(':', ' ')), StandardCopyOption.REPLACE_EXISTING);

        try (SqliteManeger output = new SqliteManeger(MANGAROCK_INPUT_DB, true);
                SqliteManeger input = new SqliteManeger(dbFile, true);
                ) {
            output.executeUpdate("DELETE FROM DownloadTask");

            output.prepareStatementBlock(
                    qm().insertInto("DownloadTask").placeholders("chapter_name", "dir_name", "chapter_id", "manga_id", "source_id"), 
                    ps -> {
                        input.executeQueryIterate(qm()
                                .select(ChapterMeta.id, ChapterMeta.manga_id, ChapterMeta.number, ChapterMeta.title, ChapterMeta.page_count)
                                .from(ChapterMeta.TABLE_NAME).build(),
                                rs -> {
                                    String chapter_id = rs.getString("id");
                                    String manga_id = rs.getString("manga_id");

                                    ps.setString(1, Chapter.generateChapterName(rs.getDouble("number"), rs.getString("title")));
                                    ps.setString(2, "custom/mangarock/folder/"+chapter_id);
                                    ps.setString(3, chapter_id);
                                    ps.setString(4, manga_id);
                                    ps.setString(5, rs.getString("page_count"));
                                    ps.addBatch();
                                });

                        return ps.executeBatch().length;
                    });

            output.commit();
        }
        return true;
    }
    public synchronized void commitDatabase() throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException {
        try (SqliteManeger db = new SqliteManeger(dbFile, true)) {
/**
 *             for (MangaPresenter d : mangasList)
                d.databaseCommit(generalStmnt, resetChapter, resetPage);


            generalStmnt.executeBatch();
            resetChapter.executeBatch();
            resetPage.executeBatch();

            con.commit();
 */
        }
    }
    void databaseCommit(Statement generalStmnt, PreparedStatement resetChapter, PreparedStatement resetPage) throws SQLException {
        if(!dataUpdated.get())
            return;

        if(completedCount.get() == getChaptersCount()){
            generalStmnt.addBatch("UPDATE Mangas SET status = 'COMPLETED' WHERE id = "+manga.getId());
            generalStmnt.addBatch("UPDATE Chapters SET status = 'COMPLETED' WHERE manga_id = "+manga.getId());
            generalStmnt.addBatch("UPDATE Pages SET status = 'COMPLETED', errors = NULL WHERE manga_id = "+manga.getId());
        }
        else {
            Status status = null;
            if(remainingCount.get() > 1)
                status = Status.QUEUED;
            else if(failedCount.get() > 1)
                status = Status.FAILED;

            if(status != null)
                generalStmnt.addBatch("UPDATE Mangas SET status = '"+status+"' WHERE id = "+manga.getId());

            for (ChapterPresenter c : manga) 
                c.databaseCommit(generalStmnt, resetChapter, resetPage);                    
        }
    }
}
