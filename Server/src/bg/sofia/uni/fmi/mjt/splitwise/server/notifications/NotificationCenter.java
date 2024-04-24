package bg.sofia.uni.fmi.mjt.splitwise.server.notifications;

import bg.sofia.uni.fmi.mjt.splitwise.server.debt.DebtRecord;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.User;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;
import bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NotificationCenter {
    private final Set<DebtRecord> paidDebts = new HashSet<>();
    private final Set<DebtRecord> partlyPaidDebts = new HashSet<>();
    private final Set<DebtRecord> newDebts = new HashSet<>();
    private final Map<String, Set<DebtRecord>> newGroupDebts = new HashMap<>();
    private final Path paidDebtsFile;
    private final Path partlyPaidDebtsFile;
    private final Path newDebtsFile;
    private final Path newGroupDebtsFile;

    public NotificationCenter(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }

        paidDebtsFile = Path.of(username + "_paid_debts.txt");
        partlyPaidDebtsFile = Path.of(username + "_partly_paid_debts.txt");
        newDebtsFile = Path.of(username + "_new_debts.txt");
        newGroupDebtsFile = Path.of(username + "_new_group_debts.txt");
    }

    public void addPaidDebt(DebtRecord debt) throws ServerErrorException {
        if (debt == null) {
            throw new IllegalArgumentException("Debt cannot be null");
        }

        paidDebts.add(debt);
        FileUtils.writeToFile(paidDebtsFile, FileUtils.SINGLE_SPACE, debt);
    }

    public void addPartlyPaidDebt(DebtRecord debt) throws ServerErrorException {
        if (debt == null) {
            throw new IllegalArgumentException("Debt cannot be null");
        }

        partlyPaidDebts.add(debt);
        FileUtils.writeToFile(partlyPaidDebtsFile, FileUtils.SINGLE_SPACE, debt);
    }

    public void addNewDebt(DebtRecord debt) throws ServerErrorException {
        if (debt == null) {
            throw new IllegalArgumentException("Debt cannot be null");
        }

        newDebts.add(debt);
        FileUtils.writeToFile(newDebtsFile, FileUtils.SINGLE_SPACE, debt);
    }

    public void addNewGroupDebt(DebtRecord debt, String groupName) throws ServerErrorException {
        if (debt == null || groupName == null) {
            throw new IllegalArgumentException("Debt and group name cannot be null");
        }

        if (!newGroupDebts.containsKey(groupName)) {
            newGroupDebts.put(groupName, new HashSet<>());
        }

        newGroupDebts.get(groupName).add(debt);
        FileUtils.writeToFile(newGroupDebtsFile, FileUtils.SINGLE_SPACE, groupName, debt);
    }

    public String getNotifications() throws ServerErrorException {
        loadDebts();

        if (paidDebts.isEmpty() && partlyPaidDebts.isEmpty() && newDebts.isEmpty() && newGroupDebts.isEmpty()) {
            return "No notifications to be shown";
        }

        StringBuilder builder = new StringBuilder("*** Notifications ***\n");

        loadPaidDebts(builder);
        loadPartlyPaidDebts(builder);
        loadNewDebts(builder);
        loadNewGroupDebts(builder);

        return builder.toString();
    }

    private void loadDebts() throws ServerErrorException {
        loadDebtsFromFile(paidDebtsFile, paidDebts);
        loadDebtsFromFile(partlyPaidDebtsFile, partlyPaidDebts);
        loadDebtsFromFile(newDebtsFile, newDebts);
        loadGroupDebtsFromFile();
    }

    private void loadPaidDebts(StringBuilder builder) throws ServerErrorException {
        if (!paidDebts.isEmpty()) {
            builder.append(" * Successfully paid debts:\n");
            for (DebtRecord debt : paidDebts) {
                builder.append(debt.getSuccessfullyPaidDebt()).append('\n');
            }
            clearFiles(paidDebtsFile, paidDebts);
        }
    }

    private void loadPartlyPaidDebts(StringBuilder builder) throws ServerErrorException {
        if (!partlyPaidDebts.isEmpty()) {
            builder.append(" * Partly paid debts:\n");
            for (DebtRecord debt : partlyPaidDebts) {
                builder.append(debt.getPartlyPaidDebt()).append('\n');
            }
            clearFiles(partlyPaidDebtsFile, partlyPaidDebts);
        }
    }

    private void loadNewDebts(StringBuilder builder) throws ServerErrorException {
        if (!newDebts.isEmpty()) {
            builder.append(" * New friend debts:\n");
            for (DebtRecord debt : newDebts) {
                builder.append(" - ").append(debt.visualizeDebt("You owe")).append('\n');
            }
            clearFiles(newDebtsFile, newDebts);
        }
    }

    private void loadNewGroupDebts(StringBuilder builder) throws ServerErrorException {
        if (!newGroupDebts.isEmpty()) {
            builder.append(" * New group debts:\n");
            for (Map.Entry<String, Set<DebtRecord>> entry : newGroupDebts.entrySet()) {
                builder.append(" ** Group: ").append(entry.getKey()).append('\n');
                for (DebtRecord debt : entry.getValue()) {
                    builder.append(" - ").append(debt.visualizeDebt("You owe")).append('\n');
                }
            }
            clearGroupFiles();
        }
    }

    private void clearFiles(Path fileName, Set<DebtRecord> debts) throws ServerErrorException {
        debts.clear();
        FileUtils.deleteFile(fileName);
    }

    private void clearGroupFiles() throws ServerErrorException {
        newGroupDebts.clear();
        FileUtils.deleteFile(newGroupDebtsFile);
    }

    private void loadDebtsFromFile(Path fileName, Set<DebtRecord> debts) throws ServerErrorException {
        if (!Files.exists(fileName)) {
            return;
        }

        try (var bufferedReader = Files.newBufferedReader(fileName)) {
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                String[] user = line.split(FileUtils.SINGLE_SPACE);

                User debtor = UserRepository.toUser(user[FileUtils.ZERO_INDEX]);
                User creditor = UserRepository.toUser(user[FileUtils.ONE_INDEX]);
                double amount = Double.parseDouble(user[FileUtils.TWO_INDEX]);
                String reason = Arrays.stream(user)
                    .skip(FileUtils.THREE_INDEX)
                    .collect(Collectors.joining(FileUtils.SINGLE_SPACE));

                debts.add(new DebtRecord(debtor, creditor, amount, reason));
            }
        } catch (IOException e) {
            throw new ServerErrorException(FileUtils.READING_FROM_FILE_ERROR + fileName, e);
        }
    }

    private void loadGroupDebtsFromFile() throws ServerErrorException {
        if (!Files.exists(newGroupDebtsFile)) {
            return;
        }

        try (var bufferedReader = Files.newBufferedReader(newGroupDebtsFile)) {
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                String[] user = line.split(FileUtils.SINGLE_SPACE);

                String groupName = user[FileUtils.ZERO_INDEX];
                User debtor = UserRepository.toUser(user[FileUtils.ONE_INDEX]);
                User creditor = UserRepository.toUser(user[FileUtils.TWO_INDEX]);
                double amount = Double.parseDouble(user[FileUtils.THREE_INDEX]);
                String reason = Arrays.stream(user)
                    .skip(FileUtils.FOUR_INDEX)
                    .collect(Collectors.joining(FileUtils.SINGLE_SPACE));

                if (!newGroupDebts.containsKey(groupName)) {
                    newGroupDebts.put(groupName, new HashSet<>());
                }
                newGroupDebts.get(groupName).add(new DebtRecord(debtor, creditor, amount, reason));
            }
        } catch (IOException e) {
            throw new ServerErrorException(FileUtils.READING_FROM_FILE_ERROR + newGroupDebtsFile, e);
        }
    }
}
