import lenz.htw.bogapr.Move;
import lenz.htw.bogapr.net.NetworkClient;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Created by Wayne on 12.04.2017.
 */
public class MyClient implements Callable<Void> {

    private String hostName;
    private String teamName;
    private BufferedImage logo;
    private Integer[] points = {0, 0, 0,};
    private Map<Integer, Integer> fieldBounds;
    protected int myPlayerNr;

    public MyClient(String hostname, String teamName, BufferedImage logo) {
        this.hostName = hostname;
        this.teamName = teamName;
        this.logo = logo;
    }

    @Override
    public Void call() {
        Move receivedMove;
        Move calculatedMove;
        initFieldBounds();
        Stack[][] currentField = initField();
        NetworkClient networkClient = new NetworkClient(hostName, teamName, logo);
        myPlayerNr = networkClient.getMyPlayerNumber();

        for (; ; ) {
            while ((receivedMove = networkClient.receiveMove()) != null) {
                moveChip(currentField, receivedMove);
            }
            calculatedMove = calculateMove(currentField);
            networkClient.sendMove(calculatedMove);
        }
    }

    protected void initFieldBounds() {
        fieldBounds = new HashMap<>();
        for (int i = 1; i < 6; i++) {
            fieldBounds.put(i, 2 * i + 1);
        }
        fieldBounds.put(6, 12);
    }

    protected Stack[][] initField() {
        Stack[][] field = new Stack[12][7];

        for (int y = 1; y < 7; y++) {
            for (int x = (y == 6 ? 1 : 0); x < fieldBounds.get(y); x++) {
                field[x][y] = new Stack();
            }
        }
        //player 0
        add3Chips(field[0][1], 0);
        add3Chips(field[1][1], 0);
        add3Chips(field[2][1], 0);
        //player 1
        add3Chips(field[0][5], 1);
        add3Chips(field[1][6], 1);
        add3Chips(field[2][6], 1);
        //player 2
        add3Chips(field[10][5], 2);
        add3Chips(field[10][6], 2);
        add3Chips(field[11][6], 2);

        return field;
    }

    private void add3Chips(Stack stack, int playerNr) {
        for (int i = 0; i < 3; i++) {
            stack.push(playerNr);
        }
    }

    private void moveChip(Stack[][] field, Move move) {
        int playerNr = (int) field[move.fromX][move.fromY].pop();
        Stack newPosition = field[move.toX][move.toY];

        if (!newPosition.isEmpty() && (playerNr != (int) newPosition.peek())) {
            points[playerNr]++;
        }

        newPosition.push(playerNr);
    }

    protected Move calculateMove(Stack[][] field) {
        List<Move> possibleMoves = getPossibleMoves(field, myPlayerNr);
        TreeNode<Configuration> root = new TreeNode<>(new Configuration(field, points, myPlayerNr, myPlayerNr));


        Random rnd = new Random();
        int randomNr = rnd.nextInt(possibleMoves.size());

        return possibleMoves.get(randomNr);
    }

//    private int miniMax(TreeNode<Configuration> currentNode, int depth, int alpha, int beta) {
//        if (depth <= 0 || currentNode.isLeafNode()) {
//            return getHeuristic(currentNode.getState());
//        }
//        if (currentNode.getState().getCurrentPlayer().equals(selfColor)) {
//            int currentAlpha = -INFINITY;
//            for (GameTreeNode child : currentNode.getChildren()) {
//                currentAlpha = Math.max(currentAlpha, miniMax(child, depth - 1, alpha, beta));
//                alpha = Math.max(alpha, currentAlpha);
//                if (alpha >= beta) {
//                    return alpha;
//                }
//            }
//            return currentAlpha;
//        }
//        int currentBeta = INFINITY;
//        for (GameTreeNode child : currentNode.getChildren()) {
//            currentBeta = Math.min(currentBeta, miniMax(child, depth - 1, alpha, beta));
//            beta = Math.min(beta, currentBeta);
//            if (beta <= alpha) {
//                return beta;
//            }
//        }
//        return currentBeta;
//    }

    protected List<Move> getPossibleMoves(Stack[][] field, int playerNr) {
        List<Move> possibleMoves = new ArrayList<>();
        List<Position> movableChipPositions = getMovableChips(field, playerNr);

        for (Position movableChipPosition : movableChipPositions) {
            possibleMoves.addAll(getPossibleMovesFromPosition(movableChipPosition, field));
        }

        return possibleMoves;
    }

