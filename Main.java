import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class Account {
    private double balance;
    private final ReentrantLock lock;
    private final Condition condition;

    public Account() {
        this(0.0);
    }

    public Account(double balance) {
        this.balance = balance;
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
    }

    public void deposit(double amount) {
        lock.lock();
        try {
            balance += amount;
            System.out.println("Пополнение: " + amount + " | Баланс: " + balance);
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void withdraw(double amount) {
        lock.lock();
        try {
            balance -= amount;
            System.out.println("Снятие: " + amount + " | Баланс: " + balance);
        } finally {
            lock.unlock();
        }
    }

    public void waitForAmount(double targetAmount) {
        lock.lock();
        try {
            while (balance < targetAmount) {
                System.out.println("Ожидание пополнения... (Требуется: " + targetAmount + ", текущий баланс: " + balance + ")");
                condition.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted", e);
        } finally {
            lock.unlock();
        }
    }

    public double getBalance() {
        lock.lock();
        try {
            return balance;
        } finally {
            lock.unlock();
        }
    }
}

public class Main {
    public static void main(String[] args) {
        Account account = new Account();

        Thread depositThread = new Thread(() -> {
            Random random = new Random();
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(500);
                    account.deposit(random.nextDouble(50.0, 200.0));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        depositThread.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        double targetAmount = 500.0;
        account.waitForAmount(targetAmount);
        account.withdraw(targetAmount);

        try {
            depositThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Финальный баланс: " + account.getBalance());
    }
}