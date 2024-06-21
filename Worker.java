import java.io.*;
import java.net.*;
import java.util.*;

public class Worker {
    public static void main(String[] args) throws Exception {
        try {
            ServerSocket server = new ServerSocket(9998);
            int counter = 0;
            System.out.println("Worker Started ....");
            while (true) {
                counter++;
                Socket serverClient = server.accept();
                System.out.println(" >> " + "Client No:" + counter + " started!");
                WorkerClientThread wct = new WorkerClientThread(serverClient, counter);
                wct.start();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}

class WorkerClientThread extends Thread {
    Socket serverClient;
    int clientNo;
    BufferedReader inFromServer;
    DataOutputStream outToServer;
    ObjectOutputStream objectOutputStream;

    WorkerClientThread(Socket inSocket, int counter) {
        serverClient = inSocket;
        clientNo = counter;
    }

    public void run() {
        try {
            inFromServer = new BufferedReader(new InputStreamReader(serverClient.getInputStream()));
            outToServer = new DataOutputStream(serverClient.getOutputStream());
            objectOutputStream = new ObjectOutputStream(outToServer);
            String findWord = null;


            String requestType = inFromServer.readLine(); // get request type
            System.out.print(requestType);

            if(requestType.equalsIgnoreCase("find_documents")){
                findWord = inFromServer.readLine(); // get word to find
            }

            List<String> filePaths = new ArrayList<>();
            String line;
            while ((line = inFromServer.readLine()) != null && !line.isEmpty()) {
                filePaths.add(line); // get file directories
            }
            System.out.println("Received request: " + requestType + " from Client No:" + clientNo + " for files: " + filePaths);

            Set<String> response = new HashSet<>();

            switch (requestType.toLowerCase()) {
                case "list_unique_words":
                    response = listAllUniqueWords(filePaths);
                    break;
                case "find_documents":    
                    response = findDocumentsContainingWord(filePaths, findWord);
                    break;
                default:
                    //response = "Invalid request type";
            }

            System.out.println("Sending response: " + "Set<String> sent.\n");
            //outToServer.writeBytes(response + "\n");
            objectOutputStream.writeObject(response);
            serverClient.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private Set<String> listAllUniqueWords(List<String> filePaths) {
        Set<String> uniqueWords = new HashSet<>();

        for (String filePath : filePaths) {
            File file = new File(filePath);
            if (file.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] words = line.split("\\W+");
                        for (String word : words) {
                            if (!word.isEmpty()) {
                                uniqueWords.add(word.toLowerCase());
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return uniqueWords;//"Number of unique words: " + uniqueWords.size();
    }

    private Set<String> findDocumentsContainingWord(List<String> filePaths, String wordToFind) {
        List<String> documents = new ArrayList<>();

        for (String filePath : filePaths) {
            File file = new File(filePath);
            if (file.isFile() && containsWord(file, wordToFind)) {
                documents.add(file.getName());
            }
        }
        Set<String> out = new HashSet<>();
        String outString = String.join(" ", documents);
        out.add(outString);
        return out;
    }

    private boolean containsWord(File file, String wordToFind) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(wordToFind)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
