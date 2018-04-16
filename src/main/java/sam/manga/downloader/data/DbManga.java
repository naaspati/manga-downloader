package sam.manga.downloader.data;

import static sam.manga.downloader.data.MangaMeta.STATUS;
import static sam.manga.downloader.data.MangaMeta.URL;
import static sam.manga.newsamrock.mangas.MangasMeta.MANGA_ID;
import static sam.manga.newsamrock.mangas.MangasMeta.MANGA_NAME;
import static sam.manga.newsamrock.mangas.MangasMeta.TABLE_NAME;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sam.manga.downloader.extra.Status;
import sam.manga.downloader.manga.Manga;
import sam.sql.sqlite.SQLiteManeger;

class DbManga {
    public List<Manga> read(SQLiteManeger db) throws SQLException {
        List<Manga> list = new ArrayList<>();
        db.iterate(DataManager.qm().selectAllFrom(TABLE_NAME).build(), rs -> list.add(manga(rs)));
        return list;
    }
    private Manga manga(ResultSet rs) throws SQLException {
        return new Manga(
                rs.getString(MANGA_NAME), 
                rs.getInt(MANGA_ID), 
                rs.getString(URL), 
                Status.parse(rs.getString(STATUS)));
        }
}
