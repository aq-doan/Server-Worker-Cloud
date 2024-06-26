
# PaaS Cloud-Based Weather Service

This project implements a PaaS cloud-based weather service involving a DQS server and client interaction. The components include the client, server, worker, user, and request handling.

## Prerequisites


## Files

- `Client.java`: Handles client-side operations and requests.
- `Request.java`: Represents the structure of a request.
- `Server.java`: Manages server-side operations and coordinates with workers.
- `Worker.java`: Processes individual requests.
- `User.java`: Manages user information and authentication.

## Instructions

### Step 1: Compile the Java Files

1. Open a terminal or command prompt.
2. Navigate to the directory containing the Java files.
3. Compile the Java files using the following command:

```bash
javac *.java
```

### Step 2: Run the Server and Worker in separate terminate or vm machines (need to change to localhost or the ip address)

1. Start the server by running the `Server` class. Use the following command:

```bash
java Server
java Worker

```

### Step 3: Run the Client

1. In a new terminal or command prompt, navigate to the directory containing the compiled Java files.
2. Start the client by running the `Client` class. Use the following command:

```bash
java Client
```

### Step 4: Interact with the Service

- Follow the prompts on the client-side to interact with the weather service.
- The client will send requests to the server, and the server will process these requests using the workers.

## Additional Notes

- Ensure that the server is running before starting the client.
- You may need to configure network settings (e.g., port numbers) in the source code if there are conflicts or specific requirements.

