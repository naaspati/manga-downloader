package sam.manga.downloader.extra;

import static sam.manga.downloader.extra.Status.ALL_CHAPTERS_COMPLETED;
import static sam.manga.downloader.extra.Status.COMPLETED;
import static sam.manga.downloader.extra.Status.FAILED;
import static sam.manga.downloader.extra.Status.HALTED;
import static sam.manga.downloader.extra.Status.QUEUED;
import static sam.manga.downloader.extra.Status.RUNNING;
import static sam.manga.downloader.extra.Status.UNTOUCHED;

public interface StatusHelper {
    Status getStatus();
    void setStatus(Status status);
    
    default boolean isAllChaptersCompleted() {
        return getStatus() == ALL_CHAPTERS_COMPLETED;
    }
    
    default boolean isCompleted() {
        return getStatus() == COMPLETED;
    }
    default boolean isFailed() {
        return getStatus() == FAILED;
    }
    default boolean isQueued() {
        return getStatus() == QUEUED;
    }
    default boolean isUntouched() {
        return getStatus() == UNTOUCHED;
    }
    default boolean isHalted() {
        return getStatus() == HALTED;
    }
    default void setCompleted() {
        setStatus( COMPLETED);
    }
    default void setFailed() {
        setStatus( FAILED);
    }
    default void setQueued() {
        setStatus( QUEUED);
    }
    default void setUntouched() {
        setStatus( UNTOUCHED);
    }
    default void setHalted() {
        setStatus( HALTED);
    }
    
    default void setRunning() {
        setStatus( RUNNING);
    }
    default boolean isRunning() {
        return getStatus() == RUNNING;
    }
}
