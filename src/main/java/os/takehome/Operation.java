package os.takehome;

import java.util.function.Function;

public enum Operation {
    SQUARE(x -> (double) x * x, 's'),
    CUBE(x -> (double) x * x * x, 'c'),
    SQRT(x -> Math.sqrt(x), 'r'),
    DOUBLE(x -> (double) x * 2, 'd');

    private final Function<Integer, Double> function;
    public char symbol;

    Operation(Function<Integer, Double> function, char symbol) {
        this.function = function;
        this.symbol = symbol;
    }

    public static Operation fromSymbol(char symbol) {
        for (Operation op : values()) {
            if (op.symbol == symbol) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown operation symbol: " + symbol);
    }

    public Function<Integer, Double> getFunction() {
        return function;
    }
}