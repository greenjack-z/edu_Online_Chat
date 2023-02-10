package chat.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Logger;

public class Sender implements Runnable {
    private final Logger logger = Logger.getGlobal();
    private final Socket socket;

    public Sender(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        logger.info("sender: start");
            try (Scanner scanner = new Scanner(System.in);
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream()))    {
                while (!Thread.currentThread().isInterrupted()) {
                    String message = scanner.nextLine();
                    if (message.isEmpty()) {
                        continue;
                    }
                    dataOutputStream.writeUTF(message);
                    if (message.trim().equalsIgnoreCase("/exit")) {
                        socket.close();
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (IOException e) {
                logger.warning("IO exception: " + e.getMessage());
            }
        logger.info("sender: stop");
    }
}
