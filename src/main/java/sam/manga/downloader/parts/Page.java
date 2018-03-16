package sam.manga.downloader.parts;

import sam.manga.downloader.extra.Status;
import sam.manga.downloader.extra.StatusHelper;

public class Page implements StatusHelper {
    final int id;
    private final int order;
    final String image_url;
    final String page_url;
    private volatile Status status;
    private String error;
    private boolean modified;
    
    public Page(int id, int order, String image_url, String page_url, Status status, String error) {
        this.id = id;
        this.order = order;
        this.image_url = image_url;
        this.page_url = page_url;
        this.status = status;
        this.error = error;
    }
    @Override
    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        modified = true;
        this.status = status;
    }
    public String getError() {
        return error;
    }
    public void setError(String error) {
        modified = true;
        this.error = error;
    }
    public int getOrder() {
        return order;
    }
    public String getImageUrl() {
        return image_url;
    }
    public int getId() {
        return id;
    }
    public String getPageUrl() {
        return page_url;
    }
    public boolean hasError() {
        return error != null;
    }
    public boolean isModified() {
        return modified;
    }
}
