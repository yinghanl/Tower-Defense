package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;

public class GameScreenGUI extends JFrame{

	private static final long serialVersionUID = 1L;
	private ImagePanel board;
	private JPanel chatBox;
	private JPanel optionsPanel;
	private JTextArea chat;
	private JTextField chatEdit;
	private JLabel[][] spaces;
	private JButton[] options;
	private JButton previous = new JButton("<-");
	private JButton next = new JButton("->");
	private JPanel buttonsPanel;
	private Timer timer;
	private JLabel levelTimer;
	private JLabel teamGold;
	private int timerInt = 60;
	private int goldEarned = 0;
	private String message;
	private Timer lvlTimer;
	
	private Board backendBoard;
	
	private int nextIndex = 0;
	private int previousIndex = 0;
	
	private Player currentPlayer;
	private boolean isHost;
	private boolean messageSent;
	
	private ServerSocket ss;
	private Socket s;
	private BufferedReader br;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	private Object obj;
	
	public GameScreenGUI(Board b, Player p, boolean isHost)
	{
		this.setSize(825,510);
		this.setLocation(0,0);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		this.setLayout(new BorderLayout());
		this.setResizable(false);

		messageSent = false;
		message = "";
		this.backendBoard = b;
		
		this.currentPlayer = p;
				
		this.isHost = isHost;
		
		board = this.createBoard();
		
		this.add(board, BorderLayout.CENTER);
		
		chatBox = this.getChatBox();
		
		this.add(chatBox, BorderLayout.EAST);
		
		this.createButtons(13);
		
		optionsPanel = this.getOptions();
				
		this.add(optionsPanel, BorderLayout.SOUTH);
		
		this.add(getTopPanel(), BorderLayout.NORTH);
		
		this.createActions();
		
		this.setVisible(true);
		
		Timer time = new Timer(100, new ActionListener()
		{
			public void actionPerformed(ActionEvent ae) {
				updateBoard();
				
			}
		});
		time.start();
		
		lvlTimer = new Timer(1000, new ActionListener()
		{
			public void actionPerformed(ActionEvent ae) {
				timerInt--;
				levelTimer.setText("" + timerInt);
			}
			
			
		});
		
		lvlTimer.start();
				
		
		if(isHost == true)
		{
			try {
				ss = new ServerSocket(6789);
				chat.append("\nWaiting for players to connect...");
				s = ss.accept();   //blocking line waits till accepted to proceed to next lines of code
				chat.append("\nConnection established!\n");
				br = new BufferedReader( new InputStreamReader(s.getInputStream()));
				ois = new ObjectInputStream(s.getInputStream());
				oos = new ObjectOutputStream(s.getOutputStream());
				oos.writeObject(new String("Connected to Game Room!"));
				oos.flush();
				new ReadObject().start();
			} catch (IOException e) {
				System.out.println("IOE in gamescreengui.constructor.setting up host: "+e.getMessage());
			}
		}
		else{
			try {
				s = new Socket("localhost",6789 );
				br = new BufferedReader(new InputStreamReader(s.getInputStream()));
				oos = new ObjectOutputStream(s.getOutputStream());
				ois = new ObjectInputStream(s.getInputStream());
				new ReadObject().start();
			} catch (UnknownHostException e) {
				System.out.println("unknownhost in gamescreengui.constructor.setting up client: "+e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("IOE in gamescreengui.constructor.setting up client: "+e.getMessage());
			}
			
		}//end else
		
		this.placeTower(0,1);
		
	}
	
	private JPanel getTopPanel()
	{
		JPanel toReturn = new JPanel();
		
		toReturn.setLayout(new BoxLayout(toReturn, BoxLayout.X_AXIS));
		
		levelTimer = new JLabel("" + timerInt);
		
		teamGold = new JLabel("Gold: " + goldEarned);
		
		toReturn.add(Box.createGlue());
		toReturn.add(Box.createGlue());

		
		toReturn.add(levelTimer);
		
		toReturn.add(Box.createGlue());
		
		toReturn.add(Box.createGlue());
		
		toReturn.add(teamGold);
		
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
		JScrollPane sp = new JScrollPane(chat);

		chatEdit = new JTextField();
		
		chat.setEditable(false);
		
		toReturn.add(sp, BorderLayout.CENTER);
		toReturn.add(chatEdit, BorderLayout.SOUTH);
				
		return toReturn;
	}
	
	private void createButtons(int k)
	{
		options = new JButton[k];
		
		for(int i = 0; i < k; i++)
		{
			options[i] = new JButton("" + i);
			options[i].setSize(10,10);
			options[i].setPreferredSize(options[i].getPreferredSize());
		}
	}
	
	private JPanel getOptions()
	{
		JPanel toReturn = new JPanel();
		toReturn.setSize(100,50);
		toReturn.setPreferredSize(toReturn.getSize());
		toReturn.setLayout(new BorderLayout());
		
		buttonsPanel = new JPanel();
		buttonsPanel.setSize(100,50);
		buttonsPanel.setPreferredSize(buttonsPanel.getSize());
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));

		
		toReturn.add(previous, BorderLayout.WEST);

