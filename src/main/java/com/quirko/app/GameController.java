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
                // TODO check score thresholds and play a sound
                try {
                	int jokeIndex = 0;
                	int score = board.getScore().scoreProperty().get();
                	if (score > 1000) {
                		jokeIndex = 2;
                   	} else if (score > 500) {
                		jokeIndex = 1;
                	} 	
					Runtime.getRuntime().exec(String.format("say -v Yuri %s", dialog[jokeIndex]));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                
            }
            if (board.createNewBrick()) {
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
    
    private int[] getArduinoBoardMatrix(int[][] boardMatrix, int[][] nextMatrix) {
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
    	for (int i=0; i<=nextLength; i++) {
    		for (int j=0; j<=nextWidth; j++) {
    			if ((i==nextLength)||(j==nextWidth)) {
    				arduinoMatrix[x++] = 0;
    			} else {
    				arduinoMatrix[x++] = nextMatrix[i][j];
    			}
    		}
    	}

		return arduinoMatrix;    	
    }
}
