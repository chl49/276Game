package Game;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;

import java.io.BufferedReader;
import java.io.FileReader;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

public class GameManager extends JPanel implements ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private Dimension d;
    private final int BLOCK_SIZE = 30;  //sprite size is 30x30 pixels
    // private final int N_BLOCKS = 20; //20x20 grid perhaps
    // private final int SCREEN_SIZE = N_BLOCKS * BLOCK_SIZE; //20*30 = 600 for Length
    
    //private final int MoveDistance = BLOCK_SIZE;

    // private int playerX, playerY;
    // private int moveX, moveY;
    // private int cooldown=30;  //TICK TIME
    private Timer timer;

    private static GameManager _instance = null;

    private Board board;
    private Player player;
    private Rewards rewards;
    private ArrayList<Renderable> renderables = new ArrayList<Renderable>();
    private ArrayList<Movable> movables = new ArrayList<Movable>();
    private AIPathManager pathManager;
    private ScoreManager scoreManager;
    private boolean isDirty = false;
    private ArrayList<Interactable> interactable = new ArrayList<Interactable>();

    public static GameManager instance()
    {
        if(_instance == null)
        {
            _instance = new GameManager();
        }

        return _instance;
    }

    private void init()
    {
        initTimer();
        initBoard();
        initRendering();
        initEntities();
        initControls();
        
        pathManager = new AIPathManager(player, board);
        scoreManager = new ScoreManager(5); //TODO: make this 5 the number of required rewards from the board
    }

    private void initTimer() {
        timer = new Timer(30, this);
        timer.start();
    }

    private void initBoard() {
        createBoard();
    }

    private void initRendering()
    {
        d = new Dimension(600, 600);
        //d = new Dimension(board.rowSize, board.rowCount);

        setFocusable(true);

        setBackground(Color.black);
    }

    private void initEntities()
    {
        player = new Player();
        renderables.add(player);
        movables.add(player);
        player.setPosition(16);

        //TODO: enemies and stuff
        Enemy enemy = new Enemy();
        renderables.add(enemy);
        movables.add(enemy);
        enemy.setPosition(10);

        Rewards reward = new Rewards(18, 1);
        renderables.add(reward);
        interactable.add(reward);
    }

    private void initControls() {
        addKeyListener(new TAdapter());
    }

    private void createBoard() {
        String gridString = "";
        int noOfRows = 0;
        int noOfColumns = 0;
        String contentsOfArray = "";
        try {
            board = new Board("Source/CMPT276-25748/src/resources/input.txt");
            renderables.add(board);
            Cell[] newCellArray = board.getCellArray();
            noOfRows = board.getNoOfRows();
            noOfColumns = board.getNoOfColumns();
            for (int i = 0; i < (noOfRows*noOfColumns); i++) {
                contentsOfArray += newCellArray[i].getCellChar();
            }
            System.out.println("Contents from the text file: " + board.getFileContent()
                + "\nNumber of Rows = " + Integer.toString(noOfRows)
                + "\nNumber of Columns = " + Integer.toString(noOfColumns)
                + "\nContents from the Cell Array: " + contentsOfArray
                + "\nTotal number of cells: " + (noOfRows*noOfColumns));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    

    @Override
    public void addNotify() {
        //START UP HERE
        super.addNotify();

        init(); //init game
    }

    @Override
    public void paintComponent(Graphics g) {
        //DRAW LOOP
        super.paintComponent(g);

        doDrawing(g);
    }

    private void doDrawing(Graphics g) {

        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(Color.black);
        g2d.fillRect(0, 0, d.width, d.height);

        for(var r : renderables)
        {
            if(r.isVisible())
                r.draw(g2d);
        }
    }

    class TAdapter extends KeyAdapter {
        //KEY INPUTS

        @Override
        public void keyPressed(KeyEvent e) {

            int moveX = 0;
            int moveY = 0;

            int key = e.getKeyCode();
            if (key == KeyEvent.VK_LEFT) {
                moveX = -1;
                moveY = 0;
            } else if (key == KeyEvent.VK_RIGHT) {
                moveX = 1;
                moveY = 0;
            } else if (key == KeyEvent.VK_UP) {
                moveX = 0;
                moveY = -1;
            } else if (key == KeyEvent.VK_DOWN) {
                moveX = 0;
                moveY = 1;
            } else if (key == KeyEvent.VK_PAUSE) {
                if (timer.isRunning()) {
                    timer.stop();
                } else {
                    timer.start();
                }
            }
            player.setNextPosition(Helper.move(player.getPosition(), moveX, moveY));
            isDirty = true;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if(isDirty)
        {
            updateGameLogic();
            isDirty = false;
        }
        
        updateMovables();
        
        repaint();
    }

    private void updateGameLogic()
    {
        updateEnemyPathing();
        updateMovables();
        updateInteractions();

        //check win/lose conditions
        if(checkGameConditions())
        {
            //game is over, handle it
        }
    }

    private void updateMovables()
    {
        for(var m : movables)
        {
            Cell[] newCellArray = board.getCellArray();
            if(newCellArray[m.getNextPosition()].getCellChar() != 'o') {
                m.setNextPosition(m.getPosition());
            }
            m.updatePosition();
        }
    }

    private void updateEnemyPathing()
    {
        for(var m : movables)
        {
            if(m instanceof Enemy)
            {
                pathManager.setNextPosition(m);
            }
        }
    }

    private void updateInteractions()
    {
        for(var i : interactable)
        {
            if(!i.isActive())
            {
                continue;
            }

            if(player.getPosition() == i.getPosition())
            {
                if(i instanceof Rewards)
                {
                    scoreManager.addRequiredReward(i.getScore());
                }

                //TODO: implement penalty
                /*if(i instanceof Penalty)
                {
                    scoreManager.addPenalty(i.getScore());
                }*/

                i.setActive(false);
            }
        }
    }

    //returns true if game should end
    private boolean checkGameConditions()
    {
        if(scoreManager.hasReachedRewardsGoal())
        {
            //win!
            return true;
        }

        for(var m : movables)
        {
            if(m instanceof Enemy)
            {
                if(m.getPosition() == player.getPosition())
                {
                    //lose!
                    return true;
                }
            }
        }

        return false;
    }

    public Board getBoard()
    {
        return board;
    }

    public void drawImage(Image image, Graphics2D g2d, int xPos, int yPos)
    {
        g2d.drawImage(image, xPos * BLOCK_SIZE, yPos * BLOCK_SIZE, this);
    }
}
