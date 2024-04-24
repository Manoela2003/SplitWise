package bg.sofia.uni.fmi.mjt.splitwise.server.debt;

import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.NoDebtsToBePaidException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.NonPositiveAmountException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.groups.Group;
import bg.sofia.uni.fmi.mjt.splitwise.server.groups.GroupManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.User;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;
import bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Math.abs;

public class DebtManager {
    private final Map<String, Set<DebtRecord>> moneyOwed = new HashMap<>();
    private final Map<String, Set<DebtRecord>> owesMoney = new HashMap<>();
    private final Map<String, Map<String, Set<DebtRecord>>> groupMoneyOwed = new HashMap<>();
    private final Map<String, Map<String, Set<DebtRecord>>> groupOwesMoney = new HashMap<>();
    private static DebtManager instance;

    private DebtManager() {

    }

    public static DebtManager getInstance() {
        if (instance == null) {
            instance = new DebtManager();
        }

        return instance;
    }

    public void initialize() throws ServerErrorException {
        Set<String> users = UserRepository.getUsernames();
        Set<String> groups = GroupManager.getGroups();
        loadDebtsFromFiles(users);
        loadGroupsFromFiles(groups);
    }

    private void loadDebtsFromFiles(Set<String> users) throws ServerErrorException {
        for (String user : users) {
            Path fileName = Path.of(user + "_debts.txt");
            if (!Files.exists(fileName)) {
                continue;
            }

            try (var bufferedReader = Files.newBufferedReader(fileName)) {
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    String[] money = line.split(FileUtils.SINGLE_SPACE);

                    User debtor = UserRepository.toUser(money[FileUtils.ZERO_INDEX]);
                    User creditor = UserRepository.toUser(money[FileUtils.ONE_INDEX]);
                    double amount = Double.parseDouble(money[FileUtils.TWO_INDEX]);

                    String reason = Arrays.stream(money)
                        .skip(FileUtils.THREE_INDEX)
                        .collect(Collectors.joining(FileUtils.SINGLE_SPACE));

                    DebtRecord debt = new DebtRecord(debtor, creditor, amount, reason);
                    addDebt(owesMoney, debt, debtor.getUsername());
                    addDebt(moneyOwed, debt, creditor.getUsername());
                }
            } catch (IOException e) {
                throw new ServerErrorException(FileUtils.READING_FROM_FILE_ERROR + fileName, e);
            }
        }
    }

    private void loadGroupsFromFiles(Set<String> groups) throws ServerErrorException {
        for (String group : groups) {
            Path fileName = Path.of(group + "_group_debts.txt");
            if (Files.exists(fileName)) {
                try (var bufferedReader = Files.newBufferedReader(fileName)) {
                    String line;

                    while ((line = bufferedReader.readLine()) != null) {
                        String[] money = line.split(FileUtils.SINGLE_SPACE);

                        String groupName = money[FileUtils.ZERO_INDEX];
                        User debtor = UserRepository.toUser(money[FileUtils.ONE_INDEX]);
                        User creditor = UserRepository.toUser(money[FileUtils.TWO_INDEX]);
                        double amount = Double.parseDouble(money[FileUtils.THREE_INDEX]);

                        String reason = Arrays.stream(money)
                            .skip(FileUtils.FOUR_INDEX)
                            .collect(Collectors.joining(FileUtils.SINGLE_SPACE));

                        DebtRecord debt = new DebtRecord(debtor, creditor, amount, reason);

                        addGroupDebt(groupMoneyOwed, groupName, debt, creditor.getUsername());
                        addGroupDebt(groupOwesMoney, groupName, debt, debtor.getUsername());
                    }
                } catch (IOException e) {
                    throw new ServerErrorException(FileUtils.READING_FROM_FILE_ERROR + fileName, e);
                }
            }
        }
    }

    private void addGroupDebt(Map<String, Map<String, Set<DebtRecord>>> debts, String groupName, DebtRecord debt,
                              String user) {
        if (!debts.containsKey(groupName)) {
            debts.put(groupName, new HashMap<>());
        }

        addDebt(debts.get(groupName), debt, user);
    }

    public void splitBill(String creditor, String debtor, double amount, String reason)
        throws NonPositiveAmountException, ServerErrorException {
        if (amount <= 0) {
            throw new NonPositiveAmountException("The amount cannot be 0 or less");
        }

        double splitAmount = amount / 2;
        User debtorUser = UserRepository.toUser(debtor);
        User creditorUser = UserRepository.toUser(creditor);

        splitAmount = recalculateFriendsDebts(debtor, creditor, splitAmount);

        if (splitAmount > 0) {
            splitAmount = recalculateGroupDebts(debtor, creditor, splitAmount, debtorUser);

            addNewDebt(splitAmount, debtorUser, creditorUser, reason);
        }
    }

    private double recalculateFriendsDebts(String debtor, String creditor, double amount)
        throws ServerErrorException, NonPositiveAmountException {
        if (owesMoney.containsKey(creditor) && moneyOwed.containsKey(debtor)) {
            Set<DebtRecord> creditorOwesMoney = owesMoney.get(creditor);
            Set<DebtRecord> debtorMoneyOwed = moneyOwed.get(debtor);

            double recalculated =
                recalculateDebts(creditorOwesMoney, debtorMoneyOwed, amount, UserRepository.toUser(debtor));

            if (recalculated != amount) {
                amount = recalculated;
                Path creditorFile = Path.of(creditor + "_debts.txt");
                rewriteDebtFiles(creditorFile, creditorOwesMoney);
            }
        }
        return amount;
    }

    private void rewriteDebtFiles(Path fileName, Set<DebtRecord> debts) throws ServerErrorException {
        FileUtils.createFileIfNeeded(fileName);
        try (var bufferedWriter = Files.newBufferedWriter(fileName)) {

            for (DebtRecord debt : debts) {
                bufferedWriter.write(debt.toString() + System.lineSeparator());
            }
            bufferedWriter.flush();

        } catch (IOException e) {
            throw new ServerErrorException(FileUtils.WRITING_TO_FILE_ERROR + fileName, e);
        }
    }

    private void rewriteGroupDebtFiles(Path groupFile, Map<String, Set<DebtRecord>> debts, String groupName)
        throws ServerErrorException {
        FileUtils.createFileIfNeeded(groupFile);

        try (var bufferedWriter = Files.newBufferedWriter(groupFile)) {

            for (Set<DebtRecord> records : debts.values()) {
                for (DebtRecord debt : records) {
                    bufferedWriter.write(groupName + FileUtils.SINGLE_SPACE + debt + System.lineSeparator());
                    bufferedWriter.flush();
                }
            }

        } catch (IOException e) {
            throw new ServerErrorException(FileUtils.WRITING_TO_FILE_ERROR + groupFile, e);
        }
    }

    private void addNewDebt(double splitAmount, User debtor, User creditor, String reason) throws ServerErrorException {
        if (splitAmount > 0) {
            DebtRecord debt = new DebtRecord(debtor, creditor, splitAmount, reason);
            Path debtorFile = Path.of(debtor.getUsername() + "_debts.txt");

            addDebt(moneyOwed, debt, creditor.getUsername());
            addDebt(owesMoney, debt, debtor.getUsername());
            FileUtils.writeToFile(debtorFile, FileUtils.SINGLE_SPACE, debt);
            debtor.addNewDebt(debt);
        }
    }

    private double recalculateDebts(Set<DebtRecord> creditorOwesMoney, Set<DebtRecord> debtorMoneyOwed,
                                    double splitAmount, User debtorUser) throws NonPositiveAmountException {

        Iterator<DebtRecord> debtIterator = creditorOwesMoney.iterator();
        while (debtIterator.hasNext()) {
            DebtRecord debt = debtIterator.next();

            if (debt.creditor().equals(debtorUser)) {
                double difference = debt.payAmount(splitAmount);

                if (difference > 0) {
                    payDebtPartly(creditorOwesMoney, debtorMoneyOwed, debt, difference, debtIterator);
                    return 0;
                } else {
                    splitAmount = abs(difference);
                    debtIterator.remove();
                    debtorMoneyOwed.remove(debt);
                    if (splitAmount == 0) {
                        return 0;
                    }
                }
            }
        }
        return splitAmount;
    }

    private DebtRecord payDebtPartly(Set<DebtRecord> creditorOwesMoney, Set<DebtRecord> debtorMoneyOwed,
                                     DebtRecord debt, double amount, Iterator<DebtRecord> debtIterator) {
        DebtRecord partlyPaidDebt =
            new DebtRecord(debt.debtor(), debt.creditor(), amount, debt.reason());

        debtIterator.remove();
        creditorOwesMoney.add(partlyPaidDebt);

        debtorMoneyOwed.remove(debt);
        debtorMoneyOwed.add(partlyPaidDebt);

        return partlyPaidDebt;
    }

    private double recalculateGroupDebts(String debtor, String creditor, double splitAmount, User debtorUser)
        throws ServerErrorException, NonPositiveAmountException {
        Set<String> debtorGroups = UserRepository.toUser(debtor).getGroups();
        Set<String> creditorGroups = UserRepository.toUser(creditor).getGroups();

        for (String group : debtorGroups) {
            if (creditorGroups.contains(group)) {
                if (splitAmount == 0) {
                    return 0;
                }

                if (!(groupMoneyOwed.containsKey(group) && groupMoneyOwed.get(group).containsKey(debtor)) ||
                    !(groupOwesMoney.containsKey(group) && groupOwesMoney.get(group).containsKey(creditor))) {
                    continue;
                }

                Set<DebtRecord> debtorMoneyOwed = groupMoneyOwed.get(group).get(debtor);
                Set<DebtRecord> creditorOwesMoney = groupOwesMoney.get(group).get(creditor);

                double recalculated = recalculateDebts(creditorOwesMoney, debtorMoneyOwed, splitAmount, debtorUser);

                if (recalculated != splitAmount) {
                    splitAmount = recalculated;
                    Path groupFile = Path.of(group + "_group_debts.txt");
                    Map<String, Set<DebtRecord>> groupDebts = groupMoneyOwed.get(group);
                    rewriteGroupDebtFiles(groupFile, groupDebts, group);
                }
            }
        }
        return splitAmount;
    }

    private void addDebt(Map<String, Set<DebtRecord>> debtMoney, DebtRecord debt, String username) {
        if (!debtMoney.containsKey(username)) {
            debtMoney.put(username, new HashSet<>());
        }
        debtMoney.get(username).add(debt);
    }

    public void splitGroupBill(Group group, String creditor, double amount, String reason)
        throws NonPositiveAmountException, ServerErrorException {
        if (amount <= 0) {
            throw new NonPositiveAmountException("The amount cannot be 0 or less");
        }

        double splitAmount = amount / group.members().size();
        addGroupDebt(group, creditor, splitAmount, reason);
    }

    private void addGroupDebt(Group group, String creditor, double amount, String reason)
        throws ServerErrorException, NonPositiveAmountException {
        Set<DebtRecord> debts = new HashSet<>();

        for (User user : group.members()) {
            double splitAmount = amount;
            String debtor = user.getUsername();
            if (debtor.equals(creditor)) {
                continue;
            }

            splitAmount = recalculateFriendsDebts(debtor, creditor, splitAmount);

            if (splitAmount > 0) {
                splitAmount = recalculateGroupDebts(debtor, creditor, splitAmount, user);

                DebtRecord debt = addNewGroupDebt(splitAmount, debtor, creditor, reason, group.name());

                if (debt != null) {
                    debts.add(debt);
                }
            }
        }

        initializeIfNeeded(group.name(), creditor, groupMoneyOwed);
        groupMoneyOwed.get(group.name()).get(creditor).addAll(debts);
        Path groupFile = Path.of(group.name() + "_group_debts.txt");
        writeGroupDebtsToFile(debts, groupFile, group.name());
    }

    private DebtRecord addNewGroupDebt(double splitAmount, String debtor, String creditor, String reason,
                                       String groupName)
        throws ServerErrorException {
        if (splitAmount > 0) {
            User debtorUser = UserRepository.toUser(debtor);
            User creditorUser = UserRepository.toUser(creditor);

            DebtRecord debt = new DebtRecord(debtorUser, creditorUser, splitAmount, reason);

            initializeIfNeeded(groupName, debtor, groupOwesMoney);
            groupOwesMoney.get(groupName).get(debtor).add(debt);

            debtorUser.addNewGroupDebt(debt, groupName);
            return debt;
        }
        return null;
    }

    private void writeGroupDebtsToFile(Set<DebtRecord> debts, Path groupFile, String groupName)
        throws ServerErrorException {
        FileUtils.createFileIfNeeded(groupFile);

        try (var bufferedWriter = Files.newBufferedWriter(groupFile, StandardOpenOption.APPEND)) {

            for (DebtRecord debt : debts) {
                bufferedWriter.write(groupName + FileUtils.SINGLE_SPACE + debt + System.lineSeparator());
            }
            bufferedWriter.flush();

        } catch (IOException e) {
            throw new ServerErrorException(FileUtils.WRITING_TO_FILE_ERROR + groupFile, e);
        }
    }

    private void initializeIfNeeded(String groupName, String username,
                                    Map<String, Map<String, Set<DebtRecord>>> debtMoney) {
        if (!debtMoney.containsKey(groupName)) {
            debtMoney.put(groupName, new HashMap<>());
        }

        if (!debtMoney.get(groupName).containsKey(username)) {
            debtMoney.get(groupName).put(username, new HashSet<>());
        }
    }

    public Set<DebtRecord> getMoneyOwed(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }

        return moneyOwed.get(username);
    }

    public Set<DebtRecord> getOwesMoney(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }

        return owesMoney.get(username);
    }

    public Set<DebtRecord> getGroupMoneyOwed(String groupName, String username) {
        if (groupName == null || username == null) {
            throw new IllegalArgumentException("Group name and username cannot be null");
        }

        if (groupMoneyOwed.containsKey(groupName) && groupMoneyOwed.get(groupName).containsKey(username)) {
            return groupMoneyOwed.get(groupName).get(username);
        }

        return null;
    }

    public Set<DebtRecord> getGroupOwesMoney(String groupName, String username) {
        if (groupName == null || username == null) {
            throw new IllegalArgumentException("Group name and username cannot be null");
        }

        if (groupOwesMoney.containsKey(groupName) && groupOwesMoney.get(groupName).containsKey(username)) {
            return groupOwesMoney.get(groupName).get(username);
        }

        return null;
    }

    public Set<DebtRecord> pay(String username, double amount, String friend, Set<DebtRecord> partlyPaidDebts)
        throws NonPositiveAmountException, NoDebtsToBePaidException, ServerErrorException {
        if (username == null || friend == null || partlyPaidDebts == null) {
            throw new IllegalArgumentException("Username, friend and debts cannot be null");
        }

        if (amount <= 0) {
            throw new NonPositiveAmountException("The amount cannot be 0 or less");
        }

        Set<DebtRecord> paidDebts = new HashSet<>();

        Set<DebtRecord> debts = moneyOwed.get(username);
        Set<DebtRecord> friendDebts = owesMoney.get(friend);

        if (friendDebts != null && !friendDebts.isEmpty() && debts != null && !debts.isEmpty()) {
            amount = payDebts(debts, friendDebts, paidDebts, friend, amount, partlyPaidDebts);

            if (!paidDebts.isEmpty() || !partlyPaidDebts.isEmpty()) {
                Path debtorFile = Path.of(friend + "_debts.txt");
                rewriteDebtFiles(debtorFile, friendDebts);
            }
        }

        paidDebts.addAll(getPaidDebts(username, friend, amount, partlyPaidDebts, paidDebts));

        return paidDebts;
    }

    private Set<DebtRecord> getPaidDebts(String username, String friend, double amount, Set<DebtRecord> partlyPaidDebts,
                                         Set<DebtRecord> paidDebts)
        throws ServerErrorException, NoDebtsToBePaidException, NonPositiveAmountException {
        Set<String> userGroups = UserRepository.toUser(username).getGroups();
        Set<String> friendGroups = UserRepository.toUser(friend).getGroups();

        Set<DebtRecord> groupDebts = payGroupDebts(userGroups, friendGroups, username, friend, amount, partlyPaidDebts);

        if (groupDebts.isEmpty() && paidDebts.isEmpty() && partlyPaidDebts.isEmpty()) {
            throw new NoDebtsToBePaidException(friend + " doesn't owe you anything");
        }

        return groupDebts;
    }

    private Set<DebtRecord> payGroupDebts(Set<String> userGroups, Set<String> friendGroups, String username,
                                          String friend, double amount, Set<DebtRecord> partlyPaidDebts)
        throws ServerErrorException, NonPositiveAmountException {
        Set<DebtRecord> paidDebts = new HashSet<>();
        for (String group : userGroups) {

            if (friendGroups.contains(group)) {
                if (amount == 0) {
                    break;
                }

                if (!(groupMoneyOwed.containsKey(group) && groupMoneyOwed.get(group).containsKey(username)) ||
                    !(groupOwesMoney.containsKey(group) && groupOwesMoney.get(group).containsKey(friend))) {
                    continue;
                }

                Set<DebtRecord> debts = groupMoneyOwed.get(group).get(username);
                Set<DebtRecord> friendDebts = groupOwesMoney.get(group).get(friend);
                Set<DebtRecord> groupPaidDebts = new HashSet<>();

                amount = payDebts(debts, friendDebts, groupPaidDebts, friend, amount, partlyPaidDebts);

                if (!groupPaidDebts.isEmpty() || !partlyPaidDebts.isEmpty()) {
                    Path groupFile = Path.of(group + "_group_debts.txt");
                    rewriteGroupDebtFiles(groupFile, groupMoneyOwed.get(group), group);

                    paidDebts.addAll(groupPaidDebts);
                }
            }
        }
        return paidDebts;
    }

    private double payDebts(Set<DebtRecord> debts, Set<DebtRecord> friendDebts, Set<DebtRecord> paidDebts,
                            String friend, double amount, Set<DebtRecord> partlyPaidDebts)
        throws ServerErrorException, NonPositiveAmountException {
        Iterator<DebtRecord> debtIterator = debts.iterator();
        while (debtIterator.hasNext()) {
            DebtRecord debt = debtIterator.next();

            if (debt.debtor().getUsername().equals(friend)) {
                double difference = debt.payAmount(amount);

                if (difference > 0) {
                    DebtRecord partlyPaidDebt = payDebtPartly(debts, friendDebts, debt, difference, debtIterator);

                    partlyPaidDebts.add(partlyPaidDebt);
                    debt.debtor().addPartlyPaidDebt(partlyPaidDebt);

                    return 0;
                } else {
                    amount = abs(difference);
                    debt.debtor().addPaidDebt(debt);
                    paidDebts.add(debt);
                    debtIterator.remove();
                    friendDebts.remove(debt);

                    if (amount == 0) {
                        return 0;
                    }
                }
            }
        }
        return amount;
    }
}