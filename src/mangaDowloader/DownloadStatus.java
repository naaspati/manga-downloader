package mangaDowloader;

enum DownloadStatus{
	RUNNING,
	COMPLETED,
	QUEUED, 
	UNTOUCHED,
	FAILED,
	HALTED,
	ALL_CHAPTERS_COMPLETED;
	
	private final String classString;
	private DownloadStatus() {
		classString = toString().toLowerCase();
	}
	
	boolean isAllChaptersCompleted() {
		return this == ALL_CHAPTERS_COMPLETED;
	}
	
	boolean isCompleted() {
		return this == COMPLETED;
	}
	
	boolean isFailed() {
		return this == FAILED;
	}
	
	boolean isQueued() {
		return this == QUEUED;
	}
	
	boolean isUntouched() {
		return this == UNTOUCHED;
	}
	
	boolean isHalted() {
		return this == HALTED;
	}
	
	/**
	 * @return toString().toLowerCase()
	 */
	String getClassName() {
		return classString;
	}
}