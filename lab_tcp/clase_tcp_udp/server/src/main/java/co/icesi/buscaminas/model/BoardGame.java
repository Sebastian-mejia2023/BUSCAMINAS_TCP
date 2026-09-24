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

    private Cell[][] board;

    private int mines;

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
        // Se construye en una variable local y solo al final se publica: nunca queda un tablero a medias.
        Cell[][] newBoard = new Cell[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                newBoard[i][j] = new Cell(false, 0);
            }
        }
        Random rd = new Random();
        int placed = 0;
        while (placed < mines) {          // minas distintas: siempre quedan exactamente 'mines'
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
            throw new IllegalArgumentException("Cell no valid");
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
        for (int i = 0; i <board.length; i++) {
            System.out.print(i+" [");
            for (int j = 0; j < board[0].length; j++) {
                System.out.print(" "+board[i][j]);
            }
            System.out.println(" ]");
        }
    }

    public synchronized boolean selectCell(int i, int j){
        requireBoard();
        checkBounds(i, j);
        Cell cell = board[i][j];
        if (cell.isMarked()) {
            return validWin();            // una casilla con bandera esta protegida: hay que desmarcarla primero
        }
        if(cell.isLandMine()){
            showAll(true);
            throw new GameOverException("Game over");
        }
        if (cell.isHide()) {
            showCells(i, j);
        }
        return validWin();
    }

    private boolean validWin(){
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (board[i][j].isHide() && !board[i][j].isLandMine()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Expansion iterativa (BFS): una recursion en tableros grandes podia causar StackOverflowError. */
    private void showCells(int startI, int startJ) {
        Deque<int[]> pending = new ArrayDeque<>();
        pending.push(new int[]{startI, startJ});
        while (!pending.isEmpty()) {
            int[] pos = pending.pop();
            int i = pos[0], j = pos[1];
            if (!inBounds(board, i, j)) continue;
            Cell cell = board[i][j];
            if (!cell.isHide() || cell.isMarked() || cell.isLandMine()) continue;
            cell.setHide(false);
            if (cell.getValue() == 0) {
                for (int di = -1; di <= 1; di++) {
                    for (int dj = -1; dj <= 1; dj++) {
                        if (di != 0 || dj != 0) pending.push(new int[]{i + di, j + dj});
                    }
                }
            }
        }
    }

    /** Devuelve una copia (foto) del tablero, para que otro hilo no lo mute mientras Gson lo serializa. */
    public synchronized Cell[][] getBoard() {
        requireBoard();
        Cell[][] snapshot = new Cell[board.length][board[0].length];
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                snapshot[i][j] = board[i][j].copy();
            }
        }
        return snapshot;
    }

    public synchronized void markCell(int i, int j) {
        requireBoard();
        checkBounds(i, j);
        Cell cell = board[i][j];
        if (cell.isHide()) {              // el contrato: solo se marca si la celda esta oculta
            cell.setMarked(!cell.isMarked());
        }
    }
}