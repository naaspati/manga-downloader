package sam.manga.downloader.db;

interface MangaMeta {
    final String createTableSql = 
            "CREATE TABLE  `Mangas` ("+
                    "   `id`    INTEGER NOT NULL PRIMARY KEY UNIQUE,"+
                    "   `dir_name`  TEXT NOT NULL UNIQUE,"+
                    "   `manga_name`  TEXT NOT NULL UNIQUE,"+
                    "   `url`   TEXT NOT NULL UNIQUE,"+
                    "   `status`    TEXT NOT NULL DEFAULT 'UNTOUCHED'"+
                    ");";
    
    final String TABLE_NAME = "Mangas";
    final String id = "id";
    final String dir_name = "dir_name";
    final String manga_name = "manga_name";
    final String url = "url";
    final String status = "status";
}
