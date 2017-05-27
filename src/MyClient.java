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
    private List<Integer> players;
    private Integer lastPlayer;
    private Move bestMove;
    private boolean stopCalculation;

    public MyClient(String hostname, String teamName, BufferedImage logo) {
        this.hostName = hostname;
        this.teamName = teamName;
        this.logo = logo;
    }

    @Override
    public Void call() {
        Move receivedMove;
        initFieldBounds();
        Stack[][] currentField = initField();
        NetworkClient networkClient = new NetworkClient(hostName, teamName, logo);
        myPlayerNr = networkClient.getMyPlayerNumber();
        int calculationTime = networkClient.getTimeLimitInSeconds() * 1000 - networkClient.getExpectedNetworkLatencyInMilliseconds() - 300;
        Timer timer = new Timer();
        players = new ArrayList<>();
        for (int i = 0; i < 3; i++){
            players.add(i);
        }

        for (; ; ) {
            while ((receivedMove = networkClient.receiveMove()) != null) {
                moveChip(currentField, receivedMove);
            }
            stopCalculation = false;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    networkClient.sendMove(getCurrentBestMove());
                    stopCalculation = true;
                }
            }, calculationTime);
            calculateMove(currentField);
        }
    }

    protected Move getCurrentBestMove() {
        return bestMove;
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
        int expectedCurrentPlayer = lastPlayer == null ? 0 : getCurrentPlayer(lastPlayer);

        if (playerNr != expectedCurrentPlayer) {
            for (int i = 0; i < players.size(); i++) {
                if (players.get(i) == expectedCurrentPlayer) {
                    players.remove(i);
                }
            }
        }

        if (!newPosition.isEmpty() && (playerNr != (int) newPosition.peek())) {
            points[playerNr]++;
        }

        newPosition.push(playerNr);
        lastPlayer = playerNr;
    }

    protected Move calculateMove(Stack[][] field) {
        BoardConfiguration currentConfig = new BoardConfiguration(field, fieldBounds, points, myPlayerNr, null);
        List<BoardConfiguration> possibleNewConfigs = getPossibleMoves(currentConfig, myPlayerNr);
        Collections.reverse(possibleNewConfigs);
        double bestScore = Double.NEGATIVE_INFINITY;
        bestMove = null;

//        for (BoardConfiguration possibleNewConfig : possibleNewConfigs) {
//            System.out.println(possibleNewConfig.getEvaluationScore());
//        }

        for (BoardConfiguration possibleNewConfig : possibleNewConfigs) {
            if (stopCalculation) break;
            if (bestMove == null) {
                bestMove = possibleNewConfig.getMove();
            }
            double alpha = alphaBetaSearch(possibleNewConfig, 3, bestScore, Double.POSITIVE_INFINITY);
            //System.out.println("(" + possibleMove.fromX + "," + possibleMove.fromY + ") -> (" + possibleMove.toX + "," + possibleMove.toY + ") Score: " + alpha);
            if (alpha > bestScore) {
                bestMove = possibleNewConfig.getMove();
                bestScore = alpha;
            }
        }
        //System.out.println("best score: " + bestScore);
        return bestMove;
    }

    private int getCurrentPlayer(int lastPlayerNr) {
        int lastPlayerIndex = 0;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i) == lastPlayerNr) {
                lastPlayerIndex = i;
            }
        }
        if (lastPlayerIndex == players.size() - 1) {
            return players.get(0);
        } else {
            return players.get(lastPlayerIndex + 1);
        }
    }

    private double alphaBetaSearch(BoardConfiguration currentConfig, int depth, double alpha, double beta) {
        if (depth <= 0 || currentConfig.isFinishedGame()) {
            return currentConfig.getEvaluationScore();
        }
        int currentPlayer = getCurrentPlayer(currentConfig.getMovePlayerNr());
        List<BoardConfiguration> possibleNewConfigs = getPossibleMoves(currentConfig, currentPlayer);

        if (currentPlayer == myPlayerNr) {
            double currentAlpha = Double.NEGATIVE_INFINITY;
            Collections.reverse(possibleNewConfigs);

            for (BoardConfiguration possibleNewConfig : possibleNewConfigs) {
                if (stopCalculation) break;
                currentAlpha = Math.max(currentAlpha, alphaBetaSearch(possibleNewConfig, depth - 1, alpha, beta));
                alpha = Math.max(alpha, currentAlpha);
                if (alpha >= beta) {
                    return alpha;
                }
            }
            return currentAlpha;
        }
        double currentBeta = Double.POSITIVE_INFINITY;
        for (BoardConfiguration possibleNewConfig : possibleNewConfigs) {
            if (stopCalculation) break;
            currentBeta = Math.min(currentBeta, alphaBetaSearch(possibleNewConfig, depth - 1, alpha, beta));
            beta = Math.min(beta, currentBeta);
            if (beta <= alpha) {
                return beta;
            }
        }
        return currentBeta;
    }

    protected List<BoardConfiguration> getPossibleMoves(BoardConfiguration currentConfig, int playerNr) {
        List<BoardConfiguration> possibleNewConfigs = new ArrayList<>();
        List<Move> possibleMoves = new ArrayList<>();
        List<Position> movableChipPositions = getMovableChips(currentConfig.getField(), playerNr);

        for (Position movableChipPosition : movableChipPositions) {
            possibleMoves.addAll(getPossibleMovesFromPosition(movableChipPosition, currentConfig.getField()));
        }
        for (Move possibleMove : possibleMoves) {
            possibleNewConfigs.add(new BoardConfiguration(currentConfig.getField(), fieldBounds, currentConfig.getPoints(), myPlayerNr, possibleMove));
        }
        possibleNewConfigs.sort(new BoardConfigComparator());
        return possibleNewConfigs;
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

    private class BoardConfigComparator implements Comparator<BoardConfiguration> {
        @Override
        public int compare(BoardConfiguration config1, BoardConfiguration config2) {
            return Double.compare(config1.getEvaluationScore(), config2.getEvaluationScore());
        }
    }
}
