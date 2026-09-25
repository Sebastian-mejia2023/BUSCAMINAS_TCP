package co.icesi.buscaminas;

import java.util.Scanner;

import co.icesi.buscaminas.controllers.TCPController;
import co.icesi.buscaminas.model.BoardGame;
import co.icesi.buscaminas.services.ServicesImpl;

public class Main {

    /** Uso: Main [puerto] [--console]   (puerto por defecto: 12345) */
    public static void main(String[] args)
    {
        int port = 12345;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                if (port < 1 || port > 65535) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                System.err.println("Puerto invalido: " + args[0] + ". Uso: Main [puerto] [--console]");
                System.exit(1);
            }
        }

        ServicesImpl serv = new ServicesImpl();
        // La consola local es opcional: es un "cliente" extra que muta el mismo tablero compartido
        // y, con 'gradlew run', no tiene stdin disponible.
        if (args.length > 1 && args[1].equals("--console")) {
            new Thread(() -> apply(serv.getGame())).start();
        }

        TCPController iceController = new TCPController(serv, port);
        iceController.startService();
    }
    public static void apply(BoardGame bg) {

        int n = bg.getBoard().length;
        int m = bg.getBoard()[0].length;
        System.out.println("LandMines on the table: "+ bg.getMines());
//        bg.showAll(true);
//        bg.printBoard();
//        bg.showAll(false);
        bg.printBoard();
        Scanner scanner = new Scanner(System.in);
        System.out.println("select a cell (i,j) between 0 and "+(n-1)+","+(m-1)+" to play, or (-1,-1) to exit");
        System.out.println("you have "+bg.getMines()+" mines to avoid");
        System.out.println("use the format: <operation> <i> <j>");
        System.out.println("operation 1: select cell, operation 2: mark/unmark cell");
        int operation = scanner.nextInt();
        int i = scanner.nextInt();
        int j = scanner.nextInt();
        do{
            try {
                if(operation==2){
                    bg.markCell(i,j);
                }else if(operation ==1){
                    boolean alive = bg.selectCell(i, j);
                    if (bg.getState() == BoardGame.GameState.WON) {
                        bg.printBoard();
                        System.out.println("you win, Congratulations");
                        break;
                    } else if (bg.getState() == BoardGame.GameState.LOST) {
                        bg.printBoard();
                        System.out.println("BOOM: pisaste una mina");
                        break;
                    }
                }
                bg.printBoard();
            }catch (RuntimeException e){
                System.out.println(e.getMessage());
                break;
            }
            operation = scanner.nextInt();
            i = scanner.nextInt();
            j = scanner.nextInt();
        }while (i>=0 && j>=0);
        bg.showAll(true);
        bg.printBoard();
        System.out.println("exit");
        scanner.close();

    }
}