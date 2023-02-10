package chat.client;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;

public class Receiver implements Runnable {
    private final Logger logger = Logger.getGlobal();
    private final Socket socket;

    public Receiver(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        logger.info("receiver: start");
        try (DataInputStream dataInputStream = new DataInputStream(socket.getInputStream())) {
            while (!Thread.currentThread().isInterrupted()) {
                String message = dataInputStream.readUTF();
                System.out.println(message);
            }
        } catch (IOException e) {
            logger.warning("IO exception: " + e.getMessage());
        }
        logger.info("receiver: stop");
    }
}
