package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.text.DefaultCaret;

public class GameScreenGUI extends JFrame{

	private static final long serialVersionUID = 1L;
	private ImagePanel board;
	private JPanel chatBox;
	private JPanel progressPanel;
	private JTextArea chat;
	private JTextField chatEdit;
	private JLabel[][] spaces;
	private Timer progressTimer;
	private JProgressBar progressBar;
	private JLabel task;	
	
	private JLabel levelTimer;
	private JLabel teamGold;
	private JLabel lives;
	
	private int timerInt = 60;
	private int goldEarned = 0;
	private String message;
	private int livesInt = 10;
	
	private Timer lvlTimer;
	
	private Board backendBoard;
	
	private int nextIndex = 0;
	private int previousIndex = 0;
	
	private Player currentPlayer;
	private boolean isHost;
	
	private Vector<ChatThread> ctVector = new Vector<ChatThread>();
	private ChatThread ct;
	private ServerSocket ss;
	private Socket s;
	private BufferedReader br;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	private Object obj;
	
	private boolean isConnected = false;

	private Tower currentTower;
		
	private Vector<Player> players; 
	private HashMap<Integer, Creep> creeps;
	private Level [] levels;
	
	private int MAX_CREEPS = 10;
	
	private ImageIcon creepImage;
	private ImageIcon bulletImage;
	private ImageIcon explosionImage;
	private ImageIcon mineralImage;
	
	private boolean cooldown = false;
	
	private int timer = 1000;
	private int numLevels = 4;
	private int level = 0;
	private Timer cooldownTimer;
	
	private int numCreeps;
//	
//	private static Lock lock = new ReentrantLock();
//	private static Condition allCreepsDead = lock.newCondition();
	
	User currentUser;
	