		buttonsPanel.add(javax.swing.Box.createGlue());
		
		for(int i = 0; i < 5; i++)
		{
			buttonsPanel.add(options[i]);
			buttonsPanel.add(javax.swing.Box.createGlue());
		}

		toReturn.add(buttonsPanel, BorderLayout.CENTER);
		toReturn.add(next, BorderLayout.EAST);

		nextIndex = 5;
		previousIndex = 0;
		
		return toReturn;
	}
	
	private JPanel updateOptionsNext()
	{
		JPanel toReturn = new JPanel();
		
		toReturn.setSize(100,50);
		toReturn.setPreferredSize(toReturn.getSize());
		toReturn.setLayout(new BoxLayout(toReturn, BoxLayout.X_AXIS));

		toReturn.add(javax.swing.Box.createGlue());
		int counter = 0;
		
		for(int i = nextIndex; i < nextIndex + 5; i++)
		{
			if(i < options.length)
			{
				toReturn.add(options[i]);
				counter++;
			}
			toReturn.add(javax.swing.Box.createGlue());
		}
		
		nextIndex = nextIndex + counter;
		previousIndex = previousIndex + 5;
				
		return toReturn;
	}
	
	private JPanel updateOptionsPrevious()
	{
		JPanel toReturn = new JPanel();
		
		toReturn.setSize(100,50);
		toReturn.setPreferredSize(toReturn.getSize());
		toReturn.setLayout(new BoxLayout(toReturn, BoxLayout.X_AXIS));

		toReturn.add(javax.swing.Box.createGlue());
		int counter = 0;

		for(int i = previousIndex-5; i < previousIndex; i++)
		{
			if(i < options.length)
			{
				toReturn.add(options[i]);
				counter++;
			}
			toReturn.add(javax.swing.Box.createGlue());
		}
		
		previousIndex = previousIndex - 5;
		nextIndex = previousIndex + 5;
		
		return toReturn;
	}
	
	private void createActions()
	{
		next.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae) {
				
				if(nextIndex != options.length)
				{
					JPanel newOptionsPanel = updateOptionsNext();
					buttonsPanel.setVisible(false);
					optionsPanel.remove(buttonsPanel);	
					optionsPanel.add(newOptionsPanel, BorderLayout.CENTER);
					buttonsPanel = newOptionsPanel;
				}
			}
			
		});
		
		previous.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				if(previousIndex != 0)
				{
					JPanel newOptionsPanel = updateOptionsPrevious();
					buttonsPanel.setVisible(false);
					optionsPanel.remove(buttonsPanel);	
					optionsPanel.add(newOptionsPanel, BorderLayout.CENTER);
					buttonsPanel = newOptionsPanel;
				}
			}
		});
		
		board.addKeyListener(new KeyAdapter()
		{
			public void keyPressed(KeyEvent ke) {

				int key = ke.getKeyCode();
				System.out.println(key);
				int playerx = currentPlayer.getLocation().getX();
				int playery = currentPlayer.getLocation().getY();
				
				System.out.println(playerx + " " + playery);
				
				if(key == ke.VK_UP)
				{
					try {
						currentPlayer.move(0);
					} catch (BoundaryException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if(key == ke.VK_DOWN)
				{
					try {
						currentPlayer.move(1);
					} catch (BoundaryException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				else if(key == ke.VK_RIGHT)
				{
					try {
						currentPlayer.move(2);
					} 
					catch (BoundaryException e) {
						e.printStackTrace();
					}
				}
				else if(key == ke.VK_LEFT)
				{
					try {
						currentPlayer.move(3);
					} catch (BoundaryException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if(key == ke.VK_1)
				{
					if(currentPlayer.getPlayerDirection() == "SOUTH")
					{
						if(playerx+1 < 20)
						{
							placeTower(playerx+1, playery);
						}
					}
					else if(currentPlayer.getPlayerDirection() == "NORTH")
					{
						if(playerx-1 > 0)
						{
							placeTower(playerx-1, playery);
						}
					}
					
					else if(currentPlayer.getPlayerDirection() == "WEST")
					{
						if(playery-1 > 0)
						{
							placeTower(playerx, playery-1);
						}
					}
					else if(currentPlayer.getPlayerDirection() == "EAST")
					{
						if(playery+1 < 32)
						{
							placeTower(playerx, playery+1);
						}
					}
				}
			}
		});
		chatEdit.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent ke) {
			
				int key = ke.getKeyCode();
				
				if(key == ke.VK_ENTER && chatEdit.getText() != null){
					messageSent = true;
					System.out.println("setting messageSent to true");
					String toAppend = currentPlayer.getPlayerName() + ": " + chatEdit.getText() + "\n";
					message = toAppend;
					chat.setText(chat.getText() + toAppend);
					chatEdit.setText(null);
					try {
						oos.writeObject(message);
						oos.flush();
					} catch (IOException e) {
						System.out.println("IOE when trying to write string object");
					}
					
				}
				
			}

		});
	}
	
	
	public void updateBoard()
	{
		for(int i = 0; i < 20; i++)
		{
			for(int j = 0; j < 32; j++)
			{
				if(backendBoard.getSpace(i, j).isOccupied())
				{
					if(backendBoard.getSpace(i, j).getMoveable() instanceof Player)
					{
						spaces[i][j].setBorder(BorderFactory.createLineBorder(Color.yellow));
					}
					else
					{
						ImageIcon icon = new ImageIcon(backendBoard.getSpace(i,j).getMoveable().getMoveableImage());
					
						spaces[i][j].setIcon(icon);
					}

					
				}
			}
		}
	}
	
	public void placeTower(int x, int y)
	{
		BasicTower b = new BasicTower();
		BufferedImage img[] = b.getTowerImages();
		int count = 0;
		
		for(int i = 0; i < 2; i++)
		{
			for(int j = 0; j < 2; j++)
			{				
				Image resizedImage = img[count].getScaledInstance(spaces[i][j].getWidth(), spaces[i][j].getHeight(), Image.SCALE_SMOOTH);
				
				spaces[x+i][y+j].setIcon(new ImageIcon(resizedImage));
				
				count++;
			}
		}
	}
	
	public void restartLevelTimer()
	{
		

		
	}

	public class ReadObject extends Thread{
		ReadObject(){
		}
		
		public synchronized void run(){
			try {
				obj = ois.readObject();
				while(obj != null){
					System.out.println("ob not null in client: "+obj.getClass());
					if(obj instanceof String){
						System.out.println("got string: "+(String)obj);
						chat.append("\n"+((String)obj));
					}//end of if ob is String
					obj = ois.readObject();
				}//end of while	
			}catch(IOException ioe){
				System.out.println("IOE in chatserver constructor: " + ioe.getMessage());
			} catch(ClassNotFoundException cnfe){
				System.out.println("CNFE in chatserver constructor: " + cnfe.getMessage());
			}
		}//end of run
	}//end of inner class read object
	
}//end of class
