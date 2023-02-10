package chat.server;

import java.io.Serial;
import java.io.Serializable;

public record Message(User author, String text) implements Serializable {
    @Serial
    private static final long serialVersionUID = 10L;
    @Override
    public String toString() {
        return "%s: %s".formatted(author.name(), text);
    }
}


