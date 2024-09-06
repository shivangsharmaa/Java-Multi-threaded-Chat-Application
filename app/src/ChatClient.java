import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String username;

    public ChatClient(Socket socket) {
        try {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            closeEverything(socket, reader, writer);
        }
    }

    public void startClient() {
        Scanner scanner = new Scanner(System.in);
        
        // Add a registration or login prompt
        System.out.println("Welcome to the Chat Application!");
        System.out.println("1. Login");
        System.out.println("2. Register");
        System.out.print("Choose an option: ");
        int choice = scanner.nextInt();
        scanner.nextLine();  // Consume newline

        if (choice == 1) {
            login(scanner);
        } else if (choice == 2) {
            register(scanner);
            login(scanner);  // Automatically login after registration
        } else {
            System.out.println("Invalid choice. Exiting...");
            scanner.close();
            return;
        }

        // After login, start listening for messages and sending messages
        listenForMessages();
        sendMessage(scanner);
    }

    private void login(Scanner scanner) {
        try {
            System.out.print("Enter your username: ");
            username = scanner.nextLine();
            System.out.print("Enter your password: ");
            String password = scanner.nextLine();

            writer.write("LOGIN " + username + " " + password);
            writer.newLine();
            writer.flush();

            String response = reader.readLine();
            if ("SUCCESS".equals(response)) {
                System.out.println("Login successful! Welcome, " + username);
            } else {
                System.out.println("Login failed: " + response);
                System.exit(0);
            }
        } catch (IOException e) {
            closeEverything(socket, reader, writer);
        }
    }

    private void register(Scanner scanner) {
        try {
            System.out.print("Choose a username: ");
            String newUsername = scanner.nextLine();
            System.out.print("Choose a password: ");
            String newPassword = scanner.nextLine();

            writer.write("REGISTER " + newUsername + " " + newPassword);
            writer.newLine();
            writer.flush();

            String response = reader.readLine();
            if ("REGISTRATION SUCCESS".equals(response)) {
                System.out.println("Registration successful! You can now log in with your new credentials.");
            } else {
                System.out.println("Registration failed: " + response);
                System.exit(0);
            }
        } catch (IOException e) {
            closeEverything(socket, reader, writer);
        }
    }

    public void listenForMessages() {
        new Thread(() -> {
            String msgFromGroupChat;
            while (socket.isConnected()) {
                try {
                    msgFromGroupChat = reader.readLine();
                    System.out.println(msgFromGroupChat);
                } catch (IOException e) {
                    closeEverything(socket, reader, writer);
                }
            }
        }).start();
    }

    public void sendMessage(Scanner scanner) {
        while (socket.isConnected()) {
            try {
                System.out.println("Do you want to send a public or private message?");
                System.out.println("1. Public Message");
                System.out.println("2. Private Message");
                int choice = scanner.nextInt();
                scanner.nextLine();  // Consume newline
    
                String messageToSend;
                if (choice == 1) {
                    System.out.println("Enter your message:");
                    messageToSend = scanner.nextLine();
                    writer.write("PUBLIC " + messageToSend);
                    writer.newLine();
                    writer.flush();
                } else if (choice == 2) {
                    System.out.println("Enter the recipient's username:");
                    String recipient = scanner.nextLine();
                    System.out.println("Enter your message:");
                    String privateMessage = scanner.nextLine();
                    writer.write("PRIVATE " + recipient + " " + privateMessage);
                    writer.newLine();
                    writer.flush();
                } else {
                    System.out.println("Invalid choice. Try again.");
                }
            } catch (IOException e) {
                closeEverything(socket, reader, writer);
            }

        }
    }
    

    public void closeEverything(Socket socket, BufferedReader reader, BufferedWriter writer) {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 1234);
        ChatClient client = new ChatClient(socket);
        client.startClient();
    }
}
