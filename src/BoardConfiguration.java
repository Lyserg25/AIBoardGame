import lenz.htw.bogapr.Move;

import java.util.*;

/**
 * Created by Wayne on 12.05.2017.
 */
public class BoardConfiguration {
    private final Stack[][] field;
    private Map<Integer, Integer> fieldBounds;
    private final Integer[] points;
    private final int myPlayerNr;
    private final Move move;
    private final int movePlayerNr;
    private final double evaluationScore;
    private final boolean isFinishedGame;

    /**
     * Representation of a board configuration
     * @param field the game field with the chips of each player
     * @param fieldBounds the bounds of the field
     * @param points points of each player
     * @param myPlayerNr our player number
     * @param move the last move that was made
     * @param movePlayerNr the player number of the player that made the last move
     */
    public BoardConfiguration(Stack[][] field, Map<Integer, Integer> fieldBounds, Integer[] points, int myPlayerNr, Move move, int movePlayerNr) {
        this.field = field;
        this.fieldBounds = fieldBounds;
        this.myPlayerNr = myPlayerNr;
        this.move = move;
        this.movePlayerNr = movePlayerNr;
        this.isFinishedGame = checkGameFinished();
        this.points = calcPoints(points);
        this.evaluationScore = evaluateConfiguration();
    }

    private boolean checkGameFinished() {
        for (int y = 1; y < 7; y++) {
            for (int x = (y == 6 ? 1 : 0); x < fieldBounds.get(y); x++) {
                int playerNr = (int) field[x][y].peek();
                if ((playerNr == 0 && y == 6)
                        || (playerNr == 1 && x == fieldBounds.get(y))
                        || (playerNr == 2 && x == (y == 6 ? 1 : 0))) {
                    return true;
                }
            }
        }
        return false;
    }

    private Integer[] calcPoints(Integer[] points) {
        if (isFinishedGame()) {
            points[movePlayerNr] += 5;
        }
        return points;
    }

    private double evaluateConfiguration() {
        List<Integer> points = Arrays.asList(this.points);

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

    public Move getMove() { return move; }

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