package os.takehome.component;

import java.net.*;
import java.util.concurrent.*;

public class Component {
    private final int index;
    private final char symbol;
    private Future<Double> result;
    private ComponentStatus status;
    private final Socket socket;

    public Component(int index, char symbol, Socket socket) {
        this.index = index;
        this.symbol = symbol;
        this.socket = socket;
        this.status = ComponentStatus.CREATED;
    }

    public int getIndex() { return index; }
    public char getSymbol() { return symbol; }
    public Future<Double> getResult() { return result; }
    public void setResult(Future<Double> result) { this.result = result; }
    public ComponentStatus getStatus() { return status; }
    public void setStatus(ComponentStatus status) { this.status = status; }
    public Socket getSocket() { return socket; }
}