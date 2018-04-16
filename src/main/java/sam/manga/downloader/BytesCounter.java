package sam.manga.downloader;

import static sam.manga.downloader.extra.Utils.DOWNLOAD_DIR;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import javafx.application.Platform;
import javafx.scene.text.Text;
import sam.myutils.MyUtils; 

public class BytesCounter {
	private static final AtomicLong TOTAL_BYTES = new AtomicLong();
	public static final Text COUNTER_VIEW = new Text("0000000");
	
	private BytesCounter() {}
	
	public static synchronized void addBytes(long bytes){
	    set(TOTAL_BYTES.addAndGet(bytes));
	}
	private static void set(long n) {
	    Platform.runLater(() -> COUNTER_VIEW.setText(MyUtils.bytesToHumanReadableUnits(n, false, new StringBuilder()).toString()));
	}
	public synchronized static void reset() {
		MyUtils.runOnDeamonThread(() -> {
            if(Files.exists(DOWNLOAD_DIR)){
                try {
                     long l = Files.walk(DOWNLOAD_DIR)
                     .map(Path::toFile)
                     .filter(File::isFile)
                     .mapToLong(File::length)
                     .sum();
                     TOTAL_BYTES.set(l);
                } catch (IOException e) {}
            }
            else
                TOTAL_BYTES.set(0);
            set(TOTAL_BYTES.get());
        });
	}
}