    private Set<Move> getPossibleMovesFromPosition(Position startingPos, Stack[][] field) {
        Set<Move> possibleMoves = new HashSet<>();
        Map<Position, Position> nextPositions = new HashMap<>(); //position: previous position
        nextPositions.put(startingPos, null);
        int steps = field[startingPos.x][startingPos.y].size();

        while (steps > 0) {
            nextPositions = getNextPositions(nextPositions, steps--, field);
        }

        for (Position endPos : nextPositions.keySet()) {
            possibleMoves.add(new Move(startingPos.x, startingPos.y, endPos.x, endPos.y));
        }

        return possibleMoves;
    }

    private Map<Position, Position> getNextPositions(Map<Position, Position> positions, int remainingSteps, Stack[][] field) {
        Map<Position, Position> nextPositions = new HashMap<>();
        nextPositions.putAll(getNextPositionsX(positions, remainingSteps, field));
        nextPositions.putAll(getNextPositionsY(positions, remainingSteps, field));

        return nextPositions;
    }

    private Map<Position, Position> getNextPositionsX(Map<Position, Position> startingPositions, int remainingSteps, Stack[][] field) {
        Map<Position, Position> nextPositionsX = new HashMap<>();
        int xBound;
        int nextX;
        for (Position startingPos : startingPositions.keySet()) {
            xBound = fieldBounds.get(startingPos.y);
            nextX = startingPos.x - 1;
            Position prevPos = startingPositions.get(startingPos);
            if ((nextX >= (startingPos.y == 6 ? 1 : 0)) && ((prevPos == null) || !((prevPos.x == nextX) && (prevPos.y == startingPos.y)))) {
                if (!((remainingSteps == 1) && (field[nextX][startingPos.y].size() == 3))) {
                    nextPositionsX.put(new Position(nextX, startingPos.y), startingPos);
                }
            }
            nextX = startingPos.x + 1;
            if ((nextX < xBound) && ((prevPos == null) || !((prevPos.x == nextX) && (prevPos.y == startingPos.y)))) {
                if (!((remainingSteps == 1) && (field[nextX][startingPos.y].size() == 3))) {
                    nextPositionsX.put(new Position(nextX, startingPos.y), startingPos);
                }
            }
        }

        return nextPositionsX;
    }

    private Map<Position, Position> getNextPositionsY(Map<Position, Position> startingPositions, int remainingSteps, Stack[][] field) {
        Map<Position, Position> nextPositionsY = new HashMap<>();
        int nextY;
        int nextX;
        for (Position startingPos : startingPositions.keySet()) {
            nextY = (startingPos.x % 2 == 0) ? startingPos.y + 1 : startingPos.y - 1;
            nextX = (startingPos.x % 2 == 0) ? startingPos.x + 1 : startingPos.x - 1;
            Position prevPos = startingPositions.get(startingPos);
            if ((nextY >= 1) && (nextY <= 6) && ((prevPos == null) || !((prevPos.x == nextX) && (prevPos.y == nextY)))) {
                if (!((remainingSteps == 1) && (field[nextX][nextY].size() == 3))) {
                    nextPositionsY.put(new Position(nextX, nextY), startingPos);
                }
            }
        }

        return nextPositionsY;
    }

    private List<Position> getMovableChips(Stack[][] field, int playerNr) {
        List<Position> movableChips = new ArrayList<>();
        for (int y = 1; y < 7; y++) {
            for (int x = (y == 6 ? 1 : 0); x < fieldBounds.get(y); x++) {
                if (!field[x][y].isEmpty() && (playerNr == (int) field[x][y].peek())) {
                    movableChips.add(new Position(x, y));
                }
            }
        }

        return movableChips;
    }

    private class Position {
        public final int x;
        public final int y;

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private class Configuration {
        public final Stack[][] field;
        public final Integer[] points;
        public final int myPlayerNr;
        public final int turnPlayerNr;
        public final int evaluationScore;

        public Configuration(Stack[][] field, Integer[] points, int myPlayerNr, int turnPlayerNr) {
            this.field = field;
            this.points = points;
            this.myPlayerNr = myPlayerNr;
            this.turnPlayerNr = turnPlayerNr;
            this.evaluationScore = evaluate();
        }

        private int evaluate() {

            List<Integer> points = Arrays.asList(this.points);
            int myPoints = points.remove(myPlayerNr);
            int maxEnemyPoints = Collections.max(points);
            return myPoints - maxEnemyPoints;
        }

        public boolean isGameFinished() {
            for (int y = 1; y < 7; y++) {
                for (int x = (y == 6 ? 1 : 0); x < fieldBounds.get(y); x++) {
                    int playerNr = (int) field[x][y].peek();
                    if ((playerNr == 0 && y == 6)
                        ||(playerNr == 1 && x == fieldBounds.get(y))
                            || (playerNr == 2 && x == (y == 6 ? 1 : 0))) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
