package main;
//To make this work on your machine:
//Run the CreateDatabaseAndTable script on your machine
//replace the string on line 19 with the string you should connect to
//It should start with "jdbc:mysql://localhost/towerdefense?user=YourUsername"  (most likely root)
//If you have a password, append "&password=YourPassword" to the end of the string
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;


public class DataBaseUtils {

	private static String getConnectionString()
	{
		return("jdbc:mysql://localhost/towerdefense?user=root&password=YOURPASSWORD");
	}
	
	public static boolean verifyUser(String username, char [] password)
	//Checks if the username and password is in the database
	//returns true is user exists
	//returns false if not
	{
		String pass = cArrayToString(password);
		boolean userExists = false;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection(getConnectionString());
			Statement st = conn.createStatement();
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM User WHERE username=? AND password=?");
			ps.setString(1, username); // set first variable in prepared statement
			ps.setString(2, pass);
			ResultSet rs = ps.executeQuery();
			if(rs.next())
			{
				userExists = true;
			}
			else
			{
				userExists = false;
			}
			rs.close();
			st.close();
			conn.close();
		} catch (SQLException sqle) {
			System.out.println ("SQLException: " + sqle.getMessage());
		} catch (ClassNotFoundException cnfe) {
			System.out.println ("ClassNotFoundException: " + cnfe.getMessage());
		}
		return userExists;
	}
	
	public static int getUserID(String username)
	//returns the userID associated with a username
	//returns -1 if the username is not in the database
	{
		int userID = -1;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection(getConnectionString());
			Statement st = conn.createStatement();
			PreparedStatement ps = conn.prepareStatement("SELECT userID FROM User WHERE username=?");
			ps.setString(1, username); // set first variable in prepared statement
			ResultSet rs = ps.executeQuery();
			if(rs.next())
			{
				userID = rs.getInt("userID");
			}
			else
			{
				userID = -1;
			}
			rs.close();
			st.close();
			conn.close();
		} catch (SQLException sqle) {
			System.out.println ("SQLException: " + sqle.getMessage());
		} catch (ClassNotFoundException cnfe) {
			System.out.println ("ClassNotFoundException: " + cnfe.getMessage());
		}
		return userID;
	}
	
	public static int createNewUser(String username, char [] password)
	{
		int userID = -1;
		String pass = cArrayToString(password);
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection(getConnectionString());
			Statement st = conn.createStatement();
			PreparedStatement ps = conn.prepareStatement("INSERT INTO user (username, password) VALUES(?, ?) ");
			ps.setString(1, username); // set first variable in prepared statement
			ps.setString(2, pass);
			ps.executeUpdate();
			PreparedStatement getIDps = conn.prepareStatement("SELECT LAST_INSERT_ID()");
			ResultSet rs = getIDps.executeQuery();
			if(rs.next())
			{
				userID = rs.getInt("LAST_INSERT_ID()");
			}
			else
			{
				userID = -1;
			}
			rs.close();
			st.close();
			conn.close();
		} catch (SQLException sqle) {
			System.out.println ("SQLException: " + sqle.getMessage());
		} catch (ClassNotFoundException cnfe) {
			System.out.println ("ClassNotFoundException: " + cnfe.getMessage());
		}
		return userID;
	}
	
	public static boolean changePassword(int userID, char [] password)
	// Takes in a userID and a password,
	// Changes the value of the password in the database
	// Returns true if it worked, returned false if the userID wasn't found
	{
		String pass = cArrayToString(password);
		boolean userExists = false;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection(getConnectionString());
			Statement st = conn.createStatement();
			PreparedStatement ps = conn.prepareStatement("UPDATE user SET password=? WHERE userID=?");
			ps.setString(1, pass);
			ps.setInt(2, userID); // set first variable in prepared statement
			int rowsAffected = ps.executeUpdate();
			
			if(rowsAffected > 0)
			{
				userExists = true;
			}
			
			st.close();
			conn.close();
		} catch (SQLException sqle) {
			System.out.println ("SQLException: " + sqle.getMessage());
		} catch (ClassNotFoundException cnfe) {
			System.out.println ("ClassNotFoundException: " + cnfe.getMessage());
		}
		return userExists;
	}
	
	public static User createUser(int userID)
	//Creates user from userID
	//returns null if userID is not in the database
	{
		User newUser = null;
		
		String username;
		int creepsKilled;
		int goldEarned;
		int gamesPlayed;
		int gamesWon;
		int gamesLost;
		int [] userData = new int[5];
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection(getConnectionString());
			Statement st = conn.createStatement();
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM User WHERE userID=?");
			ps.setInt(1, userID); // set first variable in prepared statement
			ResultSet rs = ps.executeQuery();
			if(rs.next())
			{
				username = rs.getString("username");
				creepsKilled = rs.getInt("creeps_killed");
				goldEarned = rs.getInt("gold_earned");
				gamesPlayed = rs.getInt("games_played");
				gamesWon = rs.getInt("games_won");
				gamesLost = rs.getInt("game_lost");
				
			}
			else
			{
				userID = -1;
				return null;
			}
			userData[0] = creepsKilled;
			userData[1] = goldEarned;
			userData[2] = gamesPlayed;
			userData[3] = gamesWon;
			userData[4] = gamesLost;
			newUser = new User(userID, username, userData);
			rs.close();
			st.close();
			conn.close();
		} catch (SQLException sqle) {
			System.out.println ("SQLException: " + sqle.getMessage());
		} catch (ClassNotFoundException cnfe) {
			System.out.println ("ClassNotFoundException: " + cnfe.getMessage());
		}
		
		return newUser;
	}
	
	public static Guest createGuest()
	{
		int guestID = createNewUser("N/A", "N/A".toCharArray());
		Guest newGuest = new Guest(guestID);
		return newGuest;
	}

	public static boolean deleteGuest(Guest g)
	{
		int guestID = g.getUserID();
		boolean deleted = false;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection(getConnectionString());
			Statement st = conn.createStatement();
			PreparedStatement ps = conn.prepareStatement("DELETE FROM user WHERE userID=?");
			ps.setInt(1, guestID);
			int rowsAffected = ps.executeUpdate();
			
			if(rowsAffected > 0)
			{
				deleted = true;
			}
			
			st.close();
			conn.close();
		} catch (SQLException sqle) {
			System.out.println ("SQLException: " + sqle.getMessage());
		} catch (ClassNotFoundException cnfe) {
			System.out.println ("ClassNotFoundException: " + cnfe.getMessage());
		}
		
		return deleted;
	}
	
	public static void updatePlayerStats(Vector<GameStats> gsv)
	{ 
		for(int i = 0; i < gsv.size(); i++)
		{
			GameStats gs = gsv.get(i);
			AbstractUser tempUser = gs.getAbstractUser();
			if(tempUser instanceof User)
			{
				int [] userData = ((User)tempUser).getUserData();
				if(gs.getGameResult())
				{
					userData[3]++;
				}
				else
				{
					userData[4]++;
				}
				try {
					Class.forName("com.mysql.jdbc.Driver");
					Connection conn = DriverManager.getConnection(getConnectionString());
					Statement st = conn.createStatement();
					PreparedStatement ps = conn.prepareStatement("UPDATE user SET gold_earned=? "
							+ "AND creeps_killed=?, gold_earned=?, "
							+ "games_played=?, games_won=?, games_lost=?"
							+ "WHERE userID=?");
					ps.setInt(1, userData[0] + gs.getGold());
					ps.setInt(2, userData[1] + gs.getCreepsKilled());
					ps.setInt(3, userData[2]+1);
					ps.setInt(4, userData[3]);
					ps.setInt(5, userData[4]);
					ps.setInt(6, tempUser.getUserID());
					ps.executeUpdate();
					
					st.close();
					conn.close();
				} catch (SQLException sqle) {
					System.out.println ("SQLException: " + sqle.getMessage());
				} catch (ClassNotFoundException cnfe) {
					System.out.println ("ClassNotFoundException: " + cnfe.getMessage());
				}
			}
		}
	}
	
	private static String cArrayToString(char [] cArray)
	{
		String returnValue = new String(cArray);
		return returnValue;
	}
	
}
