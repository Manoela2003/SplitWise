package bg.sofia.uni.fmi.mjt.splitwise.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {
    private static final int SERVER_PORT = 3333;
    private static final String SERVER_HOST = "localhost";
    private static final int BUFFER_SIZE = 512;
    private static final String ERROR_MESSAGE =
        "The server is shutdown. Try again later or contact administrator by providing the logs in logs_file.txt";
    private static final String QUIT_MESSAGE = "quit";
    private static final String GOODBYE_MESSAGE = "Goodbye!";
    private static ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

    public static void main(String[] args) {

        try (SocketChannel socketChannel = SocketChannel.open();
             Scanner scanner = new Scanner(System.in)) {

            socketChannel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));

            buffer.clear();
            socketChannel.read(buffer);
            buffer.flip();

            byte[] byteArray = new byte[buffer.remaining()];
            buffer.get(byteArray);
            String reply = new String(byteArray, StandardCharsets.UTF_8);

            System.out.println(reply);

            boolean isConnected = true;

            while (isConnected) {
                isConnected = interactWithServer(scanner, socketChannel);
            }

        } catch (IOException e) {
            System.out.println(ERROR_MESSAGE);
        }
    }

    public static boolean interactWithServer(Scanner scanner, SocketChannel socketChannel) throws IOException {
        String message = scanner.nextLine();

        if (QUIT_MESSAGE.equals(message)) {
            System.out.println(GOODBYE_MESSAGE);
            return false;
        }

        buffer.clear();
        buffer.put(message.getBytes());
        buffer.flip();
        socketChannel.write(buffer);

        buffer.clear();
        socketChannel.read(buffer);
        buffer.flip();

        byte[] byteArray = new byte[buffer.remaining()];
        buffer.get(byteArray);
        String reply = new String(byteArray, StandardCharsets.UTF_8);

        System.out.println(reply);
        return true;
    }
}
