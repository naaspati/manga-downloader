package sam.manga.downloader.db;

interface PageMeta {
    final String createTableSql = 
            "CREATE TABLE `Pages` ("+
                    "   `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE ,"+
                    "   `chapter_id`    INTEGER NOT NULL,"+
                    "   `manga_id`  INTEGER NOT NULL,"+
                    "   `_order`    INTEGER NOT NULL,"+
                    "   `status`    TEXT NOT NULL DEFAULT 'UNTOUCHED',"+
                    "   `page_url`  TEXT,"+
                    "   `url`   TEXT,"+
                    "   `errors`    TEXT"+
                    ");";
    
    final String TABLE_NAME = "Pages";
    final String  id = "id";
    final String  chapter_id = "chapter_id";
    final String  manga_id = "manga_id";
    final String  order = "_order";
    final String  status = "status";
    final String  page_url = "page_url";
    final String  url = "url";
    final String  errors = "errors";
}
