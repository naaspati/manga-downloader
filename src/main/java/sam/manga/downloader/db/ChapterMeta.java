package sam.manga.downloader.db;

interface ChapterMeta {
    final String createTableSql = 
            "CREATE TABLE `Chapters` ("+
                    "   `id`    INTEGER NOT NULL UNIQUE PRIMARY KEY,"+
                    "   `manga_id`  INTEGER NOT NULL,"+
                    "   `volume`    TEXT,"+
                    "   `number`    REAL NOT NULL,"+
                    "   `title` TEXT,"+
                    "   `url`   TEXT NOT NULL,"+
                    "   `page_count`    INTEGER NOT NULL,"+
                    "   `status`    INTEGER NOT NULL DEFAULT 'UNTOUCHED'"+
                    ");";

    final String TABLE_NAME = "Chapters";
    final String  id = "id";
    final String  manga_id = "manga_id";
    final String  volume = "volume";
    final String  number = "number";
    final String  title = "title";
    final String  url = "url";
    final String  page_count = "page_count";
    final String  status = "status";
}
