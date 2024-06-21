import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class Server {
    private static Map<String, User> users = new HashMap<>();
    private static Map<String, Request> requestMap = new HashMap<>();
    private static Queue<Request> requestQueue = new LinkedList<>();
    private static RequestLinkedList requestLL = new RequestLinkedList();
    private static ExecutorService workerPool = Executors.newFixedThreadPool(5);
    private static boolean FCFS = false; // determining how queue functions.

    // List of worker IP addresses
    private static final List<String> WORKER_IPS = Arrays.asList(
        "131.217.174.39",
        "131.217.174.71",
        "131.217.175.6"
    );

    // Worker statuses
    private static Map<String, Boolean> workerStatuses = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        int port = 54507;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server started at port " + port);

        // Initialize worker statuses
        WORKER_IPS.forEach(ip -> workerStatuses.put(ip, true));

        // Start the heartbeat monitoring thread
        new Thread(Server::monitorWorkers).start();

        new Thread(Server::processRequests).start();

        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } finally {
            serverSocket.close();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream());

                String message;
                while ((message = inFromClient.readLine()) != null) {
                    System.out.println("Received request from client: " + message);
                    String response = handleClientMessage(message);
                    System.out.println("Sending to client: " + response.replace("\n", "\\n")); // Log the response in a readable way
                    outToClient.writeBytes(response + "<END>\n"); // Add a special marker to indicate the end of the response
                }
            } catch (IOException e) {
                System.out.println("Error handling client: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Error closing socket: " + e.getMessage());
                }
            }
        }

        private String handleClientMessage(String message) {
            String[] parts = message.split(" ");
            String command = parts[0];

            switch (command.toLowerCase()) {
                case "register":
                    return registerUser(parts);
                case "status":
                    return checkRequestStatus(parts);
                case "stop":
                    return cancelRequest(parts);
                case "submit":
                    return submitRequest(parts);
                case "login":
                    return loginRequest(parts);
                default:
                    return "Invalid command";
            }
        }

        private String loginRequest(String[] parts) {
            if (parts.length < 3) {
                return "Usage: login <username> <password>";
            }

            String username = parts[1];
            String password = parts[2];
            String code = "OOOGABOOGA";
            User user = users.get(username);

            if (user == null) {
                return "Invalid username";
            }

            if (!user.getPassword().equals(password)) {
                return "Incorrect password";
            }

            return "User " + username + " successfully logged-in" + code;
        }

        private String registerUser(String[] parts) {
            if (parts.length < 2) {
                return "Usage: register <username>";
            }

            String username = parts[1];
            User user = registerUser(username);
            return "User " + username + " registered successfully with password: " + user.getPassword();
        }

        private User registerUser(String username) {
            String password = UUID.randomUUID().toString();
            User user = new User(username, password);
            users.put(username, user);
            return user;
        }

        private String submitRequest(String[] parts) {
            if (parts.length < 4) {
                return "Usage: submit <username> <queryType> <directory>";
            }

            String username = parts[1];
            String queryType = parts[2];
            String dirPath = parts[3];
            String findWord = null;
            User user = users.get(username);

            if (user == null) {
                return "Invalid username";
            }

            String requestId = UUID.randomUUID().toString();
            Request.RequestType requestType;

            if (queryType.equalsIgnoreCase("list")) {
                requestType = Request.RequestType.LIST_UNIQUE_WORDS;
            } else if (queryType.equalsIgnoreCase("find")) {
                if (parts.length < 5) {
                    return "Usage: submit <username> find <directory> <word>";
                }
                requestType = Request.RequestType.FIND_DOCUMENTS;
                findWord = parts[4];
            } else {
                return "Invalid Request Type, please try again";
            }

            Request request = new Request(requestId, dirPath, user, LocalDateTime.now(),
                    null, Request.RequestStatus.PENDING, requestType, directoryFileCount(dirPath));
            requestMap.put(requestId, request);

            request.setFindWord(findWord);

            if (FCFS) {
                requestQueue.add(request);
            } else {
                requestLL.insert(request);
            }

            user.getRequests().add(request);

            System.out.println("Request " + requestId + " received from user " + username + " for " + requestType);

            return "Request submitted successfully with ID: " + requestId;
        }

        private long directoryFileCount(String dir) {
            Path directoryPath = Paths.get(dir);
            long fileCount = 0;
            try (Stream<Path> files = Files.walk(directoryPath)) {
                fileCount = files.filter(Files::isRegularFile).count();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return fileCount;
        }

        private String checkRequestStatus(String[] parts) {
            if (parts.length < 2) {
                return "Usage: status <request_id>";
            }

            String requestId = parts[1];
            Request request = requestMap.get(requestId);

            if (request == null) {
                return "Request not found";
            }

            if (request.getStatus() == Request.RequestStatus.COMPLETED) {
                return request.getMessage(); // Return the stored message if the request is completed
            } else {
                return "Request ID: " + request.getRequestId() + "\nStatus: " + request.getStatus();
            }
        }

        private String cancelRequest(String[] parts) {
            if (parts.length < 2) {
                return "Usage: stop <request_id>";
            }

            String requestId = parts[1];
            cancelRequest(requestId);
            return "Request " + requestId + " cancelled";
        }

        private void cancelRequest(String requestId) {
            Request request = requestMap.get(requestId);
            if (request != null && request.getStatus() == Request.RequestStatus.PENDING) {
                request.setStatus(Request.RequestStatus.CANCELLED);
                if (FCFS) {
                    requestQueue.remove(request);
                } else {
                    requestLL.remove(requestId);
                }
            }
        }
    }

    private static void processRequests() {
        while (true) {
            Request request = FCFS ? requestQueue.poll() : requestLL.poll();
            if (request != null && request.getStatus() == Request.RequestStatus.PENDING) {
                System.out.println("Sending request " + request.getRequestId() + " to worker for processing at " + LocalDateTime.now());
                distributeWorkload(request);
            } else {
                try {
                    Thread.sleep(100); // Avoid busy-waiting
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static void distributeWorkload(Request request) {
        try {
            List<String> filePaths = getFilePaths(request.getDirPath());
            int numFiles = filePaths.size();
            int numWorkers = WORKER_IPS.size();
            int assignedFiles = 0;

            List<FutureTaskWrapper> futureTaskWrappers = new ArrayList<>();
            ExecutorService tempWorkerPool = Executors.newFixedThreadPool(numWorkers);

            for (int i = 0; i < numWorkers; i++) {
                if (!checkWorkerConnection(WORKER_IPS.get(i))) {
                    workerStatuses.put(WORKER_IPS.get(i), false);
                    System.out.println("Worker " + WORKER_IPS.get(i) + " is down.");
                    continue;
                }
                workerStatuses.put(WORKER_IPS.get(i), true);
                
                int start = assignedFiles;
                int end = Math.min(start + (numFiles / (numWorkers - i)), numFiles); // dynamically adjust number of files
                if (start >= end) break; // No more files to assign
                
                List<String> subList = filePaths.subList(start, end);
                WorkerTask workerTask = new WorkerTask(request, subList, WORKER_IPS.get(i));
                FutureTask<Set<String>> futureTask = new FutureTask<>(workerTask);
                futureTaskWrappers.add(new FutureTaskWrapper(futureTask, workerTask));
                tempWorkerPool.submit(futureTask);

                assignedFiles += (end - start); // increment the assigned files count
            }

            // Handle unassigned files
            if (assignedFiles < numFiles) {
                System.out.println("Some files were not assigned to any worker. Redistributing...");
                List<String> unassignedFiles = filePaths.subList(assignedFiles, numFiles);
                redistributeUnassignedFiles(request, unassignedFiles);
            }

            StringBuilder result = new StringBuilder();

            if (request.getType() == Request.RequestType.LIST_UNIQUE_WORDS) {
                try {
                    int uniqueCount = countUniqueStrings(futureTaskWrappers);
                    result.append(uniqueCount);
                    System.out.println("\n Number of unique strings: " + uniqueCount);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                } finally {
                    result.append(": Unique Words.\n");
                }
            } else {
                for (FutureTaskWrapper wrapper : futureTaskWrappers) {
                    try {
                        Set<String> resultSet = wrapper.getFutureTask().get();
                        result.append(resultSet.iterator().next());
                    } catch (ExecutionException e) {
                        // Handle worker failure by redistributing the task
                        System.out.println("Worker failed: " + e.getMessage());
                        redistributeTask(request, futureTaskWrappers, wrapper);
                    }
                }
                result.append(". Are the Documents which contain the word: ").append(request.getFindWord());
            }

            request.setEndTime(LocalDateTime.now());
            request.setStatus(Request.RequestStatus.COMPLETED);
            request.setResult(result.toString());
            generateBill(request);

            // Set the completed message
            String message = "Request ID: " + request.getRequestId() + "\n" +
                    "Status: COMPLETED " + "\n" +
                    "Result: " + request.getResult() + "\n" +
                    "Bill: $" + request.getBill();
            request.setMessage(message);

            System.out.println("Request " + request.getRequestId() + " completed and processed by worker.");

            tempWorkerPool.shutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void redistributeTask(Request request, List<FutureTaskWrapper> futureTaskWrappers, FutureTaskWrapper failedTaskWrapper) {
        // Identify the failed task
        List<String> failedSubList = failedTaskWrapper.getWorkerTask().getFilePaths();

        // Reassign the failed task to another worker
        for (String workerIp : WORKER_IPS) {
            if (workerStatuses.get(workerIp)) {
                try {
                    ExecutorService singleWorkerPool = Executors.newSingleThreadExecutor();
                    WorkerTask workerTask = new WorkerTask(request, failedSubList, workerIp);
                    FutureTask<Set<String>> futureTask = new FutureTask<>(workerTask);
                    singleWorkerPool.submit(futureTask);
                    Set<String> resultSet = futureTask.get(); // Wait for the result
                    singleWorkerPool.shutdown();
                    break; // Task reassigned successfully
                } catch (Exception e) {
                    // Continue to next worker
                    System.out.println("Failed to reassign task to worker: " + workerIp + ". Trying next worker...");
                }
            }
        }
    }

    private static void redistributeUnassignedFiles(Request request, List<String> unassignedFiles) {
        for (String workerIp : WORKER_IPS) {
            if (workerStatuses.get(workerIp)) {
                try {
                    ExecutorService singleWorkerPool = Executors.newSingleThreadExecutor();
                    WorkerTask workerTask = new WorkerTask(request, unassignedFiles, workerIp);
                    FutureTask<Set<String>> futureTask = new FutureTask<>(workerTask);
                    singleWorkerPool.submit(futureTask);
                    Set<String> resultSet = futureTask.get(); // Wait for the result
                    singleWorkerPool.shutdown();
                    break; // Task reassigned successfully
                } catch (Exception e) {
                    // Continue to next worker
                    System.out.println("Failed to assign unassigned files to worker: " + workerIp + ". Trying next worker...");
                }
            }
        }
    }

    private static void monitorWorkers() {
        while (true) {
            for (String workerIp : WORKER_IPS) {
                if (!checkWorkerHeartbeat(workerIp)) {
                    workerStatuses.put(workerIp, false);
                    System.out.println("Worker " + workerIp + " is down.");
                } else {
                    workerStatuses.put(workerIp, true);
                }
            }
            try {
                Thread.sleep(5000); // Check every 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static boolean checkWorkerConnection(String workerIp) {
        try (Socket socket = new Socket(workerIp, 9998)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean checkWorkerHeartbeat(String workerIp) {
        try (Socket socket = new Socket(workerIp, 9998);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.writeBytes("heartbeat\n");
            String response = in.readLine();
            return "alive".equals(response);
        } catch (IOException e) {
            return false;
        }
    }

    public static int countUniqueStrings(List<FutureTaskWrapper> futureTaskWrappers) throws InterruptedException, ExecutionException {
        Set<String> uniqueStrings = new HashSet<>();

        for (FutureTaskWrapper wrapper : futureTaskWrappers) {
            // Retrieve the result from the FutureTask
            Set<String> resultSet = wrapper.getFutureTask().get();
            // Add all elements from the result set to the unique set
            uniqueStrings.addAll(resultSet);
        }

        // The size of the set represents the number of unique strings
        return uniqueStrings.size();
    }

    private static List<String> getFilePaths(String dirPath) throws IOException {
        List<String> filePaths = new ArrayList<>();
        Path directoryPath = Paths.get(dirPath);
        try (Stream<Path> files = Files.walk(directoryPath)) {
            files.filter(Files::isRegularFile).forEach(path -> filePaths.add(path.toString()));
        }
        return filePaths;
    }

    private static class WorkerTask implements Callable<Set<String>> {
        private Request request;
        private List<String> filePaths;
        private String workerIp;

        public WorkerTask(Request request, List<String> filePaths, String workerIp) {
            this.request = request;
            this.filePaths = filePaths;
            this.workerIp = workerIp;
        }

        @Override
        public Set<String> call() throws Exception {
            Set<String> response = new HashSet<>();
            try {
                Socket workerSocket = new Socket(workerIp, 9998);
                DataOutputStream outToWorker = new DataOutputStream(workerSocket.getOutputStream());
                InputStream inFromWorker = new DataInputStream(workerSocket.getInputStream());
                ObjectInputStream objectInputStream = new ObjectInputStream(inFromWorker);

                outToWorker.writeBytes(request.getType().name().toLowerCase() + "\n");

                if (request.getType() == Request.RequestType.FIND_DOCUMENTS) {
                    outToWorker.writeBytes(request.getFindWord() + "\n");
                }

                for (String filePath : filePaths) {
                    outToWorker.writeBytes(filePath + "\n");
                }

                outToWorker.writeBytes("\n");

                try {
                    @SuppressWarnings("unchecked")
                    Set<String> set = (Set<String>) objectInputStream.readObject();
                    response = set;
                } catch (EOFException e) {
                    // End of stream reached
                }

                workerSocket.close();
            } catch (IOException e) {
                throw new RuntimeException("Worker communication failed", e);
            }
            return response;
        }

        public List<String> getFilePaths() {
            return filePaths;
        }
    }

    private static class FutureTaskWrapper {
        private final FutureTask<Set<String>> futureTask;
        private final WorkerTask workerTask;

        public FutureTaskWrapper(FutureTask<Set<String>> futureTask, WorkerTask workerTask) {
            this.futureTask = futureTask;
            this.workerTask = workerTask;
        }

        public FutureTask<Set<String>> getFutureTask() {
            return futureTask;
        }

        public WorkerTask getWorkerTask() {
            return workerTask;
        }
    }

    private static void generateBill(Request request) {
        long duration = Duration.between(request.getStartTime(), request.getEndTime()).toSeconds();
        double cost = duration * 0.10; // Imaginary cost per second
        request.setBill(cost);
    }
}
