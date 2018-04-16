package sam.manga.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;




class DownloadablePage {

	final int ID;
	final int ORDER;
	//final int CHAPTER_ID;
	final String IMAGE_URL;
	final String PAGE_URL;
	private DownloadStatus downloadStatus;
	private String error; 

	DownloadablePage(ResultSet rs) throws SQLException, IllegalArgumentException{
		ID = rs.getInt("id");;
		ORDER = rs.getInt("_order");
		PAGE_URL = rs.getString("page_url");
		//CHAPTER_ID = rs.getInt("chapter_id");
		IMAGE_URL = rs.getString("url"); 
		downloadStatus = DownloadStatus.valueOf(rs.getString("status"));

		if(downloadStatus.isFailed() || downloadStatus.isHalted())
			error = rs.getString("errors");
	}
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
	boolean downloadPage(Path chapterSavePath) {
		if(downloadStatus.isCompleted())
			return true;

		if(downloadStatus.isHalted())
			return false;

		if(IMAGE_URL == null){
			error = "Null Url";
			downloadStatus = DownloadStatus.FAILED;
			return false;
		}

		final Path savePath = getSavePath(chapterSavePath);

		try {
			if(Files.exists(savePath)){
				setCompleted();
				return true;
			}

			URL url = new URL(IMAGE_URL);
			URLConnection con = url.openConnection();
			con.setReadTimeout(READ_TIMEOUT);
			con.setConnectTimeout(CONNECTION_TIMEOUT);
			con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.29 Safari/537.36");

			long conFileSize = con.getContentLengthLong();

			InputStream is = con.getInputStream(); 
			long fileSize = Files.copy(is, savePath, StandardCopyOption.REPLACE_EXISTING);
			is.close();

			if(fileSize < 1000L)
				setHalted("file size("+fileSize+") < 1000 bytes");
			else if(conFileSize == -1)
				setHalted("Content Length = -1");
			else if(fileSize != conFileSize)
				setHalted("file size ("+fileSize+")  != Content Length ("+conFileSize+") ");

			if(downloadStatus == DownloadStatus.HALTED){
				Files.move(savePath, HALTED_IMAGE_DIR.resolve(String.valueOf(ID)), StandardCopyOption.REPLACE_EXISTING);
				return false;
			}
			else {
				setCompleted();
				BytesCounter.addBytes(fileSize);
				return true;
			}
		} catch (IOException | NullPointerException e) {
			downloadStatus = DownloadStatus.FAILED;

			try {
				Files.deleteIfExists(savePath);
			} catch (IOException e1) {}

			error = errorToString(e);
			return false;
		}
	}
	private String errorToString(Exception e) {
		return e == null ? null : "["+e.getClass().getSimpleName()+"] "+e.getMessage();
	}
	public Path getSavePath(Path chapterSavePath){
		return chapterSavePath.resolve(String.valueOf(ORDER));
	}
	void setCompleted() {
		error = null;
		downloadStatus = DownloadStatus.COMPLETED;
	}
	private void setHalted(String error) {
		this.error = error;
		downloadStatus = DownloadStatus.HALTED;
	}
	DownloadStatus getDownloadStatus() {
		return downloadStatus;
	}
	boolean hasError(){
		return downloadStatus != DownloadStatus.COMPLETED && error != null;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Page:[id: ")
		.append(ID)
		.append(", order: ")
		.append(ORDER)
		.append(", status: ")
		.append(downloadStatus)
		.append(", url: ")
		.append(IMAGE_URL)
		.append("]");
		return builder.toString();
	}
	//"UPDATE Pages SET status = ?, errors = ? WHERE id = ?"
	public void databaseCommit(PreparedStatement resetPage) throws SQLException {
		resetPage.setString(1, downloadStatus.toString());
		resetPage.setString(2, error);
		resetPage.setInt(3, ID);
		resetPage.addBatch();
	}
	String getError() {
		return error;
	}

}
