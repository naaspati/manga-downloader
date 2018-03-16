package sam.manga.downloader.extra;

import static sam.manga.downloader.extra.Status.*;

import sam.manga.downloader.extra.Status;

public interface StatusHelper {
    public Status getStatus();
    public void setStatus(Status status);
    
    public default boolean isAllChaptersCompleted() {
        return getStatus() == ALL_CHAPTERS_COMPLETED;
    }
    
    public default boolean isCompleted() {
        return getStatus() == COMPLETED;
    }
    public default boolean isFailed() {
        return getStatus() == FAILED;
    }
    public default boolean isQueued() {
        return getStatus() == QUEUED;
    }
    public default boolean isUntouched() {
        return getStatus() == UNTOUCHED;
    }
    public default boolean isHalted() {
        return getStatus() == HALTED;
    }
    public default void setCompleted() {
        setStatus( COMPLETED);
    }
    public default void setFailed() {
        setStatus( FAILED);
    }
    public default void setQueued() {
        setStatus( QUEUED);
    }
    public default void setUntouched() {
        setStatus( UNTOUCHED);
    }
    public default void setHalted() {
        setStatus( HALTED);
    }
    
    public default void setRunning() {
        setStatus( RUNNING);
    }
    public default boolean isRunning() {
        return getStatus() == RUNNING;
    }
}
