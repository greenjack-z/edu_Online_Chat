package chat.server;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersCollection implements Serializable {
    @Serial
    private static final long serialVersionUID = 12L;

    private Map<String, User> users = new HashMap<>();
    private List<User> moderators = new ArrayList<>();
    private List<User> banned = new ArrayList<>();

    public User getUser(String name) {
        return users.get(name);
    }

    public boolean isRegistered(String name) {
        return users.containsKey(name);
    }

    public boolean isModerator(User user) {
        return moderators.contains(user);
    }

    public boolean isBanned(User user) {
        return banned.contains(user);
    }

    public User addUser(User user) {
        return users.putIfAbsent(user.name(), user);
    }
    public void grant(User user) {
        moderators.add(user);
    }

    public void revoke(User user) {
        moderators.remove(user);
    }

    public void ban(User user) {
        banned.add(user);
    }
}
