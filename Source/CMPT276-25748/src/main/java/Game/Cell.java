package Game;

public class Cell {

    int cellRow; // position
    char cellChar;

    // default constructor
    public Cell() {}

    // constructor which store the character letter
    public Cell(char cellContent) {
        cellChar = cellContent;
    }

    // returns the content of the cell
    public char getCellChar() {
        return cellChar;
    }

}