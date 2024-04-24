package bg.sofia.uni.fmi.mjt.splitwise.server;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.CommandExecutor;
import bg.sofia.uni.fmi.mjt.splitwise.server.debt.DebtManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.friends.FriendsManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.groups.GroupManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.logs.LogsManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class Server {
    private static final String SERVER_HOST = "localhost";
    public static final int SERVER_PORT = 3333;
    private static final int BUFFER_SIZE = 1024;
    private static final int ZERO = 0;
    private static final String SERVER_CONNECTION_ERROR = "An error occurred while connecting with the server";
    private static final String ERROR_MESSAGE =
        "The server is shutdown. Try again later or contact administrator by providing the logs in logs_file.txt";
    private static final String WELCOME_MESSAGE = """
        Welcome!
        If you already have an account use the following command:
         - login <username> <password>
        If you don't have an account yet use the following command:
         - register <username> <password> <first name> <last name>""";
    private final UserRepository userRepository = UserRepository.getInstance();
    private final GroupManager groupManager = GroupManager.getInstance();
    private final DebtManager debtManager = DebtManager.getInstance();
    private final FriendsManager friendsManager = FriendsManager.getInstance();
    private final CommandExecutor commandExecutor =
        CommandExecutor.configure(userRepository, groupManager, debtManager, friendsManager);
    private final LogsManager logsManager = LogsManager.getInstance();
    private ByteBuffer buffer;
    private Selector selector;
    private boolean isServerWorking;
    private static Server instance;

    private Server() {
    }

    public static Server getInstance() {
        if (instance == null) {
            instance = new Server();
        }
        return instance;
    }

    public void start() {
        try {
            initialize();
        } catch (ServerErrorException e) {
            System.out.println(e.getMessage());
            logsManager.addLogToFile(e, null);
            return;
        }

        try (ServerSocketChannel channel = ServerSocketChannel.open()) {
            configureChannel(channel);
            isServerWorking = true;
            while (isServerWorking) {
                try {
                    communicateWithClient();
                } catch (IOException e) {
                    System.out.println();
                    logsManager.addLogToFile(e, null);
                    return;
                }
            }
        } catch (IOException e) {
            System.out.println(SERVER_CONNECTION_ERROR);
            logsManager.addLogToFile(e, null);
        }
    }

    private void initialize() throws ServerErrorException {
        userRepository.initialize();
        friendsManager.initialize();
        groupManager.initialize();
        debtManager.initialize();
    }

    private void configureChannel(ServerSocketChannel channel) throws IOException {
        channel.bind(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
        channel.configureBlocking(false);

        selector = Selector.open();
        channel.register(selector, SelectionKey.OP_ACCEPT);

        buffer = ByteBuffer.allocate(BUFFER_SIZE);
    }

    private void communicateWithClient() throws IOException {
        int readyChannels = selector.select();
        if (readyChannels == ZERO) {
            return;
        }

        Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
        while (keyIterator.hasNext()) {
            SelectionKey key = keyIterator.next();

            if (key.isReadable()) {
                readKey(key);
            } else if (key.isAcceptable()) {
                accept(key);
            }

            keyIterator.remove();
        }
    }

    private void readKey(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        String clientInput = getClientInput(socketChannel);

        if (clientInput == null) {
            return;
        }

        try {
            String output = commandExecutor.execute(clientInput, key);
            writeClientOutput(socketChannel, output);
        } catch (ServerErrorException e) {
            System.out.println(SERVER_CONNECTION_ERROR);
            writeClientOutput(socketChannel, ERROR_MESSAGE);
            logsManager.addLogToFile(e, (String) key.attachment());
        }
    }

    private String getClientInput(SocketChannel socketChannel) throws IOException {
        buffer.clear();

        int r = socketChannel.read(buffer);
        if (r < ZERO) {
            socketChannel.close();
            return null;
        }
        buffer.flip();

        byte[] clientInputBytes = new byte[buffer.remaining()];
        buffer.get(clientInputBytes);

        return new String(clientInputBytes, StandardCharsets.UTF_8);
    }

    public void stop() {
        this.isServerWorking = false;
        if (selector.isOpen()) {
            selector.wakeup();
        }
    }

    private void writeClientOutput(SocketChannel socketChannel, String output) throws IOException {
        buffer.clear();
        buffer.put(output.getBytes());
        buffer.flip();

        socketChannel.write(buffer);
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel sockChannel = (ServerSocketChannel) key.channel();
        SocketChannel accept = sockChannel.accept();
        accept.configureBlocking(false);
        accept.register(selector, SelectionKey.OP_READ);

        writeClientOutput(accept, WELCOME_MESSAGE);
    }
}
