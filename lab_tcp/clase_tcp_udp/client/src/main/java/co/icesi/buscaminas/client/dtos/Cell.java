package co.icesi.buscaminas.client.dtos;

public class Cell {
    private boolean isLandMine;
    private int value;
    private boolean hide;
    private boolean showAll;
    private boolean isMarked;

    public Cell() {}

    public Cell(boolean revealed, boolean mine, boolean flagged, int adjacentMines) {
        this.hide = !revealed;
        this.isLandMine = mine;
        this.isMarked = flagged;
        this.value = adjacentMines;
    }

    public boolean isRevealed() { return !hide; }
    public void setRevealed(boolean revealed) { this.hide = !revealed; }

    public boolean isMine() { return isLandMine; }
    public void setMine(boolean mine) { this.isLandMine = mine; }

    public boolean isFlagged() { return isMarked; }
    public void setFlagged(boolean flagged) { this.isMarked = flagged; }

    public int getAdjacentMines() { return value; }
    public void setAdjacentMines(int adjacentMines) { this.value = adjacentMines; }

    public boolean isLandMine() { return isLandMine; }
    public void setLandMine(boolean landMine) { this.isLandMine = landMine; }

    public boolean isHide() { return hide; }
    public void setHide(boolean hide) { this.hide = hide; }

    public boolean isShowAll() { return showAll; }
    public void setShowAll(boolean showAll) { this.showAll = showAll; }

    public boolean isMarked() { return isMarked; }
    public void setMarked(boolean marked) { this.isMarked = marked; }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
}