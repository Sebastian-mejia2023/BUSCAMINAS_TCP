package co.icesi.buscaminas.controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.icesi.buscaminas.model.BoardGame;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import co.icesi.buscaminas.controllers.dtos.Request;
import co.icesi.buscaminas.controllers.dtos.Response;
import co.icesi.buscaminas.model.GameOverException;
import co.icesi.buscaminas.services.ServicesImpl;

public class TCPController {

    private static final int POOL_SIZE = 5;
    private static final int BACKLOG = 50;
    /** Un cliente que conecta y no envia nada no puede secuestrar un hilo del pool para siempre. */
    private static final int CLIENT_TIMEOUT_MS = 10_000;

    private final ServicesImpl services;

    private final ServerSocket serverSocket;

    private volatile boolean running;

    private final ExecutorService executor;

    private final Gson gson;

    public TCPController(ServicesImpl services) {
        this(services, 12345);
    }

    public TCPController(ServicesImpl services, int port) {
        this.services = services;
        try {
            // 0.0.0.0 = todas las interfaces; ya no depende de una IP fija de una maquina concreta.
            serverSocket = new ServerSocket(port, BACKLOG, InetAddress.getByName("0.0.0.0"));
        } catch (IOException e) {
            // Antes se hacia printStackTrace y el servidor "arrancaba" con serverSocket == null.
            throw new UncheckedIOException("No se pudo abrir el puerto " + port, e);
        }
        executor = Executors.newFixedThreadPool(POOL_SIZE);
        gson = new GsonBuilder().create();
        running = true;
    }

    public void setRunning(boolean running) {
        if (running) {
            this.running = true;
        } else {
            stop();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        executor.shutdown();
    }

    public void startService() {
        System.out.println("TCP Service started on port " + serverSocket.getLocalPort()
                + " (ThreadPool de " + POOL_SIZE + " hilos)");
        while (running) {
            try {
                executor.execute(new TCPClientHandler(serverSocket.accept(), services));
            } catch (Exception e) {
                if (serverSocket.isClosed()) {
                    break;
                }
                e.printStackTrace();
            }
        }
        stop();
    }

    class TCPClientHandler implements Runnable {
        private final Socket clientSocket;
        private final ServicesImpl services;

        public TCPClientHandler(Socket clientSocket, ServicesImpl services) {
            this.clientSocket = clientSocket;
            this.services = services;
        }

        @Override
        public void run() {
            String thread = Thread.currentThread().getName();
            String remote = String.valueOf(clientSocket.getRemoteSocketAddress());
            System.out.println("[" + thread + "] Client connected: " + remote);
            // try-with-resources: el socket se cierra SIEMPRE, incluso si algo falla.
            try (Socket socket = clientSocket;
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter writer = new BufferedWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
                socket.setSoTimeout(CLIENT_TIMEOUT_MS);

                String line = reader.readLine();
                Response response = process(line, thread);

                writer.write(gson.toJson(response));
                writer.newLine();
                writer.flush();
            } catch (Exception e) {
                System.out.println("[" + thread + "] Error con " + remote + ": " + e);
            }
            System.out.println("[" + thread + "] Client disconnected: " + remote);
        }

        /** Siempre devuelve una respuesta (OK o ERROR): el cliente nunca se queda esperando. */
        private Response process(String line, String thread) {
            try {
                Request rq = (line == null) ? null : gson.fromJson(line, Request.class);
                if (rq == null || rq.action == null) {
                    return error("Peticion invalida: se esperaba un JSON con el campo 'action'");
                }
                Map<String, String> data = (rq.data != null) ? rq.data : new HashMap<>();
                System.out.println("[" + thread + "] Action: " + rq.action);

                Response response = new Response();
                response.data = new HashMap<>();
                switch (rq.action) {
                    case "INIT_GAME": {
                        int n = parseInt(data, "n");
                        int m = parseInt(data, "m");
                        int minas = parseInt(data, "minas");
                        services.initGame(n, m, minas);
                        response.status = "OK";
                        response.data.put("board", services.printBoard());
                        break;
                    }
                    case "SELECT_CELL": {
                        int i = parseInt(data, "i");
                        int j = parseInt(data, "j");
                        try {
                            boolean alive = services.selectCell(i, j);
                            boolean isWon = (services.getGame().getState() == BoardGame.GameState.WON);
                            boolean isLost = (services.getGame().getState() == BoardGame.GameState.LOST);

                            response.data.put("win", isWon);
                            response.data.put("gameEnd", isWon || isLost);

                            if (isWon) {
                                response.data.put("message", "Victoria: todas las casillas seguras destapadas");
                            } else if (isLost) {
                                response.data.put("message", "BOOM: pisaste una mina");
                            } else {
                                response.data.put("message", "Celda destapada");
                            }
                        } catch (GameOverException e) {
                            response.data.put("win", false);
                            response.data.put("gameEnd", true);
                            response.data.put("message", "BOOM: pisaste una mina");
                        }
                        response.status = "OK";
                        response.data.put("board", services.printBoard());
                        break;
                    }
                    case "MARK_CELL": {
                        int mi = parseInt(data, "i");
                        int mj = parseInt(data, "j");
                        services.markCell(mi, mj);
                        response.status = "OK";
                        response.data.put("board", services.printBoard());
                        break;
                    }
                    case "GET_BOARD":
                        response.status = "OK";
                        response.data.put("board", services.printBoard());
                        break;
                    case "SOW_ALL":
                        services.showAll(true);
                        response.status = "OK";
                        response.data.put("board", services.printBoard());
                        break;
                    default:
                        return error("Accion desconocida: " + rq.action);
                }
                return response;
            } catch (JsonParseException e) {
                return error("JSON invalido");
            } catch (IllegalArgumentException | IllegalStateException e) {
                return error(e.getMessage());
            } catch (RuntimeException e) {
                e.printStackTrace();
                return error("Error interno del servidor");
            }
        }

        private int parseInt(Map<String, String> data, String key) {
            String value = data.get(key);
            if (value == null) {
                throw new IllegalArgumentException("Falta el parametro '" + key + "'");
            }
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("El parametro '" + key + "' debe ser un entero");
            }
        }

        private Response error(String message) {
            Response r = new Response();
            r.status = "ERROR";
            r.data = new HashMap<>();
            r.data.put("message", message);
            return r;
        }
    }

}