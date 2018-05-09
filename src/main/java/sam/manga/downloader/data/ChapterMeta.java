package sam.manga.downloader.data;

import sam.manga.newsamrock.chapters.ChaptersMeta;

interface ChapterMeta extends  ChaptersMeta {
    final String  URL = "url";
    final String  PAGE_COUNT = "page_count";
    final String  STATE = "state";
    final String VOLUME = "volume";
    final String TITLE = "title";
    
    final String createTableSql = 
            String.format("CREATE TABLE `%s` ("+
                    "   `%s`    INTEGER NOT NULL UNIQUE PRIMARY KEY,"+
                    "   `%s`  INTEGER NOT NULL,"+
                    "   `%s`    TEXT,"+
                    "   `%s`    REAL NOT NULL,"+
                    "   `%s` TEXT,"+
                    "   `%s`   TEXT NOT NULL,"+
                    "   `%s`    INTEGER NOT NULL,"+
                    "   `%s`    TEXT NOT NULL DEFAULT 'UNTOUCHED'"+
                    ");", TABLE_NAME, CHAPTER_ID, MANGA_ID, VOLUME, NUMBER, TITLE, URL, PAGE_COUNT, STATE);
}
