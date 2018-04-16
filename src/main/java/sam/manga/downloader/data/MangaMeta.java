package sam.manga.downloader.data;

import sam.manga.newsamrock.mangas.MangasMeta;

interface MangaMeta extends MangasMeta {
    final String URL = "url";
    final String STATUS = "status";
    
    final String createTableSql = 
            String.format("CREATE TABLE  `%s` ("+
                    "   `%s`    INTEGER NOT NULL PRIMARY KEY UNIQUE,"+
                    "   `%s`  TEXT NOT NULL UNIQUE,"+
                    "   `%s`  TEXT NOT NULL UNIQUE,"+
                    "   `%s`   TEXT NOT NULL UNIQUE,"+
                    "   `%s`    TEXT NOT NULL DEFAULT 'UNTOUCHED'"+
                    ");", TABLE_NAME, MANGA_ID, DIR_NAME, MANGA_NAME, URL, STATUS);
}
