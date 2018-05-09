package sam.manga.downloader.data;

public class ProcessResult {
    private final boolean failed;
    private final String errorTitle;
    private String errorBody;
    private Throwable exception;
    
    public ProcessResult(boolean failed, String errorTitle) {
        this.failed = failed;
        this.errorTitle = errorTitle;
    }
    ProcessResult setErrorBody(String errorBody) {
        this.errorBody = errorBody;
        return this;
    }
    ProcessResult setErrorBody(String errorBody, Throwable e) {
        this.errorBody = errorBody;
        this.exception = e;
        return this;
    }
    
    public Throwable getException() {
        return exception;
    }
    public boolean isFailed() {
        return failed;
    }
    public String getErrorTitle() {
        return errorTitle;
    }

    public String getErrorBody() {
        return errorBody;
    }
}
