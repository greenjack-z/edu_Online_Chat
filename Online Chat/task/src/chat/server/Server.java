package chat.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final int CONNECTION_TIMEOUT = 1000;
    private static final Path userDbPath = Path.of("usersdb.txt");
    private static final Path messageDbPath = Path.of("messagedb.txt");
    private final Logger logger = Logger.getGlobal();
    int port;
    private final Map<User, Session> onlineSessions;
    private UsersCollection registeredUsers;
    private ChatsCollection openedChats;
    public Server(int port) {
        this.port = port;
        onlineSessions = new HashMap<>();
    }

    public Map<User, Session> getOnlineSessions() {
        return onlineSessions;
    }

    public UsersCollection getRegisteredUsers() {
        return registeredUsers;
    }

    public ChatsCollection getOpenedChats() {
        return openedChats;
    }

    public static void main(String[] args) {
        new Server(3128).run();
    }

    private void run() {
        logger.setLevel(Level.INFO);
        logger.info("server: started");
        System.out.println("Server started!");
        registeredUsers = loadRegisteredUsers();
        addNewUser(new User("admin", "12345678".hashCode()));
        openedChats = loadMessages();
        try (ServerSocket serverSocket = new ServerSocket(5578)) {
            serverSocket.setSoTimeout(CONNECTION_TIMEOUT);
            acceptClients(serverSocket);
        } catch (IOException e) {
            logger.warning("socket error " + e.getMessage());
        }
        logger.info("server: stop");
    }

    private void acceptClients(ServerSocket serverSocket) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket socket = serverSocket.accept();
                startSession(socket);
            } catch (SocketTimeoutException e) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            } catch (IOException e) {
                logger.warning("accepting error: " + e.getMessage());
            }
        }
        logger.info("stop accepting");
    }

    private void startSession(Socket socket) {
        Session session = new Session(socket, this);
        new Thread(session).start();
    }

    private UsersCollection loadRegisteredUsers() {
        UsersCollection users = new UsersCollection();
        if (Files.exists(userDbPath)) {
            try (InputStream inputStream = Files.newInputStream(userDbPath);
                 ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
                users = (UsersCollection) objectInputStream.readObject();
            } catch (IOException e) {
                logger.warning("error users loading: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                logger.warning("deserialization error: " + e.getMessage());
            }
        } else {
            try {
                Files.createFile(userDbPath);
            } catch (IOException e) {
                logger.warning("error making new user DB file:" + e.getMessage());
            }
        }
        logger.info("userdb loaded");
        return users;
    }

    public synchronized void addNewUser(User user) {
        registeredUsers.addUser(user);
        logger.info("user added");
        saveUsers();
    }

    public void saveUsers() {
        try (OutputStream outputStream = Files.newOutputStream(userDbPath, StandardOpenOption.TRUNCATE_EXISTING);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
            objectOutputStream.writeObject(registeredUsers);
        } catch (IOException e) {
            logger.warning("error users writing: " + e.getMessage());
        }
        logger.info("users saved");
    }

    private ChatsCollection loadMessages() {
        ChatsCollection chats = new ChatsCollection();
        if (Files.exists(messageDbPath)) {
            try (InputStream inputStream = Files.newInputStream(messageDbPath);
                 ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
                chats = (ChatsCollection) objectInputStream.readObject();
            } catch (IOException e) {
                logger.warning("error messages loading: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                logger.warning("deserialization error: " + e.getMessage());
            }
        } else {
            try {
                Files.createFile(messageDbPath);
            } catch (IOException e) {
                logger.warning("error making new message DB file: " + e.getMessage());
            }
        }
        logger.info("messages loaded");
        return chats;
    }

    public void saveMessages() {
        try (OutputStream outputStream = Files.newOutputStream(messageDbPath, StandardOpenOption.TRUNCATE_EXISTING);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
            objectOutputStream.writeObject(openedChats);
        } catch (IOException e) {
            logger.warning("error messages writing: " + e.getMessage());
        }
        logger.info("messages saved");
    }
}
