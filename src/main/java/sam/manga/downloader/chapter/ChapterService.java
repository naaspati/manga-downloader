package sam.manga.downloader.chapter;

import static javafx.concurrent.Worker.State.SUCCEEDED;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import sam.manga.downloader.page.Page;

class ChapterService  extends Service<Void>  {
    private final Chapter chapter;

    public ChapterService(Chapter chapter) {
        this.chapter = chapter;
    }
    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            public Void call() throws IOException {
                final long total = chapter.size();

                if(chapter.getState() == SUCCEEDED){
                    updateProgress(total, total);
                    return null;
                }

                Path chapSavePath = chapter.getSavePath(); 
                String[] files = Files.exists(chapSavePath) ? chapSavePath.toFile().list() : null;

                if(files != null && files.length == total){
                    updateProgress(total, total);
                    return null;
                } else if(files == null) {
                    Files.createDirectories(chapSavePath);
                }

                if(isCancelled())
                    return null;

                BitSet b = new BitSet();

                if(files != null) {
                    for (String f : files)
                        b.set(Integer.parseInt(f));
                }

                int progress = 0;
                updateProgress(progress, total);

                for (Page page : chapter) {
                    if(isCancelled())
                        return null;

                    if(page.isCompleted() || b.get(page.getOrder())){
                        updateProgress(++progress, total);
                        page.setCompleted();
                        continue;
                    }
                    if(page.isHalted())
                        continue;

                    new PageDownloader(chapter, page).run();

                    if(page.isCompleted())
                        updateProgress(++progress, total);
                }
                if(progress != total)
                    throw new InCompleteDownloadException();

                return null;
            }
        };
    }
    class InCompleteDownloadException extends IOException {
        private static final long serialVersionUID = 9023123708082447243L;
    } 
}
