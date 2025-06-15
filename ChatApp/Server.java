import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private ServerSocket serverSocket;
    private HashMap<Integer, ClientHandler> clients = new HashMap<>();
    private int clientID = 1;
    private boolean isRunning = true;

    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            // Server input handling for exit
            new Thread(this::handleServerCommands).start();

            while (isRunning) {
                Socket socket = serverSocket.accept();
                if (!isRunning) break;  // If server is stopping, don't accept new clients

                System.out.println("New Client connected: " + clientID);

                ClientHandler handler = new ClientHandler(socket, clientID, this);
                clients.put(clientID, handler);
                new Thread(handler).start();

                clientID++;
            }
        } catch (IOException e) {
            System.out.println("Server stopped.");
        }
    }

    // **Private Message to Specific Client**
    public synchronized void sendMessage(int clientID, String message, int senderID) {
        if (clients.containsKey(clientID)) {
            clients.get(clientID).sendMessage("Private from Client " + senderID + ": " + message);
            clients.get(senderID).sendMessage("Private message sent to Client " + clientID);
        } else {
            clients.get(senderID).sendMessage("Client ID " + clientID + " not found!");
        }
    }

    // **Broadcast Message to All Clients (Except Sender)**
    public synchronized void broadcastMessage(String message, int senderID) {
        for (Map.Entry<Integer, ClientHandler> entry : clients.entrySet()) {
            if (entry.getKey() != senderID) {
                if (senderID == -1) {
                    entry.getValue().sendMessage("" + message);
                } else {
                    entry.getValue().sendMessage("Client " + senderID + ": " + message);
                }
            }
        }
    }

    // **Remove Disconnected Client**
    public synchronized void removeClient(int clientID) {
        clients.remove(clientID);
        System.out.println("Client " + clientID + " disconnected.");
        broadcastMessage("Client " + clientID + " has left the chat.", -1);
    }

    // **Handle Server Exit and Commands**
    private void handleServerCommands() {
        Scanner scanner = new Scanner(System.in);
        while (isRunning) {
            String command = scanner.nextLine();
            if (command.equalsIgnoreCase("exit")) {
                shutdownServer();
            } else if (command.equalsIgnoreCase("/list")) {
                System.out.println("Connected Clients: " + getClientList());
            } else {
                broadcastMessage("Server: " + command, -1);
            }
        }
        scanner.close();
    }

    // **Shutdown Server and Notify Clients**
    private synchronized void shutdownServer() {
        System.out.println("Shutting down server...");
        isRunning = false;

        for (ClientHandler client : clients.values()) {
            client.sendMessage("Server is shutting down. You will be disconnected.");
            client.disconnect();
        }

        clients.clear();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public synchronized String getClientList() {
        return clients.keySet().toString();
    }

    public static void main(String[] args) {
        new Server(5000);
    }
}
class ClientHandler implements Runnable {
    private Socket socket;
    private int clientID;
    private Server server;
    private BufferedReader reader;
    private PrintWriter writer;

    public ClientHandler(Socket socket, int clientID, Server server) {
        this.socket = socket;
        this.clientID = clientID;
        this.server = server;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            sendMessage("Connected! Your client ID is " + clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        writer.println(message);
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                if (message.equalsIgnoreCase("exit")) {
                    System.out.println("Client " + clientID + " exited.");
                    server.removeClient(clientID);
                    socket.close();
                    break;
                } else if (message.startsWith("@")) {
                    String[] parts = message.split(" ", 2);
                    if (parts.length < 2) continue;
                    int targetID = Integer.parseInt(parts[0].substring(1));
                    String privateMsg = parts[1];
                    server.sendMessage(targetID, privateMsg, clientID);
                } else if (message.equalsIgnoreCase("/list")) {
                    sendMessage("Connected Clients: " + server.getClientList());
                } else {
                    System.out.println("Client " + clientID + ": " + message);
                    server.broadcastMessage(message, clientID);
                }
            }
        } catch (IOException e) {
            System.out.println("Client " + clientID + " disconnected.");
        }
    }
}


