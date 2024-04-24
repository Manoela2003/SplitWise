package bg.sofia.uni.fmi.mjt.splitwise.server.command.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;
import bg.sofia.uni.fmi.mjt.splitwise.server.debt.DebtRecord;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.User;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;
import bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SeeTransactionsCommand extends Command {
    private final String username;

    public SeeTransactionsCommand(String username) {
        this.username = username;
    }

    @Override
    public String execute() throws ServerErrorException {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }

        Path transactionsPath = Path.of(username + "_transaction_history.txt");
        if (!Files.exists(transactionsPath)) {
            return "There are no transactions";
        }

        Set<DebtRecord> paidDebts = loadTransactionHistory(transactionsPath);

        StringBuilder builder = new StringBuilder(" * Transactions *\n");
        for (DebtRecord debt : paidDebts) {
            builder.append(debt.getSuccessfullyPaidDebt()).append('\n');
        }

        return builder.toString();
    }

    private Set<DebtRecord> loadTransactionHistory(Path fileName) throws ServerErrorException {
        Set<DebtRecord> debts = new HashSet<>();

        try (var bufferedReader = Files.newBufferedReader(fileName)) {
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                String[] debt = line.split(FileUtils.SINGLE_SPACE);

                User debtor = UserRepository.toUser(debt[FileUtils.ZERO_INDEX]);
                User creditor = UserRepository.toUser(debt[FileUtils.ONE_INDEX]);
                double amount = Double.parseDouble(debt[FileUtils.TWO_INDEX]);

                String reason = Arrays.stream(debt)
                    .skip(FileUtils.THREE_INDEX)
                    .collect(Collectors.joining(FileUtils.SINGLE_SPACE));

                debts.add(new DebtRecord(debtor, creditor, amount, reason));
            }

            return debts;
        } catch (IOException e) {
            throw new ServerErrorException(FileUtils.READING_FROM_FILE_ERROR + fileName, e);
        }
    }
}