	public GameScreenGUI(Board b, Player p, boolean isHost, User u)
	{
		
		currentUser = u;
		
		cooldownTimer = new Timer(500, new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				cooldown = false;
				cooldownTimer.stop();
			}
		}
		);
		
		
		levels = new Level[numLevels];
		levels[0] = new Level(10, 2000, 4000, 5);
		levels[1] = new Level(10, 1000, 2000, 10);
		levels[2] = new Level(20, 800, 1600, 20);
		levels[3] = new Level(30, 400, 800, 30);
		players = new Vector<Player>();
		creeps = new HashMap<Integer, Creep>();
		
		this.setTitle(p.getPlayerName());
		this.setSize(825,510);
		this.setLocation(0,0);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		this.setLayout(new BorderLayout());
		this.setResizable(false);
		
		message = "";

		this.backendBoard = b;
		
		this.currentPlayer = p;
		
		players.add(currentPlayer);
				
		this.isHost = isHost;
		
		board = this.createBoard();
		
		this.add(board, BorderLayout.CENTER);
		
		chatBox = this.getChatBox();
		
		this.add(chatBox, BorderLayout.EAST);
				
		progressPanel = this.getProgressPanel();
				
		this.add(progressPanel, BorderLayout.SOUTH);
		
		this.add(getTopPanel(), BorderLayout.NORTH);
				
		this.setVisible(true);
		try
		{
			BufferedImage image = ImageIO.read(new File("images/Explosion.png"));
			Image temp = image.getScaledInstance(spaces[0][0].getWidth(), spaces[0][0].getHeight(), 0);
			explosionImage = new ImageIcon(temp);
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
		
			
		try
		{
			BufferedImage image = ImageIO.read(new File("images/Creep.png"));
			Image temp = image.getScaledInstance(spaces[0][0].getWidth(), spaces[0][0].getHeight(), 0);
			creepImage = new ImageIcon(temp);
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
		try
		{
			BufferedImage image = ImageIO.read(new File("images/bulletSprite.png"));
			Image temp = image.getScaledInstance(spaces[0][0].getWidth(), spaces[0][0].getHeight(), 0);
			bulletImage = new ImageIcon(temp);
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
		try
		{
			BufferedImage image = ImageIO.read(new File("images/Minerals.png"));
			Image temp = image.getScaledInstance(spaces[0][0].getWidth(), spaces[0][0].getHeight(), 0);
			mineralImage = new ImageIcon(temp);
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
		
		Timer time = new Timer(10, new ActionListener()
		{
			public void actionPerformed(ActionEvent ae) {
				updateBoard();
			}
		});
		time.start();
		
		
		board.addMouseListener(new MouseAdapter()
		{
            public void mouseClicked(MouseEvent e) {
                board.requestFocusInWindow();
            }
		});
		
		new CreateConnections().start();
		
		

		lvlTimer = new Timer(1000, new ActionListener()
		{
			public void actionPerformed(ActionEvent ae) {
				
				timerInt = creeps.size();
				
				Command c = new Command(currentPlayer, "Timer", timerInt, 0);
				sendMessageToClients(c);
				
				levelTimer.setText("Creeps Remaining: " + timerInt);
			}
			
			
		});
		
		if(isHost == true)
		{
			lvlTimer.start();
		}
		
		
	}//end of constructor
	
	private boolean getIsConnected(){
		return isConnected;
	}
	
	private JPanel getTopPanel()
	{
		JPanel toReturn = new JPanel();
		
		toReturn.setLayout(new BoxLayout(toReturn, BoxLayout.X_AXIS));
		
		levelTimer = new JLabel("" + timerInt);
		
		teamGold = new JLabel("Gold: " + goldEarned);
		
		lives = new JLabel("Lives: " + livesInt);
		
		toReturn.add(lives);
		toReturn.add(Box.createGlue());
		toReturn.add(Box.createGlue());

		
		toReturn.add(levelTimer);
		
		toReturn.add(Box.createGlue());
		
		toReturn.add(Box.createGlue());
		
		toReturn.add(teamGold);
		
		toReturn.add(Box.createGlue());
		
		return toReturn;
	}
	
	private ImagePanel createBoard()
	{		
		ImagePanel toReturn = new ImagePanel(new ImageIcon("TowerDefense.png").getImage());
		
		toReturn.setSize(600,700);
		toReturn.setPreferredSize(toReturn.getSize());
		
		toReturn.setLayout(new GridLayout(20,32));
		spaces = new JLabel[20][32];
		
		for(int i = 0; i < 20; i++)
		{
			for(int j = 0; j < 32; j++)
			{
				spaces[i][j] = new JLabel("");
				spaces[i][j].setBorder(BorderFactory.createLineBorder(Color.black));
				spaces[i][j].setOpaque(false);
				toReturn.add(spaces[i][j]);
			}
		}
		
		toReturn.setFocusable(true);
		toReturn.requestFocusInWindow();
		
		return toReturn;
	}
	
	private JPanel getChatBox()
	{
		JPanel toReturn = new JPanel();
		toReturn.setLayout(new BorderLayout());
		
		toReturn.setSize(200,500);
		
		toReturn.setPreferredSize(toReturn.getSize());
		
		
		chat = new JTextArea();
		chat.setLineWrap(true);
		chat.setWrapStyleWord(true);
		DefaultCaret caret = (DefaultCaret)chat.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		JScrollPane sp = new JScrollPane(chat);
		chatEdit = new JTextField();
		
		chat.setEditable(false);
		
		toReturn.add(sp, BorderLayout.CENTER);
		toReturn.add(chatEdit, BorderLayout.SOUTH);
				
		return toReturn;
	}
	
	
	private JPanel getProgressPanel()
	{
		JPanel toReturn = new JPanel();
		toReturn.setSize(100,50);
		toReturn.setPreferredSize(toReturn.getSize());
		toReturn.setLayout(new BoxLayout(toReturn, BoxLayout.Y_AXIS));
		
		JPanel topPanel = new JPanel();
		task = new JLabel("Task in Progress");
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		topPanel.add(Box.createGlue());
		topPanel.add(task);
		topPanel.add(Box.createGlue());
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
		progressBar = new JProgressBar(0, 100);
		progressBar.setString("No Task");
		progressBar.setStringPainted(true);
		progressBar.setBackground(new Color(139,69,19));
		progressBar.setForeground(new Color(0, 100, 0));
		bottomPanel.add(Box.createGlue());
		bottomPanel.add(progressBar);
		bottomPanel.add(Box.createGlue());
		
		toReturn.add(topPanel);
		toReturn.add(bottomPanel);

		return toReturn;
	}
	
	private void cancelBuildingTower()
	{
		timer = 100;
		progressTimer.stop();
		progressBar.setString("No Task");
		progressBar.setValue(0);
	}
	private void cancelMining()
	{
		timer = 100;
		progressTimer.stop();
		progressBar.setString("No Task");
		progressBar.setValue(0);
	}
	
	private synchronized void createActions()
	{
	
		board.addKeyListener(new KeyAdapter()
		{
			public void keyPressed(KeyEvent ke) {

				int key = ke.getKeyCode();
				//System.out.println(key);
				int playerx = currentPlayer.getLocation().getX();
				int playery = currentPlayer.getLocation().getY();
//				
//				System.out.println("Before " + playerx + " " + playery);
				
				if(key == ke.VK_UP)
				{
					try {
						
						if(progressBar.getString().startsWith("Building Tower"))
						{
							cancelBuildingTower();
						}
						if(progressBar.getString().startsWith("Mining Space"))
						{
							cancelMining();
						}
						
						currentPlayer.move(0);
						currentPlayer.setPlayerDirection("NORTH");

						if(currentPlayer.moveableCouldMove())
						{
							
							if(isHost){
								sendMessageToClients(new Command(currentPlayer, "Move(0)"));
							}
							else{
								oos.writeObject(new Command(currentPlayer, "Move(0)"));
								oos.flush();
							}	
						}
						else
						{
							if(isHost)
							{
								sendMessageToClients(new Command(currentPlayer, "Turn(0)"));
							}
							else
							{
								oos.writeObject(new Command(currentPlayer, "Turn(0)"));
								oos.flush();
							}
						}
					} catch (BoundaryException e) {
						// TODO Auto-generated catch block

						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if(key == ke.VK_DOWN)
				{
					try {
						if(progressBar.getString().startsWith("Building Tower"))
						{
							cancelBuildingTower();
						}
						if(progressBar.getString().startsWith("Mining Space"))
						{
							cancelMining();
						}
						
						currentPlayer.move(1);
						currentPlayer.setPlayerDirection("SOUTH");
						if(currentPlayer.moveableCouldMove())
						{
							if(isHost){
								sendMessageToClients(new Command(currentPlayer, "Move(1)"));
							}
							else{
								oos.writeObject(new Command(currentPlayer, "Move(1)"));
								oos.flush();
							}	
						}
						else
						{
							if(isHost)
							{
								sendMessageToClients(new Command(currentPlayer, "Turn(1)"));
							}
							else
							{
								oos.writeObject(new Command(currentPlayer, "Turn(1)"));
								oos.flush();
							}
						}

					}catch (BoundaryException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if(key == ke.VK_RIGHT)
				{
					try {
						if(progressBar.getString().startsWith("Building Tower"))
						{
							cancelBuildingTower();
						}
						if(progressBar.getString().startsWith("Mining Space"))
						{
							cancelMining();
						}
						
						currentPlayer.move(2);
						currentPlayer.setPlayerDirection("EAST");
						if(currentPlayer.moveableCouldMove())
						{
							if(isHost){
								sendMessageToClients(new Command(currentPlayer, "Move(2)"));
							}
							else{
								oos.writeObject(new Command(currentPlayer, "Move(2)"));
								oos.flush();
							}	
							
						}
						else
						{
							if(isHost)
							{
								sendMessageToClients(new Command(currentPlayer, "Turn(2)"));
							}
							else
							{
								oos.writeObject(new Command(currentPlayer, "Turn(2)"));
								oos.flush();
							}
						}
					} 
					catch (BoundaryException e) {
						e.printStackTrace();
					}
					catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if(key == ke.VK_LEFT)
				{
					try {
						if(progressBar.getString().startsWith("Building Tower"))
						{
							cancelBuildingTower();
						}
						if(progressBar.getString().startsWith("Mining Space"))
						{
							cancelMining();
						}
						
						currentPlayer.move(3);
						currentPlayer.setPlayerDirection("WEST");
						if(currentPlayer.moveableCouldMove())
						{
							if(isHost){
								sendMessageToClients(new Command(currentPlayer, "Move(3)"));
							}
							else{
								oos.writeObject(new Command(currentPlayer, "Move(3)"));
								oos.flush();
							}	
						}
						else
						{
							if(isHost)
							{
								sendMessageToClients(new Command(currentPlayer, "Turn(3)"));
							}
							else
							{
								oos.writeObject(new Command(currentPlayer, "Turn(3)"));
								oos.flush();
							}
						}
					} catch (BoundaryException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				else if(key == ke.VK_SPACE)
				{	
					if(currentPlayer.playerOperatingTower() != null && cooldown == false)
					{
						Tower t = currentPlayer.playerOperatingTower();
						t.shoot();
						
						cooldown = true;
						
						cooldownTimer.start();
						
						Command c = new Command(currentPlayer, "Shoot", t.getX(), t.getY());
						
						
						try
						{
							if(isHost){
								sendMessageToClients(c);
							}
							else{
								oos.writeObject(c);
								oos.flush();
							}	
						}
						catch(IOException ioe)
						{
							ioe.printStackTrace();
						}
					}
					else if(currentPlayer.playerOperatingTower() == null)
					{
						if(currentPlayer.getPlayerDirection() == "SOUTH")
						{
							if(backendBoard.getSpace(playerx+1, playery) instanceof MineableSpace)
							{
								if(progressBar.getString().startsWith("Mining Space"))
								{
									return;
								}
								mineSpaces(playerx+1, playery, true);
							}	
							else if(playerx+1 < 20)
							{
								if(goldEarned < 1 || progressBar.getString().startsWith("Building Tower"))
								{
									return;
								}
								placeTower(playerx+1, playery, true);
							}
						}
						else if(currentPlayer.getPlayerDirection() == "NORTH")
						{
							if(backendBoard.getSpace(playerx-1, playery) instanceof MineableSpace)
							{
								if(progressBar.getString().startsWith("Mining Space"))
								{
									return;
								}
								mineSpaces(playerx-1, playery, true);
							}	
							else if(playerx-1 > 0)
							{
								if(goldEarned < 1 || progressBar.getString().startsWith("Building Tower"))
								{
									return;
								}
								placeTower(playerx-1, playery, true);
							}
						}
						
						else if(currentPlayer.getPlayerDirection() == "WEST")
						{
							if(backendBoard.getSpace(playerx, playery-1) instanceof MineableSpace)
							{
								if(progressBar.getString().startsWith("Mining Space"))
								{
									return;
								}
								mineSpaces(playerx, playery-1, true);
							}	
							else if(playery-1 > 0)
							{
								if(goldEarned < 1 || progressBar.getString().startsWith("Building Tower"))
								{
									return;
								}
								placeTower(playerx, playery-1, true);
							}
						}
						else if(currentPlayer.getPlayerDirection() == "EAST")
						{
							if(backendBoard.getSpace(playerx, playery+1) instanceof MineableSpace)
							{
								if(progressBar.getString().startsWith("Mining Space"))
								{
									return;
								}
								mineSpaces(playerx, playery+1, true);
							}	
							else if(playery+1 < 32)
							{
								if(goldEarned < 1 || progressBar.getString().startsWith("Building Tower"))
								{
									return;
								}
								placeTower(playerx, playery+1, true);
							}
						}
					}
				}
				else if(key == ke.VK_SHIFT)
				{
					if(currentPlayer.playerOperatingTower() != null)
					{
						Tower t = currentPlayer.playerOperatingTower();
						
						if(isHost)
						{
							t.rotate();
						}
						
						if(t instanceof BasicTower)
						{
							int x = ((BasicTower) t).getX();
							int y = ((BasicTower) t).getY();
							BufferedImage image = ((BasicTower) t).getTowerImages();
							Image icon = image.getScaledInstance(spaces[x][y].getWidth(), spaces[x][y].getHeight(), Image.SCALE_SMOOTH);
							spaces[x][y].setIcon(new ImageIcon(icon));

							Command c = new Command(currentPlayer, "RotateTower", x, y);
							try
							{
								if(isHost)
								{
									sendMessageToClients(c);
								}
								else{
									oos.writeObject(c);
									oos.flush();
								}	
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
							
						}
					}
				}

			}
			
		});
		
		chatEdit.addKeyListener(new KeyAdapter()
		{
			public void keyPressed(KeyEvent ke) {
			
				int key = ke.getKeyCode();
				
				if(key == ke.VK_ENTER && chatEdit.getText() != null){
					//System.out.println("setting messageSent to true");					 
					String toAppend = "\n"+currentPlayer.getPlayerName() + ": " + chatEdit.getText() + "\n";
					message = toAppend;
					chatEdit.setText("");
					System.out.println("enter was hit, sending message: " + message);
					if(isHost){
						chat.append(message);
						sendMessageToClients(message);
					}	
					else{
						try {
							oos.writeObject(message);
							oos.flush();
						} catch (IOException e) {
							System.out.println("IOE in GameRoom.Chatclient.run() in while loop writing string object");
						}//end of try-catch
					}	
					
				}
				
			}

		});
	}
	
	public void mineSpaces(int x, int y, boolean miner)
	{
		timer = 100;
		
		progressTimer = new Timer(10, new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				
				if(timer > 0)
				{
					if(miner == true)
					{
						progressBar.setString("Mining Space (" + timer/10 + "s)");
						progressBar.setStringPainted(true);
						progressBar.setValue(progressBar.getValue() + 1);
					}
					timer--;
				}
				else
				{
					if(miner == true)
					{
						progressBar.setString("No Task");
						progressBar.setValue(0);
						if(backendBoard.getSpace(x,y) instanceof MineableSpace)
						{
							if(isHost)
							{
								int valueMined = ((MineableSpace)(backendBoard.getSpace(x, y))).mine();
								goldEarned = goldEarned + valueMined;
							}
							teamGold.setText("Gold:" + goldEarned);
							
							if(isHost)
							{
								sendMessageToClients(new Command(currentPlayer, "Mine", x, y));
							}
							else
							{
								try
								{
									oos.writeObject(new Command(currentPlayer, "Mine", x, y));
									oos.flush();
								}
								catch (IOException ioe)
								{
									ioe.printStackTrace();
								}
							}
							
						}
					}
					progressTimer.stop();
				}
				
			}
		});
		progressTimer.start();

	}
	
	public void startGame(){
		new StartGameThread().start();
		System.out.println("game is started");
	}
	
	class StartGameThread extends Thread{
		private Level l;
		public StartGameThread(){
			System.out.println("run");
		}
		public void run(){
			while(true){
				l = levels[level];
				numCreeps = l.getNumber();
				while(numCreeps>0){ //there are remaining creeps
					try {
						Thread.sleep(l.getFrequency());
						Creep c = new Creep(backendBoard.getPathSpace(0), l.getHealth(), l.getSpeed());
						creeps.put(numCreeps, c);
						c.start();
						//new Creep(backendBoard.getPathSpace(0)).start();
						numCreeps--;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}	
				}
				while(creeps.size()>0){
					System.out.println(creeps.size());
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				System.out.println("dead");
				try {
					//allCreepsDead.await();
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				level++;
				if(level == numLevels){
					//team has beat the game
					break;
				}
			}
			
			//end of if end of level 	
		}
	}//end of startgame thread
	
	
	public void updateBoard()
	{
		for(int i = 0; i<MAX_CREEPS; i++){
			if(creeps.containsKey(i)){
				Creep c = creeps.get(i);
				int x = c.getPathLocation().getX();
				int y = c.getPathLocation().getY();
				if(c.isDead()){
					creeps.remove(i);
//					if(creeps.size()==0){
//						allCreepsDead.signalAll();
//					}
					spaces[x][y].setBorder(BorderFactory.createLineBorder(Color.BLACK));
					new ExplosionThread(x, y).start();

				}
				else if(c.isOffGrid()){
					creeps.remove(i);
//					if(creeps.size()==0){
//						allCreepsDead.signalAll();
//					}
					livesInt--;
					lives.setText("Lives: " + livesInt);
				}
				else{
					//spaces[x][y].setBorder(BorderFactory.createLineBorder(Color.RED))
					spaces[x][y].setIcon(creepImage);
				
				}
				if(c.getPrevious() !=null && !c.getPrevious().isOccupied()){
					int p = c.getPrevious().getX();
					int q = c.getPrevious().getY();
					//spaces[p][q].setBorder(BorderFactory.createLineBorder(Color.BLACK));
					spaces[p][q].setIcon(null);
				}
			}
			
		}
	
		for(int i=0; i < players.size(); i++)
		{
			Player p = players.get(i);
			int playerx = p.getLocation().getX();
			int playery = p.getLocation().getY();
			
			//spaces[playerx][playery].setBorder(BorderFactory.createLineBorder(Color.YELLOW));
			
			Image image = p.getIcon().getScaledInstance(spaces[playerx][playery].getWidth(), spaces[playerx][playery].getHeight(), Image.SCALE_SMOOTH);
			
			spaces[playerx][playery].setIcon(new ImageIcon(image));
			
			if(p.getPrevious() != null && p.moveableCouldMove())
			{
				int x = p.getPrevious().getX();
				int y = p.getPrevious().getY();
				//spaces[x][y].setBorder(BorderFactory.createLineBorder(Color.BLACK));
				spaces[x][y].setIcon(null);
			}
		}
		
		//bullets
		for(int i = 0; i < 20; i++)
		{
			for(int j = 0; j < 32; j++)
			{
				if(backendBoard.getSpace(i, j) instanceof MineableSpace)
				{
					spaces[i][j].setIcon(mineralImage);
					MineableSpace mine = (MineableSpace)(backendBoard.getSpace(i, j));
					if(mine.getAvailable() == 0)
					{
						backendBoard.setBlank(mine);
						spaces[i][j].setIcon(null);
					}
					
				}
				
				if(backendBoard.getSpace(i, j).isOccupied())
				{
					
					if(backendBoard.getSpace(i, j).getMoveable() instanceof Bullet){
						//spaces[i][j].setBorder(BorderFactory.createLineBorder(Color.GREEN));
						spaces[i][j].setIcon(bulletImage);
						//normal movement
						if(backendBoard.getSpace(i, j).getMoveable().getPrevious() != null ){//&& !backendBoard.getSpace(i,j).getMoveable().getPrevious().isOccupied()){
							int x = backendBoard.getSpace(i, j).getMoveable().getPrevious().getX();
							int y = backendBoard.getSpace(i, j).getMoveable().getPrevious().getY();
							//spaces[x][y].setBorder(BorderFactory.createLineBorder(Color.BLACK));
							if(!(backendBoard.getSpace(i, j).getMoveable().getPrevious() instanceof TowerSpace)){
								spaces[x][y].setIcon(null);
							}
							
							
						}
						//bullet reaching end of map
						if(!backendBoard.getSpace(i, j).getMoveable().moveableCouldMove()){	
							int x = backendBoard.getSpace(i, j).getMoveable().getLocation().getX();
							int y = backendBoard.getSpace(i, j).getMoveable().getLocation().getY();
							backendBoard.getSpace(i, j).removeOccupant();
							//spaces[x][y].setBorder(BorderFactory.createLineBorder(Color.BLACK));
							if(!(backendBoard.getSpace(i, j) instanceof TowerSpace)){
								spaces[x][y].setIcon(null);
							}
						}
						
					}
//					if(backendBoard.getSpace(i,j).getMoveable().getMoveableImage() != null)
//					{
//						ImageIcon icon = new ImageIcon(backendBoard.getSpace(i,j).getMoveable().getMoveableImage());
//					
//						spaces[i][j].setIcon(icon);
//					
//					}
				}
			}
		}
	}
	
	public void placeTower(int x, int y, boolean maker)
	{	
		if(backendBoard.getSpace(x, y) instanceof PathSpace){
			return;
		}
		if(backendBoard.getSpace(x, y) instanceof TowerSpace)
		{
			return;
		}
		if(backendBoard.getSpace(x, y) instanceof MineableSpace)
		{
			return;
		}
		
		BasicTower b = new BasicTower(backendBoard.getSpace(x, y));
		
		BufferedImage img = b.getTowerImages();
		
		Image resizedImage = img.getScaledInstance(spaces[x][y].getWidth(), spaces[x][y].getHeight(), Image.SCALE_SMOOTH);

		
		if(x < 0 || y < 0 || x > 19 || y > 30)
		{
			return;
		}
			
		timer = 100;
		
		goldEarned--;
		teamGold.setText("Gold: " + goldEarned);
		
		Command c = new Command(currentPlayer, "BuyTower");
		
		try
		{
			if(!isHost)
			{
				oos.writeObject(c);
				oos.flush();	
			}
			else
			{
				sendMessageToClients(c);
			}

		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		
		
		progressTimer = new Timer(50, new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				
				if(timer > 0)
				{
					if(maker == true)
					{
						progressBar.setString("Building Tower (" + timer/10 + "s)");
						progressBar.setStringPainted(true);
						progressBar.setValue(progressBar.getValue() + 1);
					}
					timer--;
				}
				else
				{
					if(maker == true)
					{
						progressBar.setString("No Task");
						progressBar.setValue(0);
					}
					Command c = new Command(currentPlayer, "PlaceTower", x, y);
					try {
						if(!isHost)
						{
							oos.writeObject(c);
							oos.flush();	
						}
						else
						{
							sendMessageToClients(c);
						}

					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
					
					
					spaces[x][y].setIcon(new ImageIcon(resizedImage));
					backendBoard.placeTower(backendBoard.getSpace(x,y));
					progressTimer.stop();
				}
				
			}
		});
		progressTimer.start();
	}
	
	public void placeTowerImmediately(int x, int y)
	{
		BasicTower b = new BasicTower(backendBoard.getSpace(x, y));
		
		BufferedImage img = b.getTowerImages();
		
		Image resizedImage = img.getScaledInstance(spaces[x][y].getWidth(), spaces[x][y].getHeight(), Image.SCALE_SMOOTH);

		
		spaces[x][y].setIcon(new ImageIcon(resizedImage));
		backendBoard.placeTower(backendBoard.getSpace(x,y));
	}
	
	public void restartLevelTimer()
	{
		timerInt = 60;
	}
	
	public void removeChatThread(ChatThread ct) {
		ctVector.remove(ct);
	}
	public synchronized void sendMessageToClients(Object obj) {
		if(isHost){
			for (ChatThread ct1 : ctVector) {
				System.out.println("sending msg: " + obj.getClass());
				ct1.sendMessage(obj);
			}
		}	
	}
	
	
	class ChatThread extends Thread {
		//private BufferedReader br;
		private ObjectOutputStream oos;
		private ObjectInputStream ois;
		//private GameRoomGUI grg;
		private Socket s;
		public ChatThread(Socket s) {
			//this.grg = grg;
			this.s = s;
			try {
				ois = new ObjectInputStream(s.getInputStream());
				oos = new ObjectOutputStream(s.getOutputStream());
				oos.writeObject("Connected to server!\n");
				oos.flush();
			} catch (IOException ioe) {
				System.out.println("IOE in ChatThread constructor: " + ioe.getMessage());
			}
		}//end of chat thread

		public void sendMessage(Object obj) {
			try {
				oos.writeObject(obj);
				oos.flush();
			} catch (IOException e) {
				System.out.println("IOE from ChatThread.sendMessage(): "+e.getMessage());
			}
		}//end of send message

		public synchronized void run(){
			try {
				obj = ois.readObject();
				while(obj != null){
					if(obj instanceof String){				
						chat.append(((String)obj));
						System.out.println("sending chat message to other clients: "+(String)obj);
						sendMessageToClients(obj);
					}//end of if ob is String
					else if(obj instanceof Player)
					{	
						sendMessageToClients(currentPlayer);
						backendBoard.setPlayer((Player)obj);
						players.add((Player)obj);
						sendMessageToClients(obj);
						sendMessageToClients(new Integer(-1));
						System.out.println("before startgame");
						startGame();
						System.out.println("not a blocking line");
						
					}
					else if(obj instanceof Command)
					{
						Player player = ((Command)obj).getPlayer();
						
						String command = ((Command)obj).getCommand();

						for(int i=0; i<players.size(); i++){
							Player p = players.get(i);
							if(player.getPlayerName() == p.getPlayerName())
							{
								if(command.equals("Move(0)"))
								{
									try
									{
										p.setPlayerDirection("NORTH");
										p.move(0);

									}
									catch (BoundaryException e) {
										e.printStackTrace();
									}
								}
								else if(command.equals("Move(1)"))
								{
									try
									{
										p.setPlayerDirection("SOUTH");
										p.move(1);
									}
									catch (BoundaryException e) {
										e.printStackTrace();
									}
								}
								else if(command.equals("Move(2)"))
								{
									try
									{
										p.setPlayerDirection("EAST");
										p.move(2);
									}
									catch (BoundaryException e) {
										e.printStackTrace();
									}
								}
								else if(command.equals("Move(3)"))
								{
									try
									{
										p.setPlayerDirection("WEST");
										p.move(3);
									}
									catch (BoundaryException e) {
										e.printStackTrace();
									}
								}
								else if(command.equals("PlaceTower"))
								{
									Command c = (Command)obj;
									int x = c.getX();
									int y = c.getY();
									placeTowerImmediately(x, y);
									goldEarned--;
									teamGold.setText("Gold: " + goldEarned);
								}
								else if(command.equals("RotateTower"))
								{
									Command c = (Command)obj;
									int x = c.getX();
									int y = c.getY();
									
									//Tower t = currentPlayer.playerOperatingTower();
									if(backendBoard.getSpace(x, y) instanceof TowerSpace)
									{
										TowerSpace ts = (TowerSpace) backendBoard.getSpace(x, y);
										Tower t = ts.getTower();
										t.rotate();
										
										if(t instanceof BasicTower)
										{
											BufferedImage image = ((BasicTower) t).getTowerImages();
											Image icon = image.getScaledInstance(spaces[x][y].getWidth(), spaces[x][y].getHeight(), Image.SCALE_SMOOTH);
											spaces[x][y].setIcon(new ImageIcon(icon));
										}
									}								

								}
								else if(command.equals("Shoot"))
								{
									Command c = (Command)obj;
									int x = c.getX();
									int y = c.getY();
									
									if(backendBoard.getSpace(x, y) instanceof TowerSpace)
									{
										TowerSpace ts = (TowerSpace) backendBoard.getSpace(x, y);
										Tower t = ts.getTower();
										
										if(t instanceof BasicTower)
										{
											t.shoot();
										}
									}
								}
								else if(command.equals("Timer"))
								{
									System.out.println("got command timer in client");
									Command c = (Command)obj;
									int timer = c.getX();
									
									timerInt = timer;
									levelTimer.setText("" + timerInt);
									
								}
								else if(command.equals("Mine"))
								{
									Command c = (Command)obj;
									if(backendBoard.getSpace(c.getX(), c.getY()) instanceof MineableSpace)
									{
										MineableSpace m = (MineableSpace)(backendBoard.getSpace(c.getX(), c.getY()));
										int valueMined = m.mine();
										goldEarned = goldEarned + valueMined;
										teamGold.setText("Gold: " + goldEarned);
									}
								}
								else if(command.equals("BuyTower"))
								{
									goldEarned--;
									teamGold.setText("Gold: " + goldEarned);
								}
							}
						}
						sendMessageToClients(obj);
					}//end of else command object
					obj = ois.readObject();
				}//end of while	
			}catch(IOException ioe){
				System.out.println("IOE in chatthread.run: " + ioe.getMessage());
			} catch(ClassNotFoundException cnfe){
				System.out.println("CNFE in chatthread.run: " + cnfe.getMessage());	
			}//end of finally block
		}//end of run
	}//end of chathread
	
	public class ReadObject extends Thread{
		ReadObject(){
		}
		
		public synchronized void run(){
			try {
				obj = ois.readObject();
				
				while(obj != null){
					if(obj instanceof String){
						chat.append(((String)obj));
					}//end of if ob is String
					else if(obj instanceof Player)
					{						
						backendBoard.setPlayer((Player)obj);
						players.add((Player)obj);
						
					}
					else if(obj instanceof Integer){
						if((Integer)obj == -1){
							System.out.println("before blocking lines");
							startGame();
							System.out.println("not a blocking line");
						}
					}
					else if(obj instanceof Command)
					{
						Player player = ((Command)obj).getPlayer();
						
						String command = ((Command)obj).getCommand();
						
						for(int i=0; i<players.size(); i++){
							Player p = players.get(i);
							if(player.getPlayerName() == p.getPlayerName())
							{
								if(command.equals("Move(0)"))
								{
									try
									{
										p.setPlayerDirection("NORTH");
										p.move(0);

									}
									catch (BoundaryException e) {
										e.printStackTrace();
									}
								}
								else if(command.equals("Turn(0)"))
								{
									p.setPlayerDirection("NORTH");
								}
								else if(command.equals("Move(1)"))
								{
									try
									{
										p.setPlayerDirection("SOUTH");
										p.move(1);
									}
									catch (BoundaryException e) {
										e.printStackTrace();
									}
								}
								else if(command.equals("Turn(1)"))
								{
									p.setPlayerDirection("SOUTH");
								}
								else if(command.equals("Move(2)"))
								{
									try
									{
										p.setPlayerDirection("EAST");
										p.move(2);
									}
									catch (BoundaryException e) {
										e.printStackTrace();
									}
								}
								else if(command.equals("Turn(2)"))
								{
									p.setPlayerDirection("EAST");
								}
								else if(command.equals("Move(3)"))
								{
									try
									{
										p.setPlayerDirection("WEST");
										p.move(3);
									}
									catch (BoundaryException e) {
										e.printStackTrace();
									}
								}
								else if(command.equals("Turn(3)"))
								{
									p.setPlayerDirection("WEST");
								}
								else if(command.equals("PlaceTower"))
								{
									Command c = (Command)obj;
									int x = c.getX();
									int y = c.getY();
									//placeTower(x, y, false);
									placeTowerImmediately(x, y);
								}
								else if(command.equals("RotateTower"))
								{
									Command c = (Command)obj;
									int x = c.getX();
									int y = c.getY();
									
									//Tower t = currentPlayer.playerOperatingTower();
									if(backendBoard.getSpace(x, y) instanceof TowerSpace)
									{
										TowerSpace ts = (TowerSpace) backendBoard.getSpace(x, y);
										Tower t = ts.getTower();
										t.rotate();
										
										if(t instanceof BasicTower)
										{
											BufferedImage image = ((BasicTower) t).getTowerImages();
											Image icon = image.getScaledInstance(spaces[x][y].getWidth(), spaces[x][y].getHeight(), Image.SCALE_SMOOTH);
											spaces[x][y].setIcon(new ImageIcon(icon));
											
										}
										
									}								

								}
								else if(command.equals("Shoot"))
								{
									Command c = (Command)obj;
									int x = c.getX();
									int y = c.getY();
									
									if(backendBoard.getSpace(x, y) instanceof TowerSpace)
									{
										TowerSpace ts = (TowerSpace) backendBoard.getSpace(x, y);
										Tower t = ts.getTower();
										
										if(t instanceof BasicTower)
										{
											t.shoot();
										}
									}
								}
								else if(command.equals("Timer"))
								{
									Command c = (Command)obj;
									int timer = c.getX();
									
									timerInt = timer;
									levelTimer.setText("Creeps Remaining: " + timerInt);
									
								}
								else if(command.equals("Mine"))
								{
									Command c = (Command)obj;
									if(backendBoard.getSpace(c.getX(), c.getY()) instanceof MineableSpace)
									{
										MineableSpace m = (MineableSpace)(backendBoard.getSpace(c.getX(), c.getY()));
										int valueMined = m.mine();
										goldEarned = goldEarned + valueMined;
										teamGold.setText("Gold: " + goldEarned);
										
									}
								}
								else if(command.equals("BuyTower"))
								{
									
									goldEarned--;
									teamGold.setText("Gold: " + goldEarned);
								}
							}
						}
						
					}
					obj = ois.readObject();
				}//end of while	
			}catch(IOException ioe){
				System.out.println("IOE in readobject.run: " + ioe.getMessage());
			} catch(ClassNotFoundException cnfe){
				System.out.println("CNFE in IOE in  readobject.run: " + cnfe.getMessage());
			}
		}//end of run
	}//end of inner class read object
	class CreateConnections extends Thread{
		public CreateConnections(){
			createActions();
			if(!isHost){
				try {
					s = new Socket("localhost", 8970);
					oos = new ObjectOutputStream(s.getOutputStream());
					ois = new ObjectInputStream(s.getInputStream());
					new ReadObject().start();
				} catch (IOException ioe) {
					System.out.println("IOE client: " + ioe.getMessage());
				}
				isConnected = true;
			}//end of if not host
			try
			{
				if(oos != null)
				{
					oos.writeObject(currentPlayer);
					oos.flush();
				}
			} catch (IOException e1) {
				System.out.println("Exception sending player to server");
			}
		}//end of constructor
		public void run(){
			if(isHost){
				try {
					System.out.println("Starting Chat Server");
					ss = new ServerSocket(8970);
					while (true) {
						System.out.println("Waiting for client to connect...");
						Socket s = ss.accept();
						System.out.println("Client " + s.getInetAddress() + ":" + s.getPort() + " connected");
						ChatThread ct = new ChatThread(s);
						ctVector.add(ct);
						ct.start();
					}
				} catch (IOException ioe) {
					System.out.println("IOE: " + ioe.getMessage());
				} finally {
					if (ss != null) {
						try {
							ss.close();
						} catch (IOException ioe) {
							System.out.println("IOE closing ServerSocket: " + ioe.getMessage());
						}
					}
				}//end of finally
			}//end of if host
		}
	}
	
	public class ExplosionThread extends Thread{
		private int x, y;
		
		public ExplosionThread(int x, int y){
			this.x = x;
			this.y = y;
		}
		
		public void run(){
			spaces[x][y].setIcon(explosionImage);
			try {
				sleep(200);
				spaces[x][y].setIcon(null);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
	}
}//end of class
