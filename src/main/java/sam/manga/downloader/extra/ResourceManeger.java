package sam.manga.downloader.extra;

public class ResourceManeger {
    private static volatile ResourceManeger instance;

    public static ResourceManeger getInstance() {
        if (instance == null) {
            synchronized (ResourceManeger.class) {
                if (instance == null)
                    instance = new ResourceManeger();
            }
        }
        return instance;
    }

    private ResourceManeger() {}
    
    
}
