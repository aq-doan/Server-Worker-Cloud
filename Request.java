import java.time.LocalDateTime;

public class Request {
    public enum RequestStatus {
        PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    }

    public enum RequestType {
        LIST_UNIQUE_WORDS, FIND_DOCUMENTS
    }

    private String requestId;
    private String dirPath;
    private String findWord;
    private User user;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private RequestStatus status;
    private RequestType type;
    private String result;
    private long numFiles;
    private double bill;
    private String message; // New field to store the message

    public Request(String requestId, String dirPath, User user, LocalDateTime startTime, LocalDateTime endTime, RequestStatus status, RequestType type, long numFiles) {
        this.requestId = requestId;
        this.dirPath = dirPath;
        this.user = user;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.type = type;
        this.result = "";
        this.numFiles = numFiles;
        this.bill = 0.0;
        this.message = ""; // Initialize the message
    }

    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }

    public String getDirPath() {
        return dirPath;
    }

    public User getUser() {
        return user;
    }

    public String getFindWord(){
        return findWord;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public RequestType getType() {
        return type;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public double getBill() {
        return bill;
    }

    public void setFindWord(String findWord){
        this.findWord = findWord;
    }

    protected long getNumFiles() {
        return numFiles;
    }

    public void setBill(double bill) {
        this.bill = bill;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
