package os.takehome;

import java.util.*;

class ComponentFactory {
    private static final Map<Character, CalculationComponent> COMPONENTS = new HashMap<>();

    static {
        COMPONENTS.put('F', new FactorialComponent());     // Факторіал
        COMPONENTS.put('B', new FibonacciComponent());     // Числа Фібоначчі
        COMPONENTS.put('P', new PrimeCheckComponent());    // Перевірка на простоту
        COMPONENTS.put('S', new SqrtComponent());         // Квадратний корінь
    }

    public static CalculationComponent getComponent(char symbol) {
        CalculationComponent component = COMPONENTS.get(symbol);
        if (component == null) {
            throw new IllegalArgumentException("Unknown component symbol: " + symbol);
        }
        return component;
    }

    public static boolean isValidSymbol(char symbol) {
        return COMPONENTS.containsKey(symbol);
    }
}