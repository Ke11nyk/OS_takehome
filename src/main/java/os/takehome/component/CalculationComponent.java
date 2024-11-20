package os.takehome.component;

public interface CalculationComponent {
    double calculate(int input);
    char getSymbol();
}

class FactorialComponent implements CalculationComponent {
    @Override
    public double calculate(int input) {
        try {
            // Симулюємо складні обчислення
            long delay = 5000 + (input * 1000L); // Базова затримка 5 секунд + 1 секунда на кожну одиницю входу
            Thread.sleep(delay);

            double result = 1;
            for(int i = 1; i <= input; i++) {
                result *= i;
                Thread.sleep(500); // Додаткова затримка на кожній ітерації
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public char getSymbol() {
        return 'F';
    }
}

class FibonacciComponent implements CalculationComponent {
    @Override
    public double calculate(int input) {
        try {
            Thread.sleep(7000); // Базова затримка 7 секунд

            if (input <= 1) return input;
            double prev = 0, current = 1;

            for (int i = 2; i <= input; i++) {
                Thread.sleep(800); // Затримка на кожній ітерації
                double temp = current;
                current = prev + current;
                prev = temp;
            }
            return current;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public char getSymbol() {
        return 'B';
    }
}

class PrimeCheckComponent implements CalculationComponent {
    @Override
    public double calculate(int input) {
        try {
            Thread.sleep(6000); // Базова затримка 6 секунд

            if (input <= 1) return 0;
            for (int i = 2; i <= Math.sqrt(input); i++) {
                Thread.sleep(1000); // Затримка на кожній перевірці
                if (input % i == 0) return 0;
            }
            return 1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public char getSymbol() {
        return 'P';
    }
}

class SqrtComponent implements CalculationComponent {
    @Override
    public double calculate(int input) {
        try {
            // Симулюємо складні обчислення методом Ньютона
            Thread.sleep(8000); // Базова затримка 8 секунд

            double x = input;
            double root;
            int iterations = 10;

            for(int i = 0; i < iterations; i++) {
                Thread.sleep(500); // Затримка на кожній ітерації
                root = 0.5 * (x + (input / x));
                if (Math.abs(root - x) < 0.0001) break;
                x = root;
            }

            return x;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public char getSymbol() {
        return 'S';
    }
}