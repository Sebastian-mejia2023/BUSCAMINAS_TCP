package co.icesi.buscaminas.client.ui;

import co.icesi.buscaminas.client.dtos.Cell;

public class BoardRenderer {

    private static final String RESET = "\u001B[0m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String CYAN = "\u001B[36m";

    public static void render(Cell[][] board) {
        if (board == null || board.length == 0) {
            System.out.println("El tablero esta vacio o no se ha inicializado en el servidor.");
            return;
        }

        int rows = board.length;
        int cols = board[0].length;

        System.out.println("\n================= TABLERO ACTUALIZADO =================");
        System.out.print("     ");
        for (int c = 0; c < cols; c++) {
            System.out.printf(" %2d ", c);
        }
        System.out.println();

        System.out.print("     +");
        for (int c = 0; c < cols; c++) {
            System.out.print("----");
        }
        System.out.println("+");

        for (int r = 0; r < rows; r++) {
            System.out.printf(CYAN + "%2d |" + RESET, r);
            for (int c = 0; c < cols; c++) {
                Cell cell = board[r][c];
                String value = formatCellValue(cell);
                String colored = colorizeCell(value, cell);
                System.out.printf(" %2s |", colored);
            }
            System.out.println();
        }
        System.out.println("========================================================\n");
    }

    private static String formatCellValue(Cell cell) {
        if (cell == null) {
            return ".";
        }

        if (cell.isFlagged()) {
            return "M";
        }

        // Si la casilla NO está revelada Y tampoco se solicitó mostrar todo (showAll), sigue oculta
        if (!cell.isRevealed() && !cell.isShowAll()) {
            return ".";
        }

        // Si llegó hasta aquí, la celda o está revelada o es fin de juego (showAll = true)
        if (cell.isMine()) {
            return "*";
        }

        int adj = cell.getAdjacentMines();
        return adj > 0 ? String.valueOf(adj) : "0";
    }

    private static String colorizeCell(String value, Cell cell) {
        if (cell == null) {
            return value;
        }
        if (cell.isFlagged()) {
            return YELLOW + value + RESET;
        }

        // Si no está revelada y no es showAll, retorna el valor sin color especial
        if (!cell.isRevealed() && !cell.isShowAll()) {
            return value;
        }

        if (cell.isMine()) {
            return RED + value + RESET;
        }

        int adj = cell.getAdjacentMines();
        if (adj > 0) {
            return GREEN + value + RESET;
        }
        return value;
    }
}