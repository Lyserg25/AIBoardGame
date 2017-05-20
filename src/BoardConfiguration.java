import lenz.htw.bogapr.Move;

import java.util.*;

/**
 * Created by Wayne on 12.05.2017.
 */
public class BoardConfiguration {
    private Stack[][] field;
    private Map<Integer, Integer> fieldBounds;
    private Integer[] points;
    private final int myPlayerNr;
    private final Move move;
    private int movePlayerNr;
    private final double evaluationScore;
    private boolean isFinishedGame;

    /**
     * Representation of a board configuration
     *
     * @param field       the game field with the chips of each player
     * @param fieldBounds the bounds of the field
     * @param points      points of each player
     * @param myPlayerNr  our player number
     * @param move        the last move that was made
     */
    public BoardConfiguration(Stack[][] field, Map<Integer, Integer> fieldBounds, Integer[] points, int myPlayerNr, Move move) {
        this.fieldBounds = fieldBounds;
        this.field = copyField(field);
        this.myPlayerNr = myPlayerNr;
        this.move = move;
        if (move != null) {
            moveChip(Arrays.copyOf(points, points.length));
        } else {
            this.points = points;
        }
        this.evaluationScore = evaluateConfiguration();
    }

    private Stack[][] copyField(Stack[][] field) {
        Stack[][] copy = new Stack[12][7];

        for (int y = 1; y < 7; y++) {
            for (int x = (y == 6 ? 1 : 0); x < fieldBounds.get(y); x++) {
                copy[x][y] = (Stack) field[x][y].clone();
            }
        }
        return copy;
    }

    private void moveChip(Integer[] points) {
        this.movePlayerNr = (int) field[move.fromX][move.fromY].pop();
        Stack newPosition = field[move.toX][move.toY];
        boolean isFinishedGame = false;

        if (!newPosition.isEmpty() && (movePlayerNr != (int) newPosition.peek())) {
            points[movePlayerNr]++;
        }
        if ((movePlayerNr == 0 && move.toY == 6)
                || (movePlayerNr == 1 && move.toX == fieldBounds.get(move.toY))
                || (movePlayerNr == 2 && move.toX == (move.toY == 6 ? 1 : 0))) {
            points[movePlayerNr] += 5;
            isFinishedGame = true;
        }
        newPosition.push(movePlayerNr);
        this.isFinishedGame = isFinishedGame;
        this.points = points;
    }

    private double evaluateConfiguration() {
        List<Integer> points = new LinkedList<>(Arrays.asList(this.points));

        if (isFinishedGame() && points.get(myPlayerNr) == Collections.max(points)) {
            return Double.POSITIVE_INFINITY;
        } else if (isFinishedGame() && points.get(myPlayerNr) != Collections.max(points)) {
            return Double.NEGATIVE_INFINITY;
        }

        int myPoints = points.remove(myPlayerNr);
        int maxEnemyPoints = Collections.max(points);
        return myPoints - maxEnemyPoints;
    }

    public Stack[][] getField() {
        return field;
    }

    public Integer[] getPoints() {
        return points;
    }

    public Move getMove() {
        return move;
    }

    public int getMovePlayerNr() {
        return movePlayerNr;
    }

    public double getEvaluationScore() {
        return evaluationScore;
    }

    public boolean isFinishedGame() {
        return isFinishedGame;
    }
}