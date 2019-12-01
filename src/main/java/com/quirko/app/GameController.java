package com.quirko.app;

import java.io.IOException;

import com.quirko.gui.GuiController;
import com.quirko.logic.*;
import com.quirko.logic.events.EventSource;
import com.quirko.logic.events.InputEventListener;
import com.quirko.logic.events.MoveEvent;

public class GameController implements InputEventListener {

    private Board board = new SimpleBoard(20, 10);
    private String[] dialog = {"this is a good fish joke", 
    						   "have your heard my really good fish joke",
    						   "why did the fish cross the road?"};

    private final GuiController viewGuiController;

    public GameController(GuiController c) {
        viewGuiController = c;
        board.createNewBrick();
        viewGuiController.setEventListener(this);
        viewGuiController.initGameView(board.getBoardMatrix(), board.getViewData());
        viewGuiController.bindScore(board.getScore().scoreProperty());
    }

    @Override
    public DownData onDownEvent(MoveEvent event) {
        boolean canMove = board.moveBrickDown();
        ClearRow clearRow = null;
        if (!canMove) {
            board.mergeBrickToBackground();
            clearRow = board.clearRows();
            if (clearRow.getLinesRemoved() > 0) {
                board.getScore().add(clearRow.getScoreBonus());
                playFishJoke();
            }
            if (board.createNewBrick()) {
            	playGameOver();
                viewGuiController.gameOver();
            }


            int[] arduinoMatrix = getArduinoBoardMatrix(board.getBoardMatrix(), board.getViewData().getNextBrickData());
            // TODO pass it to Arduino.
            viewGuiController.refreshGameBackground(board.getBoardMatrix());

        } else {
            if (event.getEventSource() == EventSource.USER) {
                board.getScore().add(1);
            }
        }
        return new DownData(clearRow, board.getViewData());
    }

    @Override
    public ViewData onLeftEvent(MoveEvent event) {
        board.moveBrickLeft();
        return board.getViewData();
    }

    @Override
    public ViewData onRightEvent(MoveEvent event) {
        board.moveBrickRight();
        return board.getViewData();
    }

    @Override
    public ViewData onRotateEvent(MoveEvent event) {
        board.rotateLeftBrick();
        return board.getViewData();
    }


    @Override
    public void createNewGame() {
        board.newGame();
        viewGuiController.refreshGameBackground(board.getBoardMatrix());
    }
    
    private void playGameOver() {
    	try {    	
			Runtime.getRuntime().exec(String.format("say -v Yuri %s", "You've just been schooled!"));
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    private void playFishJoke() {
	 	int score = board.getScore().scoreProperty().get();
        
	 	int jokeIndex = (score / 500); // TODO adjust rate of progression, consider some random shuffling up or down a level.
	 	if (jokeIndex >= dialog.length) {
	 		jokeIndex = dialog.length-1;
	 	}
 	
        try {    	
			Runtime.getRuntime().exec(String.format("say -v Yuri %s", dialog[jokeIndex]));
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public static int[] getArduinoBoardMatrix(int[][] boardMatrix, int[][] nextMatrix) {
    	int boardLength = boardMatrix.length;
    	int boardWidth  = boardMatrix[0].length;
    	int nextLength  = nextMatrix.length;
    	int nextWidth   = nextMatrix[0].length;
    	
    	int totallength = boardLength * boardWidth + (nextLength+1) * (nextWidth+1) + 1;
    	int[] arduinoMatrix = new int[totallength];
    	
    	int x=0;
    	arduinoMatrix[x++] = 255;
    	for (int i=0; i<boardLength; i++) {
    		for (int j=0; j<boardWidth; j++) {
    			arduinoMatrix[x++] = boardMatrix[i][j];
    		}
    	}
    	for (int i=0; i<5; i++) {
    		arduinoMatrix[x++] = 0;
    	}
    	for (int i=0; i<nextLength; i++) {
    		arduinoMatrix[x++] = 0;
    		for (int j=0; j<nextWidth; j++) {
    				arduinoMatrix[x++] = nextMatrix[i][j];
    		}
    	}

		return arduinoMatrix;    	
    }
}
