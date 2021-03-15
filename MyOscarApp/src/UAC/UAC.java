package UAC;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * <p>Handles user control for storing information</p>
 *
 * @author Eric Rodriguez
 * @version 1.0
 */
public class UAC
{
	/**
	 * <p>Holds the user data for the app. Info is held like this:</p>
	 * <p>> Username (unencrypted)</p>
	 * <p>> Verification line (encrypted)</p>
	 * <p>> Movies watched, square bracket and comma delimited: [a], [b], ..., [z] (encrypted)</p>
	 * <p>> User preferences, square bracket and comma delimited: [a], [b], ..., [z] (encrypted)</p>
	 */
	private final File USERS_FILE = new File("users.dat");
	
	private Sentinel sentinel;
	
	private String username;
	
	private int userLine = -1;
	
	/**
	 * <p>Verifies the user file to be used. Program should call {@link #createNewSession()} or
	 * {@link #createNewUser()}
	 * afterwards.</p>
	 */
	public UAC()
	{
		if (!verifyFile(USERS_FILE))
		{
			System.out.println("There was an error verifying the user file " + USERS_FILE.getName());
		}
	}
	
	/**
	 * <p>Verifies if a file exists. If it does not exist, the program will attempt to create it.</p>
	 *
	 * @param file The file to verify.
	 * @return Returns true if the file exists or wax created.
	 */
	private boolean verifyFile(File file)
	{
		try
		{
			// Creates the file if it does not exist
			new FileOutputStream(file, true);
			return true;
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * <p>Finds a specified username in the user file.</p>
	 *
	 * @param username The username to look for.
	 * @return The line where the username is. Returns -1 if not found.
	 */
	private int findUserLine(String username)
	{
		try
		{
			FileReader reader = new FileReader(USERS_FILE);
			BufferedReader bufferedReader = new BufferedReader(reader);
			String readLine = bufferedReader.readLine();
			int line = 0;
			
			while (readLine != null)
			{
				if (readLine.equals("user:" + username))
				{
					bufferedReader.close();
					reader.close();
					return line;
				}
				readLine = bufferedReader.readLine();
				line++;
			}
			
			bufferedReader.close();
			reader.close();
			return -1;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return -1;
		}
	}
	
	/**
	 * <p>Create a new user session to allow multiple calls to the user file without having to log in again.</p>
	 */
	public void createNewSession()
	{
		System.out.println("Logging in...");
		
		Scanner scanner = new Scanner(System.in);
		String password = "";
		username = "";
		userLine = -1;
		
		// Get username
		while (userLine == -1)
		{
			System.out.print("Enter username: (type ':q' to quit) \n> ");
			username = scanner.nextLine().trim();
			
			if (username.equals(":q"))
			{
				return;
			}
			
			// Check if user exists
			if ((userLine = findUserLine(username)) == -1)
			{
				System.out.println("User doesn't exist");
			}
		}
		
		// Get password
		while (password.equals(""))
		{
			System.out.print("Enter password: (type ':q' to quit) \n> ");
			password = scanner.nextLine().trim();
			
			if (password.equals(":q"))
			{
				return;
			}
			
			// Change password to a long value that will be used for the seed
			long seed = 0;
			
			for (char temp : password.toCharArray())
			{
				seed += temp;
			}
			
			seed = (int) Math.pow(seed, password.length()) - seed * password.length();
			sentinel = new Sentinel(seed);
			String verification = readLine(userLine + 1);
			
			// Check the verification to see if the password is correct
			if (verification.equals("user:" + username))
			{
				System.out.println("Successfully logged in");
			}
			else
			{
				System.out.println("Incorrect password");
				sentinel = null;
				password = "";
			}
		}
	}
	
	/**
	 * <p>Creates a new user and initializes a session.</p>
	 */
	public void createNewUser()
	{
		System.out.println("Creating new user...");
		
		Scanner scanner = new Scanner(System.in);
		String password = "";
		username = "";
		
		// Get a username
		while (username.equals(""))
		{
			System.out.print("Enter username: (type ':q' to quit) \n> ");
			username = scanner.nextLine().trim();
			
			if (username.equals(":q"))
			{
				return;
			}
			
			if (username.contains(" "))
			{
				System.out.println("Username can't contain spaces");
				username = "";
			}
			else if (username.length() < 6)
			{
				System.out.println("Username must be at least 6 characters");
				username = "";
			}
		}
		
		// Check if user already exists
		if (findUserLine(username) != -1)
		{
			System.out.println("User already exists");
			return;
		}
		
		// Get a password
		while (password.equals(""))
		{
			System.out.print("Enter password: (type ':q' to quit) \n> ");
			password = scanner.nextLine().trim();
			
			if (password.equals(":q"))
			{
				return;
			}
			
			if (password.contains(" "))
			{
				System.out.println("Password can't contain spaces");
				password = "";
			}
			else if (password.length() < 8)
			{
				System.out.println("Password must be at least 8 characters");
				password = "";
			}
		}
		
		// Change password to a long value that will be used for the seed
		long seed = 0;
		
		for (char temp : password.toCharArray())
		{
			seed += temp;
		}
		
		seed = (int) Math.pow(seed, password.length()) - seed * password.length();
		sentinel = new Sentinel(seed);
		
		String verification;
		String usernameLine;
		String watchedMovies = "[]";
		String userPref = "[]";
		String nl = "\r\n";
		String end = "endUser";
		
		// Encrypt needed lines
		usernameLine = "user:" + username;
		verification = sentinel.encrypt(usernameLine);
		watchedMovies = sentinel.encrypt(watchedMovies);
		userPref = sentinel.encrypt(userPref);
		
		// The line to be written to the file
		String outLine = usernameLine + nl + verification + nl + watchedMovies + nl + userPref + nl + end + nl;
		
		try
		{
			// Write the new user into the file
			Files.write(Paths.get(USERS_FILE.getName()), outLine.getBytes(), StandardOpenOption.APPEND);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * <p>Gets the movies from the user profile.</p>
	 *
	 * @return String array of movies.
	 */
	public ArrayList<String> getMovies()
	{
		return getData(userLine + 2);
	}
	
	/**
	 * <p>Gets the preferences from the user profile.</p>
	 *
	 * @return String array of user preferences.
	 */
	public ArrayList<String> getPreferences()
	{
		return getData(userLine + 3);
	}
	
	/**
	 * <p>Gets the data from the user file and parses the data into a string array</p>
	 *
	 * @param line The line to read
	 * @return A string array of data
	 */
	private ArrayList<String> getData(int line)
	{
		// Makes sure sentinel has been created
		if (sentinel == null)
		{
			System.out.println("Sentinel has not been initialized. Please start a new session.");
			return new ArrayList<>();
		}
		
		String lineData = readLine(line);
		ArrayList<String> list = new ArrayList<>();
		int index = 0;
		
		// Parses the data into an array list
		while (index < lineData.length() - 1)
		{
			int openBracket = lineData.indexOf("[", index);
			int closeBracket = lineData.indexOf("]", index);
			
			// Sees if data is missing or corrupt
			if (openBracket == -1 || closeBracket == -1)
			{
				System.out.println("Data seems to be incomplete.");
				break;
			}
			
			list.add(lineData.substring(openBracket + 1, closeBracket).trim());
			index = closeBracket + 1;
		}
		
		return list;
	}
	
	public void writeMovies(ArrayList<String> data)
	{
		writeData(data, userLine + 2);
	}
	
	public void writePreferences(ArrayList<String> data)
	{
		writeData(data, userLine + 3);
	}
	
	private void writeData(ArrayList<String> data, int line)
	{
		for (int i = 0; i < data.size(); i++)
		{
			if (data.get(i).trim().equals(""))
			{
				data.remove(i);
				i--;
			}
		}
		
		if (data.isEmpty())
		{
			System.out.println("The data is empty");
			return;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(data.get(0).trim()).append("]");
		
		for (int i = 1; i < data.size(); i++)
		{
			sb.append(", [").append(data.get(i).trim()).append("]");
		}
		
		writeToFile(sb.toString(), line);
	}
	
	/**
	 * <p>Reads a specific line from the user file and decrypts it.</p>
	 *
	 * @param line The specified line to read.
	 * @return The decrypted line.
	 */
	private String readLine(int line)
	{
		// Makes sure sentinel has been created
		if (sentinel == null)
		{
			System.out.println("Sentinel has not been initialized. Please start a new session.");
			return "";
		}
		
		try
		{
			FileReader reader = new FileReader(USERS_FILE);
			BufferedReader bufferedReader = new BufferedReader(reader);
			String out;
			
			// Use readLine() to skip to the part where we actually need to read
			for (int i = 0; i < line; i++)
			{
				bufferedReader.readLine();
			}
			
			// Read the line ad decrypt it 
			out = sentinel.decrypt(bufferedReader.readLine().trim());
			bufferedReader.close();
			reader.close();
			return out;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return "";
		}
	}
	
	/**
	 * <p>Writes a a string into the user file. The line will get replaced, and as such cannot be appended.</p>
	 * <p>This method can theoretically write multiple lines instead of replacing one line by using {@code "\n"} but
	 * this is not recommended.</p>
	 *
	 * @param output The string to write into the file.
	 * @param line   The line to write to.
	 */
	private void writeToFile(String output, int line)
	{
		// Makes sure sentinel has been created
		if (sentinel == null)
		{
			System.out.println("Sentinel has not been initialized. Please start a new session.");
		}
		
		try
		{
			FileReader reader = new FileReader(USERS_FILE);
			BufferedReader bufferedReader = new BufferedReader(reader);
			StringBuilder present = new StringBuilder();
			String end;
			
			// Record the data already present into StringBuilder
			for (int i = 0; i < line; i++)
			{
				present.append(bufferedReader.readLine()).append("\n");
			}
			
			// Add the new output and skip reading the line from the user file
			present.append(sentinel.encrypt(output)).append("\n");
			bufferedReader.readLine();
			
			// Add the remaining lines
			while ((end = bufferedReader.readLine()) != null)
			{
				present.append(end).append("\n");
			}
			
			bufferedReader.close();
			reader.close();
			
			// Write the file
			FileOutputStream out = new FileOutputStream(USERS_FILE);
			out.write(present.toString().getBytes());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
