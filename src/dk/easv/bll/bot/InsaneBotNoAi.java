package dk.easv.bll.bot;

import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;
import dk.easv.bll.move.Move;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class InsaneBotNoAi implements IBot {

    private Random rand = new Random();
    private final int moveTimeMs = 1000;
    private final String BOT_NAME = getClass().getSimpleName();

    private static final int BOARD_SIZE = 9;
    private static final int SUBGRID_SIZE = 3;

    @Override
    public IMove doMove(IGameState state) {
        return calculateMove(state, moveTimeMs);
    }

    /**
     * Calculates the next move based on the current game state.
     *
     * @param state      The current game state.
     * @param maxTimeMs  The maximum time allowed for the AI to make a move.
     * @return The calculated move.
     */
    private IMove calculateMove(IGameState state, int maxTimeMs) {
        // Create a simulator to simulate the current state of the game
        GameSimulator simulator = createSimulator(state);

        // Get a list of legal moves available in the current state
        List<IMove> legalMoves = simulator.getCurrentState().getField().getAvailableMoves();

        // Check for winning move and return if found
        IMove winningMove = evaluateMoves(legalMoves, simulator, false);
        if (winningMove != null) return winningMove;

        // Check for blocking move and return if found
        IMove blockingMove = evaluateMoves(legalMoves, simulator, true);
        if (blockingMove != null) return blockingMove;

        // Optimize the initial move if it's the first move of the game
        if (state.getMoveNumber() == 0) {
            return optimizeInitialMove(legalMoves);
        }
        // After the initial move, prioritize corners
        for (IMove move : legalMoves) {
            if ((move.getX() % 2 == 0) && (move.getY() % 2 == 0)) {
                // Check if the corner vertically across it is available
                IMove oppositeCorner = findVerticallyOppositeCorner(move, legalMoves);
                if (oppositeCorner != null) {
                    return move;
                }
            }
        }

        // Find a defensive move to block opponent's potential winning move
        IMove defensiveMove = findDefensiveMove(legalMoves, simulator);
        return (defensiveMove != null) ? defensiveMove : legalMoves.get(rand.nextInt(legalMoves.size()));


    }

    /**
     * Finds a defensive move to block the opponent from winning in the next turn.
     *
     * @param legalMoves List of legal moves in the current state.
     * @param simulator  The game simulator.
     * @return The defensive move if found, otherwise null.
     */
    private IMove findDefensiveMove(List<IMove> legalMoves, GameSimulator simulator) {
        for (IMove move : legalMoves) {
            // Create a simulator for evaluating opponent's moves
            GameSimulator evalSimulator = createSimulator(simulator.getCurrentState());
            evalSimulator.setCurrentPlayer((simulator.getCurrentPlayer() + 1) % 2);
            evalSimulator.updateGame(move);

            // Get opponent's legal moves
            List<IMove> opponentMoves = evalSimulator.getCurrentState().getField().getAvailableMoves();

            // Check if opponent has a winning move
            if (evaluateMoves(opponentMoves, evalSimulator, false) != null) {
                return move;
            }
        }
        return null;
    }

    /**
     * Chooses the initial move based on the available legal moves.
     * If the middle is available, it selects the middle move.
     * Otherwise, it prioritizes choosing a corner move.
     * If no corner move is available, it selects a random move.
     *
     * @param legalMoves The list of legal moves available in the current state.
     * @return The chosen initial move.
     */
    private IMove optimizeInitialMove(List<IMove> legalMoves) {
        // Check if the middle move is available
        IMove middleMove = null;
        for (IMove move : legalMoves) {
            if (move.getX() == BOARD_SIZE / 2 && move.getY() == BOARD_SIZE / 2) {
                middleMove = move;
                break;
            }
        }

        // If the middle is available, choose it
        if (middleMove != null) {
            return middleMove;
        }

        // Check if the corner move is available
        IMove cornerMove = null;
        for (IMove move : legalMoves) {
            // Choose a corner move if available
            if ((move.getX() % 2 == 0) && (move.getY() % 2 == 0)) {
                cornerMove = move;
                break;
            }
        }
        // If a corner move is available, choose it
        if (cornerMove != null) {
            return cornerMove;
        }
        // If no corner found, choose a random move
        return legalMoves.get(rand.nextInt(legalMoves.size()));
    }

    /**
     * Finds the vertically opposite corner move of a given corner move, if available.
     *
     * @param cornerMove The corner move for which the vertically opposite corner is to be found.
     * @param legalMoves The list of legal moves available in the current state.
     * @return The vertically opposite corner move if found; otherwise, returns null.
     */
    private IMove findVerticallyOppositeCorner(IMove cornerMove, List<IMove> legalMoves) {
        // Calculate the coordinates of the vertically opposite corner
        int oppositeX = (BOARD_SIZE - 1) - cornerMove.getX();
        int oppositeY = cornerMove.getY();

        // Check if the vertically opposite corner is available
        for (IMove move : legalMoves) {
            if (move.getX() == oppositeX && move.getY() == oppositeY) {
                return move; // Return the vertically opposite corner move if found
            }
        }

        return null; // Vertically opposite corner not found
    }

    /**
     * Evaluates moves based on the specified criteria (winning or blocking).
     *
     * @param moves     List of possible moves.
     * @param simulator The game simulator.
     * @param blocking  Flag indicating if the evaluation is for blocking.
     * @return The evaluated move if found, otherwise null.
     */
    private IMove evaluateMoves(List<IMove> moves, GameSimulator simulator, boolean blocking) {
        for (IMove move : moves) {
            // Create a simulator for evaluating moves
            GameSimulator evalSimulator = createSimulator(simulator.getCurrentState());
            // Set the current player in the evaluation simulator based on the blocking flag
            evalSimulator.setCurrentPlayer(blocking ? (simulator.getCurrentPlayer() + 1) % 2 : simulator.getCurrentPlayer());
            // Update the game state in the evaluation simulator with the current move
            evalSimulator.updateGame(move);

            // Check if the move results in a win in the evaluation simulator
            if (evalSimulator.getGameOver() == GameOverState.Win) {
                // If a winning move is found, return it
                return move;
            }
        }
        return null;
    }

    private GameSimulator createSimulator(IGameState state) {
        GameSimulator simulator = new GameSimulator(new GameState());
        simulator.setGameOver(GameOverState.Active);
        simulator.setCurrentPlayer(state.getMoveNumber() % 2);

        simulator.getCurrentState().setRoundNumber(state.getRoundNumber());
        simulator.getCurrentState().setMoveNumber(state.getMoveNumber());

        simulator.getCurrentState().getField().setBoard(state.getField().getBoard());
        simulator.getCurrentState().getField().setMacroboard(state.getField().getMacroboard());
        return simulator;
    }

    @Override
    public String getBotName() {
        return BOT_NAME;
    }

    public enum GameOverState {
        Active,
        Win,
        Tie
    }

    class GameSimulator {
        private final IGameState currentState;
        private int currentPlayer = 0;
        private volatile GameOverState gameOver = GameOverState.Active;

        public GameSimulator(IGameState currentState) {
            this.currentState = currentState;
        }

        public void setGameOver(GameOverState state) {
            gameOver = state;
        }

        public GameOverState getGameOver() {
            return gameOver;
        }

        public int getCurrentPlayer() {
            return currentPlayer;
        }

        public void setCurrentPlayer(int player) {
            currentPlayer = player;
        }

        public void switchPlayer() {
            currentPlayer = (currentPlayer + 1) % 2;
        }

        public IGameState getCurrentState() {
            return currentState;
        }

        public Boolean updateGame(IMove move) {
            if (!verifyMoveLegality(move))
                return false;

            updateBoard(move);
            switchPlayer();

            return true;
        }

        private Boolean verifyMoveLegality(IMove move) {
            IField field = currentState.getField();
            boolean isValid = field.isInActiveMicroboard(move.getX(), move.getY());

            if (isValid && (move.getX() < 0 || BOARD_SIZE <= move.getX())) isValid = false;
            if (isValid && (move.getY() < 0 || BOARD_SIZE <= move.getY())) isValid = false;

            if (isValid && !field.getBoard()[move.getX()][move.getY()].equals(IField.EMPTY_FIELD))
                isValid = false;

            return isValid;
        }

        private void updateBoard(IMove move) {
            String[][] board = currentState.getField().getBoard();
            board[move.getX()][move.getY()] = currentPlayer + "";
            currentState.setMoveNumber(currentState.getMoveNumber() + 1);

            if (currentState.getMoveNumber() % 2 == 0) {
                currentState.setRoundNumber(currentState.getRoundNumber() + 1);
            }
            checkAndUpdateIfWin(move);
            updateMacroboard(move);
        }

        private void checkAndUpdateIfWin(IMove move) {
            String[][] macroBoard = currentState.getField().getMacroboard();
            int macroX = move.getX() / SUBGRID_SIZE;
            int macroY = move.getY() / SUBGRID_SIZE;

            if (macroBoard[macroX][macroY].equals(IField.EMPTY_FIELD) ||
                    macroBoard[macroX][macroY].equals(IField.AVAILABLE_FIELD)) {

                String[][] board = getCurrentState().getField().getBoard();

                if (isWin(board, move, "" + currentPlayer))
                    macroBoard[macroX][macroY] = currentPlayer + "";
                else if (isTie(board, move))
                    macroBoard[macroX][macroY] = "TIE";

                if (isWin(macroBoard, new Move(macroX, macroY), "" + currentPlayer))
                    gameOver = GameOverState.Win;
                else if (isTie(macroBoard, new Move(macroX, macroY)))
                    gameOver = GameOverState.Tie;
            }
        }

        private boolean isTie(String[][] board, IMove move) {
            int localX = move.getX() % SUBGRID_SIZE;
            int localY = move.getY() % SUBGRID_SIZE;
            int startX = move.getX() - (localX);
            int startY = move.getY() - (localY);

            for (int i = startX; i < startX + SUBGRID_SIZE; i++) {
                for (int k = startY; k < startY + SUBGRID_SIZE; k++) {
                    if (board[i][k].equals(IField.AVAILABLE_FIELD) ||
                            board[i][k].equals(IField.EMPTY_FIELD))
                        return false;
                }
            }
            return true;
        }

        public boolean isWin(String[][] board, IMove move, String currentPlayer) {
            int localX = move.getX() % SUBGRID_SIZE;
            int localY = move.getY() % SUBGRID_SIZE;
            int startX = move.getX() - (localX);
            int startY = move.getY() - (localY);

            for (int i = startY; i < startY + SUBGRID_SIZE; i++) {
                if (!board[move.getX()][i].equals(currentPlayer))
                    break;
                if (i == startY + SUBGRID_SIZE - 1) return true;
            }

            for (int i = startX; i < startX + SUBGRID_SIZE; i++) {
                if (!board[i][move.getY()].equals(currentPlayer))
                    break;
                if (i == startX + SUBGRID_SIZE - 1) return true;
            }

            if (localX == localY) {
                int y = startY;
                for (int i = startX; i < startX + SUBGRID_SIZE; i++) {
                    if (!board[i][y++].equals(currentPlayer))
                        break;
                    if (i == startX + SUBGRID_SIZE - 1) return true;
                }
            }

            if (localX + localY == SUBGRID_SIZE - 1) {
                int less = 0;
                for (int i = startX; i < startX + SUBGRID_SIZE; i++) {
                    if (!board[i][(startY + 2) - less++].equals(currentPlayer))
                        break;
                    if (i == startX + SUBGRID_SIZE - 1) return true;
                }
            }
            return false;
        }

        private void updateMacroboard(IMove move) {
            String[][] macroBoard = currentState.getField().getMacroboard();
            for (String[] row : macroBoard) {
                Arrays.fill(row, IField.AVAILABLE_FIELD);
            }

            int xTrans = move.getX() % SUBGRID_SIZE;
            int yTrans = move.getY() % SUBGRID_SIZE;

            macroBoard[xTrans][yTrans] = IField.EMPTY_FIELD;
        }
    }
}