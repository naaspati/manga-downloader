package sam.manga.downloader.data;

interface PageMeta {
    
    final String TABLE_NAME = "Pages";
    final String  PAGE_ID = "page_id";
    final String  CHAPTER_ID = "chapter_id";
    final String  MANGA_ID = "manga_id";
    final String  ORDER = "_order";
    final String  STATUS = "status";
    final String  PAGE_URL = "page_url";
    final String  IMAGE_URL = "image_url";
    final String  ERRORS = "errors";
    
    final String createTableSql = 
            String.format("CREATE TABLE `%s` ("+
                    "   `%s` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE ,"+
                    "   `%s`    INTEGER NOT NULL,"+
                    "   `%s`  INTEGER NOT NULL,"+
                    "   `%s`    INTEGER NOT NULL,"+
                    "   `%s`    TEXT NOT NULL DEFAULT 'UNTOUCHED',"+
                    "   `%s`  TEXT,"+
                    "   `%s`   TEXT,"+
                    "   `%s`    TEXT"+
                    ");",
                    TABLE_NAME,
                    PAGE_ID,
                    CHAPTER_ID,
                    MANGA_ID,
                    ORDER,
                    STATUS,
                    PAGE_URL,
                    IMAGE_URL,
                    ERRORS);
    
}
