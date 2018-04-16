package sam.manga.downloader.extra;

public enum Status{
	RUNNING,
	COMPLETED,
	QUEUED, 
	UNTOUCHED,
	FAILED,
	HALTED,
	ALL_CHAPTERS_COMPLETED;
	
	final String classString;
	private Status() {
		classString = toString().toLowerCase();
	}
	
	/**
	 * @return toString().toLowerCase()
	 */
	public String getClassName() {
		return classString;
	}
	
	public static Status parse(String s) {
        if(s == null)
            return null;
        return Status.valueOf(s);
    }
}