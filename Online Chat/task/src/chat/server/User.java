package chat.server;

import java.io.Serial;
import java.io.Serializable;

public record User(String name, int passwordHash) implements Serializable {
    @Serial
    private static final long serialVersionUID = 100L;
}
