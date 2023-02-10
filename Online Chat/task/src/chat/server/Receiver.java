package chat.server;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Logger;

public class Receiver implements Runnable {
    private final Logger logger = Logger.getGlobal();
    private final Socket socket;
    private final Deque<String> strings;

    public Receiver(Socket socket) {
        this.socket = socket;
        this.strings = new ArrayDeque<>();
    }

    public synchronized String readInput() {
        return strings.pollFirst();
    }

    @Override
    public void run() {
        logger.info("receiver: start");
        try (DataInputStream dataInputStream = new DataInputStream(socket.getInputStream())) {
            while (!socket.isClosed()) {
                String string = dataInputStream.readUTF();
                strings.add(string);
            }
        } catch (IOException e) {
            logger.warning("input stream error: " + e.getMessage());
        }
        logger.info("receiver: stop");
    }
}

