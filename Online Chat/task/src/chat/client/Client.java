package chat.client;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    private static final int CONNECTION_TIMEOUT = 1000;
    private final Logger logger = Logger.getGlobal();
    private final String address;
    private final int port;

    public Client(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public static void main(String[] args) throws InterruptedException {
        new Client("127.0.0.1", 5578).run();
    }

    private synchronized void run() throws InterruptedException {
        logger.setLevel(Level.INFO);
        logger.info("client started");
        System.out.println("Client started!");
        ExecutorService executorService = Executors.newFixedThreadPool(2 );
        Socket socket = null;
        while (socket == null) {
            try {
                socket = executorService.submit(this::getSocket).get();
            } catch (ExecutionException e) {
                logger.warning("connection error");
            }
            wait(CONNECTION_TIMEOUT);
        }
        Receiver receiver = new Receiver(socket);
        Sender sender = new Sender(socket);
        executorService.execute(receiver);
        executorService.execute(sender);
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            wait(500);
        }
        logger.info("client: stop");
    }

    private Socket getSocket() {
        try {
            return new Socket(address, port);
        } catch (IOException e) {
            logger.warning("connection error: " + e.getMessage());
            return null;
        }
    }
}
