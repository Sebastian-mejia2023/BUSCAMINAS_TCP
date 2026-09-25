package co.icesi.buscaminas.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Modelo del juego. Hay UNA sola instancia compartida por todos los hilos del ThreadPool,
 * asi que todo metodo que lee o modifica el tablero es synchronized (monitor = this).
 */
public class BoardGame {

    public static final int MAX_DIMENSION = 100;

    // Estados posibles de la partida
    public enum GameState {
        NOT_STARTED,
        IN_PROGRESS,
        WON,
        LOST
    }

    private Cell[][] board;
    private int mines;
    private GameState state = GameState.NOT_STARTED;

    public synchronized GameState getState() {
        return state;
    }

    public synchronized int getMines() {
        return mines;
    }

    public synchronized int initGame(int n, int m, int mines){
        if (n < 1 || m < 1 || n > MAX_DIMENSION || m > MAX_DIMENSION) {
            throw new IllegalArgumentException("Dimensiones invalidas: n y m deben estar entre 1 y " + MAX_DIMENSION);
        }
        if (mines < 1 || mines >= n * m) {
            throw new IllegalArgumentException("Numero de minas invalido: debe estar entre 1 y " + (n * m - 1));
        }

        Cell[][] newBoard = new Cell[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                newBoard[i][j] = new Cell(false, 0);
            }
        }
        Random rd = new Random();
        int placed = 0;
        while (placed < mines) {
            Cell cell = newBoard[rd.nextInt(n)][rd.nextInt(m)];
            if (!cell.isLandMine()) {
                cell.setLandMine(true);
                placed++;
            }
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (!newBoard[i][j].isLandMine()) {
                    newBoard[i][j].setValue(countMinesAround(newBoard, i, j));
                }
            }
        }
        this.board = newBoard;
        this.mines = mines;
        this.state = GameState.IN_PROGRESS;
        return placed;
    }

    public synchronized void showAll(boolean show){
        requireBoard();
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                board[i][j].setShowAll(show);
            }
        }
    }

    private static boolean inBounds(Cell[][] b, int i, int j) {
        return i >= 0 && i < b.length && j >= 0 && j < b[0].length;
    }

    private static int countMinesAround(Cell[][] b, int i, int j) {
        int count = 0;
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                if (di == 0 && dj == 0) continue;
                int ni = i + di, nj = j + dj;
                if (inBounds(b, ni, nj) && b[ni][nj].isLandMine()) count++;
            }
        }
        return count;
    }

    private void requireBoard() {
        if (board == null) {
            throw new IllegalStateException("El juego no ha sido inicializado");
        }
    }

    private void checkBounds(int i, int j) {
        if (!inBounds(board, i, j)) {
            throw new IllegalArgumentException("Celda no valida");
        }
    }

    public synchronized void printBoard(){
        requireBoard();
        System.out.println();
        System.out.print("   ");
        for (int i = 0; i < board[0].length; i++) {
            System.out.print(" " + i);
        }
        System.out.println();
        for (int i = 0; i < board.length; i++) {
            System.out.print(i + " [");
            for (int j = 0; j < board[0].length; j++) {
                System.out.print(" " + board[i][j]);
            }
            System.out.println(" ]");
        }
    }

    /**
     * Procesa la seleccion de una casilla por parte del cliente.
     * Retorna true si la jugada fue exitosa (continua el juego)
     * o false si piso una mina (BOOM) o el juego ya habia terminado.
     */
    public synchronized boolean selectCell(int row, int col) {
        requireBoard();
        checkBounds(row, col);

        if (this.state == GameState.LOST || this.state == GameState.WON) {
            return false;
        }

        Cell cell = board[row][col];

        // Si la casilla seleccionada estaba marcada, quitamos la marca al destapar
        if (cell.isMarked()) {
            cell.setMarked(false);
        }

        // Si es una mina, pierde
        if (cell.isLandMine()) {
            this.state = GameState.LOST;
            showAll(true);
            return false;
        }

        // Revela la casilla y aplica cascada BFS
        showCells(row, col);

        // SOLO SI ya no quedan casillas seguras ocultas en TODO el tablero, se marca WON
        if (validWin()) {
            this.state = GameState.WON;
            showAll(true);
        }

        return true;
    }
    private boolean validWin() {
        int pendingSafeCells = 0;
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                Cell cell = board[i][j];
                // Una casilla cuenta como "pendiente" si NO es mina y SIGUE oculta (hide == true)
                if (!cell.isLandMine() && cell.isHide()) {
                    pendingSafeCells++;
                }
            }
        }
        // Si no queda ninguna casilla segura oculta, entonces GANÓ
        return pendingSafeCells == 0;
    }

    private void showCells(int startI, int startJ) {
        Deque<int[]> pending = new ArrayDeque<>();
        pending.push(new int[]{startI, startJ});

        while (!pending.isEmpty()) {
            int[] pos = pending.pop();
            int i = pos[0], j = pos[1];

            if (!inBounds(board, i, j)) continue;

            Cell cell = board[i][j];

            // Ignorar si ya esta destapada, si esta marcada o si es mina
            if (!cell.isHide() || cell.isMarked() || cell.isLandMine()) continue;

            // Revelar unicamente esta celda
            cell.setHide(false);

            // Expandir a las 8 vecinas SOLO si la celda es un cero
            if (cell.getValue() == 0) {
                for (int di = -1; di <= 1; di++) {
                    for (int dj = -1; dj <= 1; dj++) {
                        if (di != 0 || dj != 0) {
                            pending.push(new int[]{i + di, j + dj});
                        }
                    }
                }
            }
        }
    }

    /** Devuelve una copia (foto) del tablero. */

    public synchronized Cell[][] getBoard() {
        requireBoard();
        Cell[][] snapshot = new Cell[board.length][board[0].length];

        // Si la partida ya terminó (ganó o perdió), la foto debe revelar todo el tablero
        boolean isFinished = (this.state == GameState.LOST || this.state == GameState.WON);

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                Cell copy = board[i][j].copy();
                if (isFinished) {
                    copy.setShowAll(true);
                }
                snapshot[i][j] = copy;
            }
        }
        return snapshot;
    }

    public synchronized void markCell(int i, int j) {
        requireBoard();
        checkBounds(i, j);
        if (this.state == GameState.LOST || this.state == GameState.WON) {
            return;
        }
        Cell cell = board[i][j];
        if (cell.isHide()) {
            cell.setMarked(!cell.isMarked());
        }
    }
}