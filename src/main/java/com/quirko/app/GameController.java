package com.quirko.app;

import java.io.IOException;

import com.fazecast.jSerialComm.SerialPort;
import com.quirko.gui.GuiController;
import com.quirko.logic.*;
import com.quirko.logic.events.EventSource;
import com.quirko.logic.events.InputEventListener;
import com.quirko.logic.events.MoveEvent;

public class GameController implements InputEventListener {
	long lastUpdated = 0;
	static final long RATE_LIMIT_MS = 400;
	static final int ROW_INDEX_OFFSET = 100;
	static final int NEXT_PIECE_INDEX_OFFSET = ROW_INDEX_OFFSET + 20;
	SerialPort port;
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
 
    
		port = SerialPort.getCommPort("/dev/cu.usbmodem14101");
		port.setBaudRate(9600);
		port.openPort();
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

            updateFullBoard();
            viewGuiController.refreshGameBackground(board.getBoardMatrix());

        } else {
            if (event.getEventSource() == EventSource.USER) {
                board.getScore().add(1);
            }
    		if(System.currentTimeMillis() - lastUpdated > RATE_LIMIT_MS) {//rate limit, but update the board once in a while just in case
    			updateFullBoard();
    		}
        }
        return new DownData(clearRow, board.getViewData());
    }

	public void updateFullBoard() {
		byte[] arduinoMatrix = getArduinoBoardMatrix(board.getBoardMatrix(), board.getViewData());
		sendToArduino(arduinoMatrix);
		lastUpdated = System.currentTimeMillis();
	}
	
	public void updatePiece() {
		//if(System.currentTimeMillis() - lastUpdated < RATE_LIMIT_MS) {//rate limit
		//	return;
		//}
		byte[] arduinoMatrix = getArduinoPieceMatrix(board.getBoardMatrix(), board.getViewData());
		sendToArduino(arduinoMatrix);
		//lastUpdated = System.currentTimeMillis();
	}

	private void sendToArduino(byte[] arduinoMatrix) {
		int offset = 0;
		while(port.bytesAwaitingWrite() < 0) {
			System.out.println("waiting");
		}
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		port.writeBytes(arduinoMatrix,arduinoMatrix.length,offset);
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
    
    public static byte[] getArduinoBoardMatrix(int[][] boardMatrix, ViewData viewData) {
    	int[][] nextMatrix = viewData.getNextBrickData();
    	int boardLength = boardMatrix.length;
    	int boardWidth  = boardMatrix[0].length;
    	int nextLength  = nextMatrix.length;
    	int nextWidth   = nextMatrix[0].length;
    	
    	int totallength = boardLength * (boardWidth+1) + (nextLength+1) * (nextWidth+1+1);
    	byte[] arduinoMatrix = new byte[totallength];
    	
    	int x=0;
    	for (int i=0; i<boardLength; i++) {
    		arduinoMatrix[x++] = (byte)(ROW_INDEX_OFFSET+i);//row number
    		for (int j=0; j<boardWidth; j++) {
    			arduinoMatrix[x++] = (byte)boardMatrix[i][j];
    		}
    	}
    	arduinoMatrix[x++] = (byte)NEXT_PIECE_INDEX_OFFSET; //row number for next
    	for (int i=0; i<5; i++) {
    		arduinoMatrix[x++] = 0;
    	}
    	for (int i=0; i<nextLength; i++) {
    		arduinoMatrix[x++] = (byte)(NEXT_PIECE_INDEX_OFFSET+1+i);
    		arduinoMatrix[x++] = 0;
    		for (int j=0; j<nextWidth; j++) {
    				arduinoMatrix[x++] = (byte)nextMatrix[i][j];
    		}
    	}
    	
    	addCurrentBrick(arduinoMatrix, viewData.getBrickData(),viewData.getxPosition(),viewData.getyPosition());

		return arduinoMatrix;    	
    }
    
    //add the current brick into only the section of board it belongs in
    public static byte[] getArduinoPieceMatrix(int[][] boardMatrix, ViewData viewData) {
    	int totalLength = 11*4;//full width of 4 rows
    	byte[] arduinoMatrix = new byte[totalLength];
    	byte startRowIndex = (byte)(viewData.getyPosition() + ROW_INDEX_OFFSET);
    	int[][] brick = viewData.getBrickData();
    	boardMatrix = MatrixOperations.merge(boardMatrix, brick, viewData.getxPosition(), viewData.getyPosition());
    	for(int i=0;i<brick.length;++i) {
    		int targetY = viewData.getyPosition() + i;
    		arduinoMatrix[11*i] = (byte)(startRowIndex + i);
    		for(int j=0;j<boardMatrix[i].length;++j) {
    			int targetX = j;
    			int linearMatrix = 11*i + targetX + 1;
    			if(targetY <= 19) {
    				//the piece matrix can extend below the end of the board
    				arduinoMatrix[linearMatrix] = (byte)boardMatrix[targetY][targetX];
    			}
    		}
    	}
    	return arduinoMatrix;
    }
    
    //add the current brick into the entire board
    public static void addCurrentBrick(byte[] arduinoMatrix, int[][] brick, int x, int y) {
        for (int i = 0; i < brick.length; i++) {
            for (int j = 0; j < brick[i].length; j++) {
                int targetX = x + j;
                int targetY = y + i;
                int linearIndex = targetX+1 + 11*targetY;
                if (brick[i][j] != 0) {
                	arduinoMatrix[linearIndex] = (byte)brick[i][j];
                }
            }
        }
    }
}
