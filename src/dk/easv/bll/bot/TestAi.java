package dk.easv.bll.bot;

import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class TestAi implements IBot {
    private String BOT_NAME = getClass().getSimpleName();
    final int moveTimeMs = 1000;

    @Override
    public IMove doMove(IGameState state) {
        return mcts(state, moveTimeMs);
    }

    private IMove mcts(IGameState state, int maxTimeMs) {
        long time = System.currentTimeMillis();
        Random rand = new Random();
        int count = 0;

        // Create the root node
        Node rootNode = new Node(null, null, state);

        while (System.currentTimeMillis() < time + maxTimeMs) {
            // Selection & Expansion phase
            Node selectedNode = selectNode(rootNode);
            if (selectedNode != null) {
                expandNode(selectedNode, state);
                // Simulation phase
                int score = simulateGame(selectedNode.getState());

                // Backpropagation phase
                selectedNode.updateStats(score);

                count++;
            }

        }

        // Select the best move based on the node statistics
        return selectBestMove(rootNode);
    }

    // Select node with UCB1 algorithm
    private Node selectNode(Node rootNode) {
        // Implement your selection strategy here, e.g., UCB1
        // Initialize variables for tracking the selected node and the best UCB1 value found so far
        Node selectedNode = null;
        double bestUCB1 = Double.NEGATIVE_INFINITY;

        // Iterate over each child node of the root node
        for (Node child : rootNode.getChildren()) {

            // Calculate the exploitation term (average score) for the child node
            double exploitationTerm = (double) child.getScore() / child.getVisits();

            // Calculate the exploration term using the UCB1 formula
            double explorationTerm = Math.sqrt(2 * Math.log(rootNode.getVisits()) / child.getVisits());

            // Calculate the UCB1 value by combining exploitation and exploration terms
            double ucb1Value = exploitationTerm + explorationTerm;

            // Update the selected node if the current child node has a higher UCB1 value
            if (ucb1Value > bestUCB1) {
                bestUCB1 = ucb1Value;
                selectedNode = child;
            }
        }

        // Return the selected child node with the highest UCB1 value
        return selectedNode;
    }

    // Expand the selected node by adding child nodes for unexplored moves
    private void expandNode(Node node, IGameState state) {
        // Get available moves from the game state
        if(node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }
        List<IMove> availableMoves = state.getField().getAvailableMoves();

        // Iterate over each available move
        for (IMove move : availableMoves) {
            // Create a new child node with the current node as parent and the move as action
            Node childNode = new Node(node, move, state);

            // Add the child node to the list of children of the current node
            node.addChild(childNode);
        }

    }


    private int simulateGame(IGameState state) {
        int totalScore = 0;
        IField field = state.getField();
        String[][] macroboard = field.getMacroboard();

        // Evaluate each small board and accumulate scores
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                // Check if the small board is available for play based on the macroboard
                if (macroboard[i][j].equals(IField.AVAILABLE_FIELD)) {

                    // Extract the small board at position (i, j)
                    String[][] smallBoard = extractSmallBoard(field, i, j);

                    // Evaluate the small board and add its score to the total score
                    totalScore += evaluateSmallBoard(smallBoard, macroboard);
                }
            }
        }

        return totalScore;
    }

    private String[][] extractSmallBoard(IField field, int macroX, int macroY) {
        String[][] board = field.getBoard();
        String[][] macroboard = field.getMacroboard();
        String[][] smallBoard = new String[3][3];

        // Extract the small board corresponding to the macroboard indices
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                // Calculate the coordinates of the small board in the larger board
                int x = macroX * 3 + i;
                int y = macroY * 3 + j;

                // Check if the small board at position (i, j) is available in the macroboard
                if (macroboard[macroX][macroY].equals(IField.AVAILABLE_FIELD)) {
                    // Copy the cell value from the larger board to the small board
                    smallBoard[i][j] = board[x][y];
                } else {
                    // If the small board is not available, mark it as an empty field
                    smallBoard[i][j] = IField.EMPTY_FIELD;
                }
            }
        }

        return smallBoard;
    }

    // Evaluate a single small board based on its configuration and return the score
    // Evaluate a single small board based on its configuration and return the score
    private int evaluateSmallBoard(String[][] smallBoard, String[][] macroBoard) {
        // Initialize score variables
        int score = 0;

        // Check for winning patterns
        // Example: Check rows
        for (int i = 0; i < 3; i++) {
            if (smallBoard[i][0].equals(smallBoard[i][1]) && smallBoard[i][1].equals(smallBoard[i][2])) {
                if (smallBoard[i][0].equals("X")) {
                    score += 100; // X wins
                } else if (smallBoard[i][0].equals("O")) {
                    score -= 100; // O wins
                }
            }
        }

        // Evaluate other factors
        // Example: Check if the small board is full
        boolean isFull = true;
        for (String[] row : smallBoard) {
            for (String cell : row) {
                if (cell.equals(IField.EMPTY_FIELD)) {
                    isFull = false;
                    break;
                }
            }
        }
        if (isFull) {
            score += 10; // Small board is full, add a positive score
        }

        // Add dynamic factors
        // 1. Control of macroboard positions
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (macroBoard[i][j].equals(IField.AVAILABLE_FIELD)) {
                    // Check if the current small board is active in the macroboard
                    if (smallBoard[i][j].equals("X")) {
                        score += 5; // Add a positive score for controlling active positions
                    } else if (smallBoard[i][j].equals("O")) {
                        score -= 5; // Add a negative score for opponent controlling active positions
                    }
                }
            }
        }

        // 2. Evaluate potential future moves
        // Example: Check for potential winning moves in rows
        for (int i = 0; i < 3; i++) {
            int xCount = 0;
            int oCount = 0;
            for (int j = 0; j < 3; j++) {
                if (smallBoard[i][j].equals("X")) {
                    xCount++;
                } else if (smallBoard[i][j].equals("O")) {
                    oCount++;
                }
            }
            if (xCount == 2 && oCount == 0) {
                score += 20; // Add a positive score for having two X's in a row with an empty cell
            } else if (oCount == 2 && xCount == 0) {
                score -= 20; // Add a negative score for opponent having two O's in a row with an empty cell
            }
        }

        // 3. Evaluate strategic positions
        // Add more strategic evaluations based on your game strategy

        return score;
    }



    // Select the best move based on the tree statistics
    private IMove selectBestMove(Node rootNode) {
        // Select the best child node using the UCB1 algorithm
        Node bestChildNode = selectNode(rootNode);

        // Check if a valid child node was selected
        if (bestChildNode != null) {
            // Return the move associated with the best child node
            return bestChildNode.getMove();
        } else {
            // No valid move found, return a default move or handle the situation according to your logic
            return getDefaultMove();
        }
    }

    private IMove getDefaultMove() {
        // Return a default move here or handle the situation according to your logic
        // For example, you can return a random move or a predefined move
        // If no moves are available, return null or handle the situation as needed
        return null;
    }
    // Node class to represent nodes in the Monte Carlo tree
    private class Node {
        private Node parent;
        private List<Node> children;
        private IMove move;
        private int visits;
        private int score;
        private IGameState state;

        public Node(Node parent, IMove move, IGameState state) {
            this.parent = parent;
            this.move = move;
            this.state = state;
            this.visits = 0;
            this.score = 0;
            this.children = new ArrayList<>();
        }

        public void addChild(Node child) {
            children.add(child);
        }
        public void updateStats(int score) {
            this.visits++;
            this.score += score;
            if (parent != null) {
                parent.updateStats(score);
            }
        }

        private IGameState getState() {

            return state;
        }

        public IMove getMove() {
            return move;
        }

        private Node[] getChildren() {
            return children.toArray(new Node[0]);
        }

        private Object getScore() {
            return score;
        }

        private int getVisits() {
            return visits;
        }
    }

    @Override
    public String getBotName() {
        return BOT_NAME;
    }
}