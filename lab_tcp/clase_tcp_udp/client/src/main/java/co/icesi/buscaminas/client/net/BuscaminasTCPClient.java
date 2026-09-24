package co.icesi.buscaminas.client.net;

import co.icesi.buscaminas.client.dtos.Request;
import co.icesi.buscaminas.client.dtos.Response;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class BuscaminasTCPClient {

    private String host;
    private int port;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson;

    public BuscaminasTCPClient() {
        this("localhost", 12345);
    }

    public BuscaminasTCPClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.gson = new Gson();
    }

    private boolean connect() {
        try {
            if (socket == null || socket.isClosed() || !socket.isConnected()) {
                this.socket = new Socket(host, port);
                this.out = new PrintWriter(socket.getOutputStream(), true);
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            }
            return true;
        } catch (Exception e) {
            System.out.println("Error al conectar con el servidor TCP (" + host + ":" + port + "): " + e.getMessage());
            return false;
        }
    }

    private Request buildRequest(String action, int rows, int cols, int mines, int row, int col) {
        Map<String, String> data = new HashMap<>();
        switch (action) {
            case "INIT_GAME":
                data.put("n", String.valueOf(rows));
                data.put("m", String.valueOf(cols));
                data.put("minas", String.valueOf(mines));
                break;
            case "SELECT_CELL":
            case "MARK_CELL":
                data.put("i", String.valueOf(row));
                data.put("j", String.valueOf(col));
                break;
            default:
                break;
        }

        Request request = new Request();
        request.setAction(action);
        request.setData(data);
        return request;
    }

    public Response sendRequest(Request request) {
        try {
            // Usar una conexion nueva por cada request para adaptarnos
            // al comportamiento del servidor (acepta una peticion y cierra).
            disconnect();
            if (!connect()) return null;

            String jsonRequest = gson.toJson(request);
            out.println(jsonRequest);

            String jsonResponse = in.readLine();
            if (jsonResponse != null) {
                return gson.fromJson(jsonResponse, Response.class);
            }
        } catch (Exception e) {
            System.out.println("Error de comunicacion TCP: " + e.getMessage());
            disconnect();
        }
        return null;
    }

    public Response initGame(int rows, int cols, int mines) {
        return sendRequest(buildRequest("INIT_GAME", rows, cols, mines, 0, 0));
    }

    public Response selectCell(int row, int col) {
        return sendRequest(buildRequest("SELECT_CELL", 0, 0, 0, row, col));
    }

    public Response markCell(int row, int col) {
        return sendRequest(buildRequest("MARK_CELL", 0, 0, 0, row, col));
    }

    public Response getBoard() {
        return sendRequest(buildRequest("GET_BOARD", 0, 0, 0, 0, 0));
    }

    public Response sowAll() {
        return sendRequest(buildRequest("SOW_ALL", 0, 0, 0, 0, 0));
    }

    public void disconnect() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception e) {
            // Ignorar al cerrar
        } finally {
            socket = null;
            out = null;
            in = null;
        }
    }
}