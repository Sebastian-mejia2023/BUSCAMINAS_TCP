package co.icesi.buscaminas.client;

import co.icesi.buscaminas.client.dtos.Request;
import co.icesi.buscaminas.client.ui.BoardRenderer;
import co.icesi.buscaminas.client.dtos.Cell;
import co.icesi.buscaminas.model.BoardGame;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolAndBoardTest {

    @Test
    void requestShouldUseDataMapContract() {
        Request request = new Request("INIT_GAME", 3, 4, 2, 0, 0);

        String json = new Gson().toJson(request);

        assertTrue(json.contains("\"action\":\"INIT_GAME\""));
        assertTrue(json.contains("\"data\""));
        assertTrue(json.contains("\"n\":\"3\""));
        assertTrue(json.contains("\"m\":\"4\""));
        assertTrue(json.contains("\"minas\":\"2\""));
    }

    @Test
    void boardShouldUpdateAfterMarkAndReveal() {
        BoardGame game = new BoardGame();
        game.initGame(3, 3, 1);

        int mineRow = -1;
        int mineCol = -1;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (game.getBoard()[i][j].isLandMine()) {
                    mineRow = i;
                    mineCol = j;
                }
            }
        }

        int safeRow = 0;
        int safeCol = 0;
        if (safeRow == mineRow && safeCol == mineCol) {
            safeRow = 2;
            safeCol = 2;
        }

        game.markCell(safeRow, safeCol);
        assertTrue(game.getBoard()[safeRow][safeCol].isMarked());

        game.selectCell(safeRow, safeCol);
        assertFalse(game.getBoard()[safeRow][safeCol].isHide());
    }

    @Test
    void boardRendererShouldKeepColumnsAligned() {
        Cell[][] board = new Cell[][] {
            { new Cell(true, false, false, 0), new Cell(false, false, false, 0), new Cell(true, false, false, 1) },
            { new Cell(true, false, false, 1), new Cell(true, false, false, 0), new Cell(true, false, false, 0) }
        };

        String output = captureStdOut(() -> BoardRenderer.render(board));
        String plainOutput = stripAnsi(output);

        assertTrue(plainOutput.contains("0 |  . |  1"));
        assertTrue(plainOutput.contains("1 |  0 |  0"));
    }

    private String captureStdOut(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        try {
            action.run();
            return out.toString(StandardCharsets.UTF_8);
        } finally {
            System.setOut(original);
        }
    }

    private String stripAnsi(String text) {
        return text.replaceAll("\\u001B\\[[;0-9]*m", "");
    }
}
