package chat.server;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class Chat implements Serializable {
    @Serial
    private static final long serialVersionUID = 12L;
    private final List<Message> messages = new ArrayList<>();

    private final Map<User, Integer> lastReadIndex = new HashMap<>(2);

    public Chat(Set<User> users) {
        users.forEach(user -> lastReadIndex.put(user, -1));
    }

    public int countMessages(User user) {
        if (user == null) {
            return messages.size();
        }
        return (int) messages.stream().filter(message -> message.author() == user).count();
    }
    public Set<User> getUsers() {
        return lastReadIndex.keySet();
    }

    public void sendMessage(Message message) {
        messages.add(message);
    }

    public Message readMessage(User user) {
        lastReadIndex.put(user, messages.size() - 1);
        return messages.get(lastReadIndex.get(user));
    }

    public List<String> getLastMessages(User user) {
        List<String> lastMessages = new ArrayList<>();
        int start = 0;
        start = Math.max(start, messages.size() - 25);
        start = Math.max(start, lastReadIndex.get(user) - 9);

        for (int i = start; i < messages.size(); i++) {
            Message message = messages.get(i);
            if (i > this.lastReadIndex.get(user)) {
                lastMessages.add("(new) " + message.toString());
            } else {
                lastMessages.add(message.toString());
            }
        }
        this.lastReadIndex.put(user, messages.size() - 1);
        return lastMessages;
    }

    public List<String> getHistory(int index) {
        int start = 0;
        start = Math.max(start, messages.size() - index);
        int end = start + 25;
        end = Math.min(end, messages.size() - 1);
        List<Message> messageList = messages.subList(start, end);
        return messageList.stream()
                .map(Message::toString)
                .toList();
    }

    public boolean haveUnreadMessages(User user) {
        if (!lastReadIndex.containsKey(user)) {
            return false;
        }
        return messages.size() - 1 > lastReadIndex.get(user);
    }
}
