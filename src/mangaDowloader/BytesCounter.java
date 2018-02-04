package mangaDowloader;

import java.io.IOException;
import java.nio.file.Files;

import javafx.application.Platform;
import javafx.scene.text.Text;
import sam.myutils.fileutils.FilesUtils;
import sam.myutils.myutils.MyUtils;

public class BytesCounter {
	private static volatile long TOTAL_BYTES = 0;
	public static final Text COUNTER_VIEW = new Text("0000000");
	
	private BytesCounter() {}
	
	public static synchronized void addBytes(long bytes){
		TOTAL_BYTES += bytes;
		set();
	}
	
	private static synchronized void set() {
		Platform.runLater(() -> COUNTER_VIEW.setText(MyUtils.bytesToHumanReadableUnits(TOTAL_BYTES)));
	}

	public synchronized static void reset() {
		new Thread(() -> {
			if(Files.exists(DownloaderApp.DOWNLOAD_DIR)){
				try {
					TOTAL_BYTES = FilesUtils.listDirsFiles(DownloaderApp.DOWNLOAD_DIR).get(FilesUtils.ListingKeys.FILES).stream().mapToLong(p -> {
						try {
							return Files.size(p);
						} catch (IOException e) {}
						return 0;
					}).sum();
				} catch (IOException e) {}
			}
			else
				TOTAL_BYTES = 0;
			set();
		}).start();
	}
}
