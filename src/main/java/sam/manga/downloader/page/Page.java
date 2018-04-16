package sam.manga.downloader.page;

import sam.manga.downloader.data.DataManager;
import sam.manga.downloader.extra.Status;
import sam.manga.downloader.extra.StatusHelper;
import sam.manga.scrapper.units.PageBase;

public class Page extends PageBase implements StatusHelper {
   public final int pageId;
   public final int chapterId;
    private volatile Status status;
    private String error;
    private boolean modified;

    public Page(int pageId, int chapterId, int order, String image_url, String page_url, Status status, String error) {
        super(order, page_url, image_url);
        this.pageId = pageId;
        this.chapterId = chapterId;
        this.status = status;
        this.error = error;
    }
    
    public Page(int chapterId, int order, String page_url, String image_url) {
        this(DataManager.newId(Page.class),chapterId, order, image_url, page_url, Status.UNTOUCHED, null);
    }
    
    @Override
    public Status getStatus() { return status; }
    @Override
    public void setStatus(Status status) {
        modified = true;
        this.status = status;
    }
    
    public void setError(String error) {
        modified = true;
        this.error = error;
    }
    
    public String getError() { return error; }
    public boolean hasError() { return error != null; }
    public boolean isModified() { return modified; }
}
