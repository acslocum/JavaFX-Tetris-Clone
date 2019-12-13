package com.quirko.app;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.fazecast.jSerialComm.SerialPort;
import com.quirko.gui.GuiController;
import com.quirko.logic.*;
import com.quirko.logic.events.EventSource;
import com.quirko.logic.events.InputEventListener;
import com.quirko.logic.events.MoveEvent;

public class GameController implements InputEventListener {
	long highScore = 0;
	long lastUpdated = 0;
	static final long RATE_LIMIT_MS = 400;
	static final int ROW_INDEX_OFFSET = 100;
	static final int NEXT_PIECE_INDEX_OFFSET = ROW_INDEX_OFFSET + 20;
	SerialPort port;
    private Board board = new SimpleBoard(20, 10);
    private String[] dialog = {"This is a fish joke",
    						   "Why did the fish, get bad grades?  Because it was below, sea level.",
    						   "What do you call a surgeon, on vacation?  A fishing, dock.",
    						   "How do you make an octopus laugh?  Give it, ten tickles.",
    						   "These jokes, usually flounder",
    						   "What does a fish do in a cry sis?  They sea, kelp.",
    						   "I made a joke about fish, but it tanked.",
    						   "It's too soon, for hurricane jokes, I'll wait for it, to blow over.",
    						   "What’s the difference between a piano and a toona?  You can toona piano, but you can’t piano a toona.",
    						   "Did you hear about the fight at the seafood restaurant?  Two fish got battered.",
    						   "I don’t always make fish jokes. But when I do, it’s just for the halibut.", 
    						   "What is a fishes fay vor right TV show? Toona Half Men.",
    						   "Who do fish pray to?  Cod Ulmighty.",
    						   "I’ve been telling too many fish jokes, I think I’ll scale back.",
    						   "I had a curlfriend, I lobster, but then I flounder",
    						   "I'm so angry, I could krill someone",
    						   "Keep your friends close, and your an enemies closer."
    						   };

    private String[] voices = {"Yuri", "Katya"};
    
    private final GuiController viewGuiController;

    public GameController(GuiController c) {
        viewGuiController = c;
        board.createNewBrick(false);
        viewGuiController.setEventListener(this);
        viewGuiController.initGameView(board.getBoardMatrix(), board.getViewData());
        viewGuiController.bindScore(board.getScore().scoreProperty());
 
    
		port = SerialPort.getCommPort("/dev/cu.usbmodem14201");
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
            boolean tetrisHappened = clearRow.getLinesRemoved() == 4;
            if (clearRow.getLinesRemoved() > 0) {
                board.getScore().add(clearRow.getScoreBonus());
                playFishJoke();
            }
            if (board.createNewBrick(tetrisHappened)) {
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
		byte[] arduinoMatrix = getArduinoPieceMatrix(board.getBoardMatrix(), board.getViewData());
		sendToArduino(arduinoMatrix);
	}

	private void sendToArduino(byte[] arduinoMatrix) {
		
		int offset = 0;
		while(port.bytesAwaitingWrite() < 0) {
			System.out.println("waiting");
		}
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
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
        playGameStart();
    }
    
    private String getRandomVoice() {
    	int index = ThreadLocalRandom.current().nextInt(voices.length);
    	return voices[index];
    }
    
    private void playGameStart() {
    	try {    	
    		Runtime.getRuntime().exec("/usr/local/bin/spotify play");
			Runtime.getRuntime().exec(String.format("say -v %s %s", getRandomVoice(), "If you know any good fish jokes, let minnow"));
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    private void playGameOver() {
    	int score = board.getScore().scoreProperty().get();
        
    	try {  
    		Runtime.getRuntime().exec("/usr/local/bin/spotify pause");
    		if (score > highScore) {
    			Runtime.getRuntime().exec(String.format("say -v %s %s %d", getRandomVoice(), "That was a, whale, of a performance. Your score is,", score));
    			highScore = score;
    		} else {
    			Runtime.getRuntime().exec(String.format("say -v %s %s %d", getRandomVoice(), "You've just been schooled. Your score is,", score));
    		}
    		
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    private void playFishJoke() {
    	int jokeIndex = ThreadLocalRandom.current().nextInt(dialog.length);
        try {    	
			Runtime.getRuntime().exec(String.format("say -v %s %s", getRandomVoice(), dialog[jokeIndex]));
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
