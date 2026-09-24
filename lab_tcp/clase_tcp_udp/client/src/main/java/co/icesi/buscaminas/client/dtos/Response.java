package co.icesi.buscaminas.client.dtos;

import java.util.ArrayList;
import java.util.Map;

public class Response {
    private String status;
    private Map<String, Object> data;

    public Response() {}

    public Response(String status, Map<String, Object> data) {
        this.status = status;
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getMessage() {
        if (data == null) return null;
        Object message = data.get("message");
        return message == null ? null : String.valueOf(message);
    }

    public Cell[][] getBoard() {
        if (data == null) return null;
        Object boardData = data.get("board");
        if (!(boardData instanceof ArrayList<?> rows) || rows.isEmpty()) {
            return null;
        }

        Cell[][] board = new Cell[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            Object rowValue = rows.get(i);
            if (!(rowValue instanceof ArrayList<?> rowCells)) {
                return null;
            }
            Cell[] row = new Cell[rowCells.size()];
            for (int j = 0; j < rowCells.size(); j++) {
                Object cellValue = rowCells.get(j);
                if (cellValue instanceof Map<?, ?> cellMap) {
                    row[j] = mapCell(cellMap);
                } else {
                    row[j] = new Cell();
                }
            }
            board[i] = row;
        }
        return board;
    }

    private Cell mapCell(Map<?, ?> cellMap) {
        Cell cell = new Cell();
        Object mine = cellMap.get("isLandMine");
        if (mine instanceof Boolean value) cell.setMine(value);

        Object hidden = cellMap.get("hide");
        if (hidden instanceof Boolean value) cell.setRevealed(!value);

        Object marked = cellMap.get("isMarked");
        if (marked instanceof Boolean value) cell.setFlagged(value);

        Object value = cellMap.get("value");
        if (value instanceof Number number) cell.setAdjacentMines(number.intValue());

        return cell;
    }
}