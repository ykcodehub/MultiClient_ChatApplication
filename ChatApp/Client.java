import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public Client(String address, int port) {
        try {
            socket = new Socket(address, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            // Thread for receiving messages
            new Thread(new ReceiveMessage()).start();

            Scanner scanner = new Scanner(System.in);
            System.out.println("Connected to Server!");

            while (true) {
                String message = scanner.nextLine();

                if (message.equalsIgnoreCase("exit")) {
                    writer.println("exit");
                    System.out.println("Disconnected.");
                    break;
                }
                writer.println(message);
            }

            socket.close();
            scanner.close();
        } catch (IOException e) {
            System.out.println("⚠ Server closed. Exiting...");
        }
    }

    class ReceiveMessage implements Runnable {
        public void run() {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    System.out.println(message);
                    if (message.contains("Server is shutting down")) {
                        System.out.println("Server stopped. Disconnecting...");
                        System.exit(0);
                    }
                }
            } catch (IOException e) {
                System.out.println("Connection lost.");
                System.exit(0);
            }
        }
    }

    public static void main(String[] args) {
        new Client("127.0.0.1", 5000);
    }
}
