import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("131.217.172.17", 54507);
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String currentUser = null;
        boolean loggedin = false;

        try {
            while (true) {

                if (!loggedin) {
                    System.out.println("1: Register");
                    System.out.println("2: Login");
                    System.out.println("3: Exit");
                    System.out.print("Enter your choice: ");
                } else {
                    System.out.println("1: Logout");
                    System.out.println("2: Check Request Status");
                    System.out.println("3: Stop Request");
                    System.out.println("4: List all unique words");
                    System.out.println("5: Find documents containing a word");
                    System.out.println("6: Exit");
                    System.out.print("Enter your choice: ");
                }

                String choice = consoleReader.readLine();

                String message = "";
                if (!loggedin) {
                    switch (choice) {
                        case "1":
                            System.out.print("Enter username: ");
                            String username = consoleReader.readLine();
                            message = "register " + username;
                            break;
                        case "2":
                            System.out.print("Enter username: ");
                            username = consoleReader.readLine();
                            currentUser = username;
                            System.out.print("Enter Password: ");
                            String password = consoleReader.readLine();
                            message = "login " + username + " " + password;
                            break;
                        case "3":
                            message = "exit";
                            outToServer.writeBytes(message + "\n");
                            return;
                        default:
                            System.out.println("Invalid choice. Please try again.");
                            continue;
                    }

                } else {
                    switch (choice) {
                        case "1":
                            loggedin = false;
                            break;
                        case "2":
                            System.out.print("Enter request ID: ");
                            String requestId = consoleReader.readLine();
                            message = "status " + requestId;
                            break;
                        case "3":
                            System.out.print("Enter request ID: ");
                            requestId = consoleReader.readLine();
                            message = "stop " + requestId;
                            break;
                        case "4":
                            String username = currentUser;
                            System.out.print("Enter directory path: ");
                            String dirPath = consoleReader.readLine();
                            message = "submit " + username + " list " + dirPath;
                            break;
                        case "5":
                            username = currentUser;
                            System.out.print("Enter directory path: ");
                            dirPath = consoleReader.readLine();
                            System.out.print("Enter word to find: ");
                            String word = consoleReader.readLine();
                            message = "submit " + username + " find " + dirPath + " " + word;
                            break;
                        case "6":
                            message = "exit";
                            outToServer.writeBytes(message + "\n");
                            return;
                        default:
                            System.out.println("Invalid choice. Please try again.");
                            continue;
                    }
                }

                outToServer.writeBytes(message + "\n");

                // Read the multiline response from the server
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = inFromServer.readLine()) != null) {
                    if (line.endsWith("<END>")) {
                        response.append(line, 0, line.length() - 5); // Remove <END> marker
                        break;
                    } else {
                        response.append(line).append("\n");
                    }
                }

                String code = "OOOGABOOGA"; // code for user successfully logged in
                if (response.toString().contains(code)) {
                    loggedin = true;
                    response = new StringBuilder(response.toString().replace(code, ""));
                }

                System.out.println("FROM SERVER: " + response.toString().trim());
            }
        } finally {
            consoleReader.close();
            outToServer.close();
            inFromServer.close();
            socket.close();
        }
    }
}
