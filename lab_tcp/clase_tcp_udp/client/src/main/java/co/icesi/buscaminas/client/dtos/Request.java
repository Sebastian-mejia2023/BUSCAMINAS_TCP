package co.icesi.buscaminas.client.dtos;

import java.util.HashMap;
import java.util.Map;

public class Request {
    private String action;
    private Map<String, String> data;

    public Request() {
        this.data = new HashMap<>();
    }

    public Request(String action, int rows, int cols, int mines, int row, int col) {
        this();
        this.action = action;
        if (action.equals("INIT_GAME")) {
            data.put("n", String.valueOf(rows));
            data.put("m", String.valueOf(cols));
            data.put("minas", String.valueOf(mines));
        } else if (action.equals("SELECT_CELL") || action.equals("MARK_CELL")) {
            data.put("i", String.valueOf(row));
            data.put("j", String.valueOf(col));
        }
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Map<String, String> getData() { return data; }
    public void setData(Map<String, String> data) { this.data = data; }
}