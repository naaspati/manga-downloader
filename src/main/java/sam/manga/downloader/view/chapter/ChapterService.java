package sam.manga.downloader.view.chapter;

import static sam.manga.downloader.extra.Status.COMPLETED;
import static sam.manga.downloader.extra.Status.FAILED;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import sam.manga.downloader.PageDownload;
import sam.manga.downloader.extra.Status;
import sam.manga.downloader.parts.Chapter;
import sam.manga.downloader.parts.Page;

class ChapterService extends Service<Void> {
    private volatile String directoryCreateFailedError;
    private final Chapter chapter;
    final ReadOnlyBooleanWrapper dataUpdated;

    public ChapterService(Chapter chapter, ReadOnlyBooleanWrapper dataUpdated) {
        this.chapter = chapter;
        this.dataUpdated = dataUpdated;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            public Void call() throws IOException {
                final long total = chapter.getPageCount();

                if(isChapterCompleted()){
                    updateProgress(total, total);
                    return null;
                }

                Path chapSavePath = chapter.getSavePath(); 
                String[] files = Files.exists(chapSavePath) ? chapSavePath.toFile().list() : null;

                if(files != null && files.length == total){
                    updateProgress(total, total);
                    setStatus(COMPLETED);
                    return null;
                }
                else if(files == null) {
                    try {
                        Files.createDirectories(chapSavePath);
                    } catch (IOException e) {
                        addCreateDirectoryError(e);
                        updateProgress(0, total);
                        setStatus(FAILED);
                        return null;
                    }
                }

                if(isCancelled())
                    return null;

                int[] numbers = files == null ? null : Stream.of(files).mapToInt(Integer::parseInt).sorted().toArray();

                int progress = 0;
                updateProgress(progress, total);

                for (Page page : chapter) {
                    if(isCancelled())
                        return null;

                    if(page.isCompleted() || (numbers != null && Arrays.binarySearch(numbers, page.getOrder()) >= 0)){
                        updateProgress(++progress, total);
                        page.setCompleted();
                        continue;
                    }
                    if(page.isHalted())
                        continue;

                    new PageDownload(chapter, page).run();

                    if(page.isCompleted())
                        updateProgress(++progress, total);
                }

                setStatus(progress == total ? COMPLETED : FAILED);
                return null;
            }

            private boolean isChapterCompleted() {
                synchronized (chapter) {
                    return chapter.isCompleted();   
                }
            }
        };
    }
    private void addCreateDirectoryError(Exception e) {
        try(StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);) {

            pw.print("Failed, to create chapter_dir");
            e.printStackTrace(pw);
            directoryCreateFailedError = sw.toString();
            chapter.setFailed();
            setStatus(FAILED);
        }
        catch (IOException e2) {}
    }
    public String getDirectoryCreateFailedError() {
        return directoryCreateFailedError;
    }
    public boolean hasError() {
        return directoryCreateFailedError != null || chapter.stream().anyMatch(Page::hasError);
    }

    private void setStatus(Status status) {
        synchronized (chapter) {
            if(directoryCreateFailedError != null){
                chapter.setFailed();
                return;
            }

            if(status != chapter.getStatus())
                Platform.runLater(() -> dataUpdated.set(true));

            chapter.setStatus(status);
        }
    }
}
