package co.icesi.buscaminas.model;

/**
 * Se lanza cuando el jugador destapa una mina. Permite al controlador distinguir
 * "fin de partida" (resultado valido del juego) de un error de parametros.
 */
public class GameOverException extends RuntimeException {

    public GameOverException(String message) {
        super(message);
    }
}