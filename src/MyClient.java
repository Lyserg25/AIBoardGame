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
            //TODO: timer thread needed in case calculation takes too much time
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
        //TODO: tree might not be needed afterall
        //TreeNode<BoardConfiguration> root = new TreeNode<>(new BoardConfiguration(field, fieldBounds, points, myPlayerNr, null));
        //BoardConfiguration currentConfig = new BoardConfiguration(field, fieldBounds, points, myPlayerNr, null);
        List<Move> possibleMoves = getPossibleMoves(field, myPlayerNr);
        Move bestMove = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Move possibleMove : possibleMoves) {
            BoardConfiguration possibleNewConfig = new BoardConfiguration(field, fieldBounds, points, myPlayerNr, possibleMove);
            double alpha = alphaBetaSearch(possibleNewConfig, 3, bestScore, Double.POSITIVE_INFINITY);
            if (alpha > bestScore || bestMove == null) {
                bestMove = possibleMove;
                bestScore = alpha;
            }
        }
        return bestMove;

//        Random rnd = new Random();
//        List<Move> possibleMoves = getPossibleMoves(field, myPlayerNr);
//        int randomNr = rnd.nextInt(possibleMoves.size());
//        return possibleMoves.get(randomNr);
    }

    private int getCurrentPlayer(BoardConfiguration currentConfig) {
        //TODO: check if there are still 3 players
        if (currentConfig.getMovePlayerNr() == 2) {
            return 0;
        } else {
            return currentConfig.getMovePlayerNr() + 1;
        }
    }

    private double alphaBetaSearch(BoardConfiguration currentConfig, int depth, double alpha, double beta) {
        if (depth <= 0 || currentConfig.isFinishedGame()) {
            return currentConfig.getEvaluationScore();
        }
        int currentPlayer = getCurrentPlayer(currentConfig);
        List<Move> possibleMoves = getPossibleMoves(currentConfig.getField(), currentPlayer);

        if (currentPlayer == myPlayerNr) {
            double currentAlpha = Double.NEGATIVE_INFINITY;
            for (Move possibleMove : possibleMoves) {
                BoardConfiguration possibleNewConfig = new BoardConfiguration(currentConfig.getField(), fieldBounds, points, myPlayerNr, possibleMove);
                currentAlpha = Math.max(currentAlpha, alphaBetaSearch(possibleNewConfig, depth - 1, alpha, beta));
                alpha = Math.max(alpha, currentAlpha);
                if (alpha >= beta) {
                    return alpha;
                }
            }
            return currentAlpha;
        }
        double currentBeta = Double.POSITIVE_INFINITY;
        for (Move possibleMove : possibleMoves) {
            BoardConfiguration possibleNewConfig = new BoardConfiguration(currentConfig.getField(), fieldBounds, points, myPlayerNr, possibleMove);
            currentBeta = Math.min(currentBeta, alphaBetaSearch(possibleNewConfig, depth - 1, alpha, beta));
            beta = Math.min(beta, currentBeta);
            if (beta <= alpha) {
                return beta;
            }
        }
        return currentBeta;
    }

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
}
