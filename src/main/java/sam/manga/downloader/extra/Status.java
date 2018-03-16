package sam.manga.downloader.extra;

public enum Status{
	RUNNING,
	COMPLETED,
	QUEUED, 
	UNTOUCHED,
	FAILED,
	HALTED,
	ALL_CHAPTERS_COMPLETED;
	
	private final String classString;
	private Status() {
		classString = toString().toLowerCase();
	}
	
	/**
	 * @return toString().toLowerCase()
	 */
	public String getClassName() {
		return classString;
	}
}