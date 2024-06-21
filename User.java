import java.util.ArrayList;
import java.util.List;

public class User {
    private String username;
    private String password;
    private List<Request> requests;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.requests = new ArrayList<>();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public List<Request> getRequests() {
        return requests;
    }
}
