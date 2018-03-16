package sam.manga.downloader;

import static sam.manga.downloader.extra.Status.COMPLETED;
import static sam.manga.downloader.extra.Status.FAILED;
import static sam.manga.downloader.extra.Status.HALTED;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.LinkedBlockingQueue;

import sam.manga.downloader.parts.Chapter;
import sam.manga.downloader.parts.Page;
import sam.myutils.myutils.MyUtils;

public class PageDownload implements Runnable {
    private static final LinkedBlockingQueue<WeakReference<byte[]>> buffers = new LinkedBlockingQueue<>(); 

    /**
     * 2 min
     */
    static int CONNECTION_TIMEOUT = 1000*60*2;
    /**
     * 5 min
     */
    static int READ_TIMEOUT = 1000*60*5;
    /**
     * @param page
     * @param tryWithAltUrl
     * @return
     */

    private final Page page;
    private final Chapter chapter;

    public PageDownload(Chapter chapter, Page page) {
        this.page = page;
        this.chapter = chapter;
    }
    @Override
    public void run() {
        if(page.isCompleted())
            return;

        if(page.isHalted())
            return;

        if(page.getImageUrl() == null){
            setStatus("Null Url", FAILED);
            return;
        }

        final Path savePath = getSavePath(chapter, page);

        try {
            if(Files.exists(savePath)){
                page.setCompleted();
                return;
            }

            URL url = new URL(page.getImageUrl());
            URLConnection con = open(url); 

            long length = con.getContentLengthLong();

            byte[] buffer = getBuffer();
            int nread = 0;

            try(InputStream is = con.getInputStream();
                    OutputStream os = Files.newOutputStream(savePath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                int n;
                while ((n = is.read(buffer)) > 0) {
                    os.write(buffer, 0, n);
                    nread += n;
                }
            } finally {
                buffers.offer(new WeakReference<byte[]>(buffer));
            } 

            if(nread < 1000L)
                setStatus("file size("+nread+") < 1000 bytes", HALTED);
            else if(length == -1)
                setStatus("Content Length = -1", HALTED);
            else if(nread != length)
                setStatus("file size ("+nread+")  != Content Length ("+length+") ", HALTED);

            if(page.isHalted())
                Files.move(savePath, DownloaderApp.HALTED_IMAGE_DIR.resolve(String.valueOf(page.getId())), StandardCopyOption.REPLACE_EXISTING);
            else {
                setStatus(null, COMPLETED);
                BytesCounter.addBytes(nread);
            }
        } catch (IOException | NullPointerException e) {
            try {
                Files.deleteIfExists(savePath);
            } catch (IOException e1) {}

            setStatus(MyUtils.exceptionToString(e), FAILED);
        }
    }
    private URLConnection open(URL url) throws IOException {
        URLConnection con = url.openConnection();
        con.setReadTimeout(READ_TIMEOUT);
        con.setConnectTimeout(CONNECTION_TIMEOUT);
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.29 Safari/537.36");
        return con;
    }
    private byte[] getBuffer() {
        WeakReference<byte[]> w;
        while((w = buffers.poll()) != null) {
            byte[] b = w.get();
            if(b != null)
                return b;
        }
        return new byte[256*1024];
    }
    private void setStatus(String error, Status status) {
        page.setError(error);
        page.setStatus(status);
    }
    public static Path getSavePath(Chapter chapter, Page page) {
        return chapter.getSavePath().resolve(String.valueOf(page.getOrder()));
    }
}
