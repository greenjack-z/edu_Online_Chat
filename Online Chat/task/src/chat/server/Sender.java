package chat.server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Logger;

public class Sender implements Runnable {
    private final Logger logger = Logger.getGlobal();
    private final Socket socket;
    private final Deque<String> strings;

    public Sender(Socket socket) {
        this.socket = socket;
        this.strings = new ArrayDeque<>();
    }

    public synchronized void printOutput(String string) {
        strings.add(string);
    }

    @Override
    public void run() {
        logger.info("sender: start");
        try (DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
            while (!socket.isClosed()) {
                String string = strings.pollFirst();
                if (string == null) {
                    continue;
                }
                dataOutputStream.writeUTF(string);
            }
        } catch (IOException e) {
            logger.warning("output stream error: " + e.getMessage());
        }
        logger.info("sender: stop");
    }
}
