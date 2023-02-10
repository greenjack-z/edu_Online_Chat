package chat.server;

public enum CommandList {
    AUTH("/auth"),
    CHAT("/chat"),
    EXIT("/exit"),
    GRANT("/grant"),
    HISTORY("/history"),
    KICK("/kick"),
    LIST("/list"),
    REGISTRATION("/registration"),
    REVOKE("/revoke"),
    STATS("/stats"),
    UNKNOWN("/unknown"),
    UNREAD("/unread");
    private final String text;

    CommandList(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
