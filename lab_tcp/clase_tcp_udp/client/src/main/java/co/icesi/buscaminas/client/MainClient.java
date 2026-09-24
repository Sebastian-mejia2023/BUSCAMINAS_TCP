package co.icesi.buscaminas.client;

import java.util.Scanner;

import co.icesi.buscaminas.client.dtos.Response;
import co.icesi.buscaminas.client.net.BuscaminasTCPClient;
import co.icesi.buscaminas.client.ui.BoardRenderer;

public class MainClient {

    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        BuscaminasTCPClient client = new BuscaminasTCPClient("localhost", 12345);

        printWelcomeBanner();

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print(CYAN + "Seleccione una opcion [1-6]: " + RESET);

            if (!scanner.hasNextLine()) break;
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    handleInitGame(scanner, client);
                    break;
                case "2":
                    handleSelectCell(scanner, client);
                    break;
                case "3":
                    handleMarkCell(scanner, client);
                    break;
                case "4":
                    handleGetBoard(client);
                    break;
                case "5":
                    handleSowAll(client);
                    break;
                case "6":
                    System.out.println(GREEN + "\nGracias por jugar Buscaminas. Conexion cerrada con el servidor." + RESET);
                    client.disconnect();
                    running = false;
                    break;
                default:
                    System.out.println(RED + "Opcion invalida. Por favor digite un numero del 1 al 6." + RESET);
            }
            System.out.println();
        }
        scanner.close();
    }

    private static void printWelcomeBanner() {
        System.out.println(CYAN + "==========================================================" + RESET);
        System.out.println(CYAN + "          CLIENTE BUSCAMINAS TCP - UNIVERSIDAD ICESI       " + RESET);
        System.out.println(CYAN + "==========================================================" + RESET);
        System.out.println(" GUIA RAPIDA DE INSTRUCCIONES:");
        System.out.println("  1. Primero debes crear la partida usando la Opcion 1.");
        System.out.println("  2. El sistema utiliza coordenadas numericas iniciando desde 0.");
        System.out.println("     Ejemplo: En un tablero de 4x4, las filas son (0, 1, 2, 3)");
        System.out.println("     y las columnas son (0, 1, 2, 3).");
        System.out.println("  3. Simbologia del tablero:");
        System.out.println("     - [ . ] : Casilla oculta (no destapada)");
        System.out.println("     - [ M ] : Casilla marcada con bandera de advertencia");
        System.out.println("     - [ * ] : Mina revelada");
        System.out.println("     - [0-8]: Numero de minas alrededor de esa casilla");
        System.out.println(CYAN + "==========================================================" + RESET);
    }

    private static void printMenu() {
        System.out.println();
        System.out.println(CYAN + "=========================================================" + RESET);
        System.out.println("--- MENU PRINCIPAL DE ACCIONES ---");
        System.out.println(" 1. Iniciar nueva partida (INIT_GAME)");
        System.out.println(" 2. Destapar una casilla del tablero (SELECT_CELL)");
        System.out.println(" 3. Marcar o desmarcar casilla con bandera (MARK_CELL)");
        System.out.println(" 4. Consultar estado actual del tablero (GET_BOARD)");
        System.out.println(" 5. Rendirse y revelar todas las minas (SOW_ALL)");
        System.out.println(" 6. Salir del programa");
        System.out.println(CYAN + "=========================================================" + RESET);
    }

    private static void handleInitGame(Scanner scanner, BuscaminasTCPClient client) {
        System.out.println(YELLOW + "\n--- OPCION 1: CONFIGURAR NUEVA PARTIDA ---" + RESET);

        int rows = readInt(scanner, "Ingresa la cantidad de FILAS (ejemplo: 4): ");
        int cols = readInt(scanner, "Ingresa la cantidad de COLUMNAS (ejemplo: 4): ");
        int mines = readInt(scanner, "Ingresa la cantidad de MINAS (ejemplo: 2): ");

        Response res = client.initGame(rows, cols, mines);
        processAndRenderBoard(res, client);
    }

    private static void handleSelectCell(Scanner scanner, BuscaminasTCPClient client) {
        System.out.println(YELLOW + "\n--- OPCION 2: DESTAPAR CASILLA ---" + RESET);

        int row = readInt(scanner, "Ingresa el numero de FILA (comenzando desde 0): ");
        int col = readInt(scanner, "Ingresa el numero de COLUMNA (comenzando desde 0): ");

        Response res = client.selectCell(row, col);
        processAndRenderBoard(res, client);
    }

    private static void handleMarkCell(Scanner scanner, BuscaminasTCPClient client) {
        System.out.println(YELLOW + "\n--- OPCION 3: MARCAR / DESMARCAR CASILLA ---" + RESET);

        int row = readInt(scanner, "Ingresa el numero de FILA a marcar (comenzando desde 0): ");
        int col = readInt(scanner, "Ingresa el numero de COLUMNA a marcar (comenzando desde 0): ");

        Response res = client.markCell(row, col);
        processAndRenderBoard(res, client);
    }

    private static void handleGetBoard(BuscaminasTCPClient client) {
        System.out.println(YELLOW + "\n--- OPCION 4: CONSULTAR ESTADO ACTUAL DEL TABLERO ---" + RESET);
        Response res = client.getBoard();
        processAndRenderBoard(res, client);
    }

    private static void handleSowAll(BuscaminasTCPClient client) {
        System.out.println(YELLOW + "\n--- OPCION 5: RENDIMIENTO Y REVELACION COMPLETA ---" + RESET);
        Response res = client.sowAll();
        processAndRenderBoard(res, client);
    }

    /**
     * Procesa la respuesta de la accion y garantiza la renderizacion
     * inmediata del tablero en consola.
     */
    private static void processAndRenderBoard(Response res, BuscaminasTCPClient client) {
        if (res == null) {
            System.out.println(RED + "Error: No se recibio respuesta desde el servidor TCP." + RESET);
            return;
        }

        String message = res.getMessage();
        if (message == null || message.isBlank()) {
            message = res.getStatus() == null ? "Operacion completada" : res.getStatus();
        }
        System.out.println();
        System.out.println("Respuesta del Servidor: " + GREEN + message + RESET);
        System.out.println(CYAN + "---------------------------------------------------------" + RESET);

        if (res.getBoard() != null) {
            BoardRenderer.render(res.getBoard());
        } else {
            Response boardRes = client.getBoard();
            if (boardRes != null && boardRes.getBoard() != null) {
                BoardRenderer.render(boardRes.getBoard());
            } else {
                System.out.println(RED + "Atencion: No hay un tablero iniciado en el servidor." + RESET);
            }
        }
    }

    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println(RED + "Error: Debes ingresar un numero entero valido. Intentalo de nuevo." + RESET);
            }
        }
    }
}