import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.ArrayList;

public class ChatServer {

    static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    protected static Connection dbConnection;

    public static void main(String[] args) {
        try {
            try (ServerSocket serverSocket = new ServerSocket(1234)) {
                System.out.println("Server is listening on port 1234...");
                // Establish database connection
                dbConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/chat_db", "sqlUser", "password");

                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println("A new client has connected!");
                    ClientHandler clientHandler = new ClientHandler(socket);
                    
                    Thread thread = new Thread(clientHandler);
                    thread.start();
                }
            }
            
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addClientHandler(ClientHandler clientHandler) {
        clientHandlers.add(clientHandler);
    }

    public static void removeClientHandler(ClientHandler clientHandler) {
        clientHandlers.remove(clientHandler);
    }

    public static boolean registerUser(String username, String password) {
        try {
            // Check if the username already exists
            String checkUserQuery = "SELECT COUNT(*) FROM users WHERE username = ?";
            PreparedStatement checkUserStmt = dbConnection.prepareStatement(checkUserQuery);
            checkUserStmt.setString(1, username);
            ResultSet rs = checkUserStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return false; // Username already exists
            }

            // Insert the new user into the database
            String hashedPassword = PasswordUtil.hashPassword(password);
            String insertUserQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement insertUserStmt = dbConnection.prepareStatement(insertUserQuery);
            insertUserStmt.setString(1, username);
            insertUserStmt.setString(2, hashedPassword);
            insertUserStmt.executeUpdate();

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean authenticateUser(String username, String password) {
        try {
            String authQuery = "SELECT password FROM users WHERE username = ?";
            PreparedStatement authStmt = dbConnection.prepareStatement(authQuery);
            authStmt.setString(1, username);
            ResultSet rs = authStmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                String inputHashedPassword = PasswordUtil.hashPassword(password);
                return storedPassword.equals(inputHashedPassword);
            } else {
                return false; // User not found
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String username;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            closeEverything(socket, reader, writer);
        }
    }

    @Override
    public void run() {
        try {
            String clientMessage;

            // Handle the login or registration process first
            while (true) {
                clientMessage = reader.readLine();
                String[] messageParts = clientMessage.split(" ");

                if (messageParts[0].equals("REGISTER")) {
                    String newUsername = messageParts[1];
                    String newPassword = messageParts[2];
                    boolean registrationSuccess = ChatServer.registerUser(newUsername, newPassword);
                    if (registrationSuccess) {
                        writer.write("REGISTRATION SUCCESS");
                    } else {
                        writer.write("REGISTRATION FAILED: Username already taken");
                    }
                    writer.newLine();
                    writer.flush();
                } else if (messageParts[0].equals("LOGIN")) {
                    String loginUsername = messageParts[1];
                    String loginPassword = messageParts[2];
                    boolean loginSuccess = ChatServer.authenticateUser(loginUsername, loginPassword);
                    if (loginSuccess) {
                        this.username = loginUsername;
                        writer.write("SUCCESS");
                        writer.newLine();
                        writer.flush();
                        break;
                    } else {
                        writer.write("FAILED: Invalid username or password");
                        writer.newLine();
                        writer.flush();
                    }
                }
            }

            // After successful login, handle messages
            ChatServer.addClientHandler(this);

            deliverStoredMessages(username);

            System.out.println("Connected clients:");
                    for (ClientHandler client : ChatServer.clientHandlers) {
                        System.out.println("- " + client.getUsername());
                    }
                    System.out.println("--------------------------");
            broadcastMessage("SERVER: " + username + " has joined the chat!");

            while ((clientMessage = reader.readLine()) != null) {
                String[] messageParts = clientMessage.split(" ", 3);
                if (messageParts[0].equals("PUBLIC")) {
                    broadcastMessage(username + ": " + messageParts[1] + " " + messageParts[2]);
                } else if (messageParts[0].equals("PRIVATE")) {
                    String recipient = messageParts[1];
                    String privateMessage = messageParts[2];
                    if(isUserOnline(recipient)){
                    sendPrivateMessage(username, recipient, privateMessage);
                    }
                    else{
                        storeMessageInDatabase(username, recipient, privateMessage);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Client " + username + " has disconnected.");
            broadcastMessage("SERVER: " + username + " has left the chat!");
        } finally {
            ChatServer.removeClientHandler(this);
            closeEverything(socket, reader, writer);
        }
    }

    public String getUsername(){
        return this.username;
    }

    public void broadcastMessage(String message) {
        for (ClientHandler clientHandler : ChatServer.clientHandlers) {
            try {
                if (!clientHandler.username.equals(this.username)) {
                    clientHandler.writer.write(message);
                    clientHandler.writer.newLine();
                    clientHandler.writer.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, reader, writer);
            }
        }
    }

    public boolean isUserOnline(String username) {
        for(ClientHandler clientHandler : ChatServer.clientHandlers){
            if(clientHandler.username.equals(username)){
                return true;
            }
        }
        return false;
    }
    
    private void storeMessageInDatabase(String sender, String recipient, String message) {
        String query = "INSERT INTO private_messages (sender_username, recipient_username, message) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = ChatServer.dbConnection.prepareStatement(query)) {
            stmt.setString(1, sender);
            stmt.setString(2, recipient);
            stmt.setString(3, message);
            stmt.executeUpdate();
            System.out.println("Message stored for offline user: " + recipient);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deliverStoredMessages(String username) {
        String query = "SELECT * FROM private_messages WHERE recipient_username = ?";
        try (PreparedStatement stmt = ChatServer.dbConnection.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
    
            while (rs.next()) {
                String sender = rs.getString("sender_username");
                String message = rs.getString("message");
    
                // Send the message to the recipient (highlight it in green)
                sendMessageToClientWithColor(username, sender + ": " + message);
    
                // Delete the message after delivery
                deleteMessageFromDatabase(rs.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();


            
        }
    }
    
    private void deleteMessageFromDatabase(int messageId) {
        String query = "DELETE FROM private_messages WHERE id = ?";
        try (PreparedStatement stmt = ChatServer.dbConnection.prepareStatement(query)) {
            stmt.setInt(1, messageId);
            stmt.executeUpdate();
            System.out.println("Message with ID " + messageId + " deleted after delivery.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageToClientWithColor(String recipient, String message) {
        // Assume client is connected and we have a way to send colored messages (could be HTML styled for a web-based interface)
       
        for(ClientHandler clientHandler : ChatServer.clientHandlers) {
            try {
                if (clientHandler.username.equals(recipient)) {
                    String coloredMessage = "\u001B[32m" + message + "\u001B[0m";
                    clientHandler.writer.write("PREVIOUS MESSAGE: " + coloredMessage);
                    clientHandler.writer.newLine();
                    clientHandler.writer.flush();
                    return;
                }
            } catch (IOException e) {
                closeEverything(socket, reader, writer);
            }
        }
    }

    public void sendPrivateMessage(String senderUsername, String recipientUsername, String message) {
        for (ClientHandler clientHandler : ChatServer.clientHandlers) {
            try {
                if (clientHandler.username.equals(recipientUsername)) {
                    clientHandler.writer.write("PRIVATE " + senderUsername + ": " + message);
                    clientHandler.writer.newLine();
                    clientHandler.writer.flush();
                    return;
                }
            } catch (IOException e) {
                closeEverything(socket, reader, writer);
            }
        }
        try {
            writer.write("SERVER: User " + recipientUsername + " not found.");
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            closeEverything(socket, reader, writer);
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
}
