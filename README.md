# Java Multi-threaded Chat Application

## Overview

This project is a multi-threaded chat application built in Java, where multiple clients can communicate with each other in real-time. It features secure user authentication, private messaging with offline support, and client disconnection notifications. The application uses sockets for client-server communication and JDBC for database integration to store user credentials and undelivered messages.

## Features

- **Multi-threaded Communication**: Supports multiple clients connected to a server simultaneously, enabling real-time messaging.
- **User Authentication**: Secure login system with usernames and hashed passwords (using SHA-256) stored in an SQL database.
- **Private Messaging**: Allows users to send direct messages to other users.
  - If a recipient is offline, the message is stored in the database and delivered upon the recipient's reconnection.
  - Messages delivered from storage are highlighted in **green** in the terminal for better visibility.
- **Disconnection Notification**: Users are notified when another user disconnects from the server.
- **Command-line Interface with Color-coded Messaging**: Offline-delivered messages appear in green using ANSI escape codes in the terminal.

## Technologies Used

- **Java**: Core programming language used for multi-threading, socket programming, and client-server communication.
- **Sockets**: For handling TCP connections between clients and the server.
- **JDBC**: For database connectivity, handling user credentials, and storing/retrieving undelivered messages.
- **MySQL (or any other SQL Database)**: Used to store user credentials and undelivered private messages.
- **SHA-256**: For hashing user passwords before storing them in the database.
- **ANSI Escape Codes**: To display messages in different colors in the command-line interface.


## Setup

### 1. Configure the Database:

Set up an SQL database (e.g., MySQL), and create tables for user credentials and private messages:

```sql
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE,
    password_hash VARCHAR(64)
);

CREATE TABLE private_messages (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sender_username VARCHAR(50),
    recipient_username VARCHAR(50),
    message TEXT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);
```
### 2. Set up the JDBC Connection:

Update the DatabaseConnection.java file with your database credentials:
```java
String url = "jdbc:mysql://localhost:3306/chat_application";
String username = "your_db_username";
String password = "your_db_password";
```
### 3. Compile the Project:
Use javac to compile the Java files:
```bash
javac -d bin src/*.java
```
### 4. Run the Server:
Start the server:
```bash
java -cp bin ChatServer
```
 ### 5. Run the Client:
Start a client instance (repeat for multiple clients):
```bash
java -cp bin ChatClient
```
## Usage
### Login or Register:
- When a client connects, they must log in with their username and password.
- If they are new, they can register an account, and their password will be securely stored in the database.
### Send Private Messages:
- Clients can send private messages to other users by typing the recipient’s username followed by the message.
- If the recipient is offline, the message will be stored in the database and delivered when they reconnect.
### Receive Offline Messages:
- When a user logs in, any private messages sent to them while they were offline are delivered immediately and highlighted in green.
### Disconnection:
- When a client disconnects, the server notifies all other connected clients.

