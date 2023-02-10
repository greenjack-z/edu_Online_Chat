package chat.server;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatsCollection implements Serializable {
    @Serial
    private static final long serialVersionUID = 10L;
    private static final Logger logger = Logger.getGlobal();
    private final Map<User, Map<User, Chat>> chats = new HashMap<>();
    public void addChat(Chat chat) {
        for (User user : chat.getUsers()) {
            Map<User, Chat> userChats = chats.getOrDefault(user, new HashMap<>());
            for (User subUser : chat.getUsers()) {
                if (subUser == user) {
                    continue;
                }
                userChats.putIfAbsent(subUser, chat);
            }
            chats.put(user, userChats);
        }
    }

    public Map<User, Chat> getUserChats(User user) {
        return chats.getOrDefault(user, new HashMap<>());
    }

    public Chat getChat(Set<User> users) {
        for (User user : users) {
            if (!chats.containsKey(user)) {
                logger.log(Level.INFO, "no chats for master user: {0}", user.name());
                continue;
            }
            for (User subUser : users) {
                if (subUser == user) {
                    continue;
                }
                if (getUserChats(user).containsKey(subUser)) {
                    return getUserChats(user).get(subUser);
                }
                logger.log(Level.INFO, "no chat for user: {0}", subUser.name());
            }
        }
        addChat(new Chat(users));
        return getChat(users);
    }
}
