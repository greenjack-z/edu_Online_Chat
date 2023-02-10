package chat.server;

import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Session implements Runnable {
    //authorisation replies:
    private static final String LOGIN_INCORRECT = "incorrect login!";
    private static final String PASSWORD_INCORRECT = "incorrect password!";
    private static final String USER_BANNED = "you are banned!";
    private static final String AUTHORIZATION_OK = "you are authorized successfully!";
    //registration replies:
    private static final String NAME_EXISTS = "this login is already taken! Choose another one.";
    private static final String PASSWORD_IS_SHORT = "the password is too short!";
    private static final String REGISTRATION_OK = "you are registered successfully!";
    //chat command replies:
    private static final String NOT_ONLINE = "the user is not online!";
    private static final String NOT_YOURSELF = "you can't chat with yourself!";
    //list command replies:
    private static final String ONLINE_NONE = "no one online";
    //kick replies:
    private static final String KICK_YOURSELF = "you can't kick yourself!";
    private static final String USER_KICKED = "%s was kicked!";
    private static final String NOT_ADMIN_MODERATOR = "you are not a moderator or an admin!";
    private static final String KICK_MESSAGE = "you have been kicked out of the server!";
    //grant replies:
    private static final String USER_NEW_MODERATOR = "%s is the new moderator!";
    private static final String USER_ALREADY_MODERATOR = "this user is already a moderator!";
    private static final String NOT_ADMIN = "you are not an admin!";
    private static final String GRANT_MESSAGE = "you are the new moderator now!";
    //revoke replies:
    private static final String USER_NO_MODERATOR = "%s is no longer a moderator!";
    private static final String USER_NOT_MODERATOR = "this user is not a moderator!";
    private static final String REVOKE_MESSAGE = "you are no longer a moderator!";
    //unread replies:
    private static final String NO_UNREAD = "no one unread";
    //stats replies:
    //history replies:
    private static final String ERROR_PARAMETERS_COUNT = "Wrong parameters count, try again.";
    private static final String NOT_LOGGED_IN = "you are not in the chat!";
    private static final String CHOOSE_USER_TO_CHAT = "use /list command to choose a user to text!";
    private static final String COMMAND_INCORRECT = "incorrect command!";
    private static final String AUTHORIZATION_REQUEST = "authorize or register";
    private static final String ADMIN = "admin";
    private final Logger logger = Logger.getGlobal();
    private final Server server;
    private final Sender sender;
    private final Receiver receiver;
    private User owner;
    private Chat activeChat;

    public Session(Socket socket, Server server) {
        this.server = server;
        this.sender = new Sender(socket);
        this.receiver = new Receiver(socket);
        this.activeChat = null;
    }

    @Override
    public void run() {
        logger.log(Level.INFO, "Session \"{0}\": start", owner);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(sender);
        executorService.execute(receiver);
        executorService.shutdown();
        printServerMessage(AUTHORIZATION_REQUEST);
        try {
            while (!executorService.awaitTermination(10, TimeUnit.MILLISECONDS)) {
                readMessage();
            }
        } catch (InterruptedException e) {
            logger.warning("interrupted while listening: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
        stopSession();
    }

    public void printServerMessage(String text) {
        printMessage(new Message(new User("Server", 0), text));
    }

    private synchronized void printMessage(Message message) {
        sender.printOutput(message.toString());
    }

    private void readMessage() {
        String inputString = receiver.readInput();
        if (inputString == null) {
            return;
        }
        logger.log(Level.INFO, "input: {0}", inputString);
        parseInput(inputString);
    }

    private void parseInput(String inputString) {
        CommandList command = parseCommand(inputString);
        logger.log(Level.INFO, "parse command: {0}", command);
        if (command != null) {
            logger.log(Level.INFO, "execute command: {0}", command);
            executeCommand(command, inputString);
            return;
        }
        if (!isLoggedIn()) {
            printServerMessage(NOT_LOGGED_IN);
            return;
        }
        if (activeChat == null) {
            printServerMessage(CHOOSE_USER_TO_CHAT);
        } else {
            logger.info("sending message to addressee");
            Message message = new Message(owner, inputString);
            activeChat.sendMessage(message);
            for (User user : activeChat.getUsers()) {
                if (server.getOnlineSessions().containsKey(user)) {
                    Session session = server.getOnlineSessions().get(user);
                    if (activeChat.equals(session.activeChat)) {
                        session.printMessage(activeChat.readMessage(user));
                    }
                }
            }
        }
    }

    private CommandList parseCommand(String string) {
        if (!string.trim().startsWith("/")) {
            return null;
        }
        for (CommandList command : CommandList.values()) {
            if (string.trim().startsWith(command.toString())) {
                return command;
            }
        }
        return CommandList.UNKNOWN;
    }

    private void executeCommand(CommandList command, String message) {
        String[] parameters = message.replaceFirst(command.toString(), "").trim().split("\\s+");
        if (isLoggedIn()) {
            switch (command) {
                case STATS -> printStatistics();
                case HISTORY -> history(parameters);
                case UNREAD -> listUnreadUsers();
                case REVOKE -> revokeUser(parameters);
                case GRANT -> grantUser(parameters);
                case KICK -> kickUser(parameters);
                case CHAT -> openChat(parameters);
                case LIST -> listOnlineUsers();
                case EXIT -> exit();
                default -> printServerMessage(COMMAND_INCORRECT);
            }
        } else {
            switch (command) {
                case AUTH -> authorise(parameters);
                case REGISTRATION -> registration(parameters);
                case UNKNOWN -> printServerMessage(COMMAND_INCORRECT);
                default -> printServerMessage(NOT_LOGGED_IN);
            }
        }
    }

    private void printStatistics() {
        Iterator<User> iterator = activeChat.getUsers().iterator();
        User user = iterator.next();
        if (user == owner) {
            user = iterator.next();
        }
        sender.printOutput("""
                Server:
                Statistics with %s:
                Total messages: %d
                Messages from %s: %d
                Messages from %s: %d
                """.formatted(user.name(),
                activeChat.countMessages(null),
                owner.name(), activeChat.countMessages(owner),
                user.name(), activeChat.countMessages(user)));
    }
    private void history(String[] parameters) {
        if (parameters.length != 1) {
            printServerMessage(ERROR_PARAMETERS_COUNT);
            return;
        }
        try {
            int index = Integer.parseInt(parameters[0]);
            sender.printOutput("Server:");
            activeChat.getHistory(index).forEach(sender::printOutput);
        } catch (NumberFormatException e) {
            printServerMessage("%s is not a number!".formatted(parameters[0]));
        }
    }
    private void revokeUser(String[] parameters) {
        if (parameters.length != 1) {
            printServerMessage(ERROR_PARAMETERS_COUNT);
            return;
        }
        if (!owner.name().equalsIgnoreCase(ADMIN)) {
            printServerMessage(NOT_ADMIN);
            return;
        }
        User targetUser = server.getRegisteredUsers().getUser(parameters[0]);
        if (!server.getRegisteredUsers().isModerator(targetUser)) {
            printServerMessage(USER_NOT_MODERATOR);
            return;
        }
        server.getRegisteredUsers().revoke(targetUser);
        server.getOnlineSessions().get(targetUser).printServerMessage(REVOKE_MESSAGE);
        printServerMessage(USER_NO_MODERATOR.formatted(targetUser.name()));
    }

    private void grantUser(String[] parameters) {
        if (parameters.length != 1) {
            printServerMessage(ERROR_PARAMETERS_COUNT);
            return;
        }
        if (!owner.name().equalsIgnoreCase(ADMIN)) {
            printServerMessage(NOT_ADMIN);
            return;
        }
        User targetUser = server.getRegisteredUsers().getUser(parameters[0]);
        if (server.getRegisteredUsers().isModerator(targetUser)) {
            printServerMessage(USER_ALREADY_MODERATOR);
            return;
        }
        server.getRegisteredUsers().grant(targetUser);
        server.getOnlineSessions().get(targetUser).printServerMessage(GRANT_MESSAGE);
        printServerMessage(USER_NEW_MODERATOR.formatted(targetUser.name()));
    }

    private void kickUser(String[] parameters) {
        if (parameters.length != 1) {
            printServerMessage(ERROR_PARAMETERS_COUNT);
            return;
        }
        User targetUser = server.getRegisteredUsers().getUser(parameters[0]);
        if (!owner.name().equalsIgnoreCase(ADMIN)) {
            if (!server.getRegisteredUsers().isModerator(owner)) {
                printServerMessage(NOT_ADMIN_MODERATOR);
                return;
            }
            if (server.getRegisteredUsers().isModerator(targetUser)) {
                printServerMessage(NOT_ADMIN);
                return;
            }
            if (targetUser.name().equalsIgnoreCase(ADMIN)) {
                printServerMessage("You can't kick an admin");
                return;
            }
        }
        if (targetUser.equals(owner)) {
            printServerMessage(KICK_YOURSELF);
            return;
        }
        server.getOnlineSessions().get(targetUser).printServerMessage(KICK_MESSAGE);
        server.getOnlineSessions().remove(targetUser);
        server.getRegisteredUsers().ban(targetUser);
        printServerMessage(USER_KICKED.formatted(targetUser.name()));
    }

    private void authorise(String[] parameters) {
        if (parameters.length != 2) {
            logger.log(Level.WARNING, "parameters: {0}", parameters.length);
            printServerMessage(ERROR_PARAMETERS_COUNT);
            return;
        }
        logger.info("authorisation routine");
        String login = parameters[0].trim();
        User user = server.getRegisteredUsers().getUser(login);
        if (user == null) {
            printServerMessage(LOGIN_INCORRECT);
            return;
        }
        String password = parameters[1].trim();
        if (user.passwordHash() != password.hashCode()) {
            printServerMessage(PASSWORD_INCORRECT);
            return;
        }
        if (server.getRegisteredUsers().isBanned(user)) {
            printServerMessage(USER_BANNED);
            return;
        }
        owner = user;
        server.getOnlineSessions().put(owner, this);
        printServerMessage(AUTHORIZATION_OK);
        logger.info("authorization success");
    }

    private void registration(String[] parameters) {
        if (parameters.length != 2) {
            printServerMessage(ERROR_PARAMETERS_COUNT);
            return;
        }
        logger.info("registration routine");
        String login = parameters[0].trim();
        if (server.getRegisteredUsers().isRegistered(login)) {
            printServerMessage(NAME_EXISTS);
            return;
        }
        String password = parameters[1].trim();
        if (password.length() < 8) {
            printServerMessage(PASSWORD_IS_SHORT);
            return;
        }
        User newUser = new User(login, password.hashCode());
        owner = newUser;
        server.addNewUser(newUser);
        server.getOnlineSessions().put(newUser, this);
        printServerMessage(REGISTRATION_OK);
        logger.info("registration success");
    }

    private void openChat(String[] parameters) {
        if (parameters.length != 1) {
            printServerMessage(ERROR_PARAMETERS_COUNT);
            return;
        }
        User targetUser = server.getRegisteredUsers().getUser(parameters[0]);
        if (targetUser == owner) {
            printServerMessage(NOT_YOURSELF);
            return;
        }
        if (targetUser == null) {
            printServerMessage(NOT_ONLINE);
            return;
        }
        Session chatSession = server.getOnlineSessions().get(targetUser);
        if (chatSession == null) {
            printServerMessage(NOT_ONLINE);
            return;
        }
        activeChat = server.getOpenedChats().getChat(Set.of(owner, targetUser));
        sendNewMessages();
    }

    private void sendNewMessages() {
        activeChat.getLastMessages(owner).forEach(sender::printOutput);
    }
    
    private void listOnlineUsers() {
        List<String> userNames = new ArrayList<>();
        server.getOnlineSessions().keySet().forEach(user -> userNames.add(user.name()));
        userNames.remove(owner.name());
        if (userNames.isEmpty()) {
            printServerMessage(ONLINE_NONE);
            return;
        }
        userNames.sort(Comparator.naturalOrder());
        printServerMessage("online: " + String.join(" ", userNames));
    }

    private void listUnreadUsers() {
        List<String> userNames = new ArrayList<>();
        for (Map.Entry<User, Chat> entry : server.getOpenedChats().getUserChats(owner).entrySet()) {
            if (entry.getValue().haveUnreadMessages(owner)) {
                userNames.add(entry.getKey().name());
            }
        }
        if (userNames.isEmpty()) {
            printServerMessage(NO_UNREAD);
            return;
        }
        userNames.sort(Comparator.naturalOrder());
        printServerMessage("unread from: " + String.join(" ", userNames));
    }

    private void exit() {
        //do nothing
    }

    private boolean isLoggedIn() {
        return server.getOnlineSessions().containsKey(owner);
    }

    private void stopSession() {
        if (!isLoggedIn()) {
            System.out.println("Client disconnected before login");
            return;
        }
        server.saveMessages();
        server.getOnlineSessions().remove(owner);
        System.out.printf("Client %s disconnected!%n", owner.name());
        logger.info("session: stop");
    }
}
