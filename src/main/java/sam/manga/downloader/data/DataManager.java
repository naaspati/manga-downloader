package sam.manga.downloader.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import sam.manga.downloader.chapter.Chapter;
import sam.manga.downloader.extra.Utils;
import sam.manga.downloader.manga.Manga;
import sam.manga.downloader.page.Page;
import sam.manga.downloader.scrapper.Scrapper;
import sam.sql.querymaker.QueryMaker;
import sam.sql.sqlite.SQLiteManeger;

public class DataManager {
    private final List<Manga> mangas;
    final Path dbFile;
    private final  Logger logger = Logger.getLogger(getClass().getName());
    
    private static volatile DataManager instance;
    
    private final int minPageId, minChapId;
    private static final AtomicInteger chapterId = new AtomicInteger();
    private static final AtomicInteger pageId = new AtomicInteger();
    
    public static int newId(Class<?> cls) {
        if(cls == Chapter.class)
           return chapterId.incrementAndGet();
        else if(cls == Page.class)
            return pageId.incrementAndGet();
        else 
            throw new IllegalArgumentException("unknown class: "+cls);
    }
    public static DataManager getInstance() {
        if(instance == null)
            throw new IllegalStateException("not initiated");
        return instance;
    }
    /**
     * default initlizer, loaded its data from tsv(s)
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    public static void init() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IOException {
        if(instance != null)
            throw new IllegalStateException("already initiated");
        instance = new DataManager();
    }
    
    private DataManager() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IOException {
        dbFile = Utils.SESSION_DIR.resolve(Scrapper.URL_COLUMN+".db");
        
        if(Files.notExists(dbFile)) {
            mangas = TsvMangaLoader.load();
            minPageId = 0;
            minChapId = 0;
        }
        else {
            mangas = load();
            minPageId = mangas.stream().flatMap(Manga::stream).flatMap(Chapter::stream).mapToInt(p -> p.pageId).max().orElse(0);
            minChapId = mangas.stream().flatMap(Manga::stream).mapToInt(c -> c.chapterId).max().orElse(0);
        }
        
        chapterId.set(minChapId + 1);
        pageId.set(minPageId + 1);
    }
    
    private List<Manga> load() throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException {
        try(SQLiteManeger db = new SQLiteManeger(dbFile)) {
            List<Manga> mangas = new DbManga().read(db);
            new DbChapters().read(db, mangas, logger);
            new DbPages().read(db, mangas, logger);
            
            List<Chapter> withDuplicateChapterTitleErrors = mangas.stream()
                    .filter(Manga::hasDuplicateChapterNames)
                    .flatMap(Manga::stream)
                    .collect(Collectors.toList());
            
            if(!withDuplicateChapterTitleErrors.isEmpty())
                new DbChapters().updateChapterTitles(db, withDuplicateChapterTitleErrors, logger);
            
            return mangas;
        }
    }
    public List<Manga> getMangas() {
        return Collections.unmodifiableList(mangas);
    }
    
   static QueryMaker qm() {
        return QueryMaker.getInstance();
    }
    public synchronized void commitDatabase() throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException {
        try (SQLiteManeger db = new SQLiteManeger(dbFile)) {
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
    public static void stop() throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException {
        instance.commitDatabase();
    }
    
    /** TODO
     *     void databaseCommit(Statement generalStmnt, PreparedStatement resetChapter, PreparedStatement resetPage) throws SQLException {
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
     */
    

}
