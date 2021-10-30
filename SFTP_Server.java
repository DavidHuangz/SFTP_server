import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.Date;
import java.util.Scanner;

class SFTP_Server {
	// Change to alter settings
	private static final Boolean supportsGenerations = true;
	private static String currentDir = "C:/";

	// Other variables
	private static int user;
	private static int acct;
	private static int pass;
	private static boolean changeDir = false;
	private static String renamedFile = null;
	private static boolean serverStatus = true;
	private static String sendFile = null;
	private static OutputStream outputStream;
	static int fileType = 1;
	private static int systemType = 0;
	private static String systemName = null;

	public static void main(String[] argv) throws Exception {
		String clientSentence;
		String userCommand;
		String userAnswer;
		String serverAnswer;
		long fileSize;
		long size = 0;

		ServerSocket welcomeSocket = new ServerSocket(6789);

		while (serverStatus) {
			Socket connectionSocket = welcomeSocket.accept();

			BufferedReader inFromClient =
					new BufferedReader(new
							InputStreamReader(connectionSocket.getInputStream()));

			DataOutputStream outToClient =
					new DataOutputStream(connectionSocket.getOutputStream());

			outputStream = connectionSocket.getOutputStream();

			clientSentence = inFromClient.readLine();

			// Get 4 characters
			if (clientSentence.length() > 4) {
				userCommand = clientSentence.substring(0, 4);
				userAnswer = clientSentence.substring(5);

				serverAnswer = switch (userCommand) {
					case "USER", "ACCT", "PASS" -> userAuth(userCommand, userAnswer);
					case "TYPE" -> typeCommand(userAnswer);
					case "LIST" -> listCommand(userAnswer);
					case "CDIR" -> cdirCommand(userAnswer);
					case "KILL" -> killCommand(userAnswer);
					case "NAME" -> nameCommand(userAnswer);
					case "RETR" -> retrCommand(userAnswer);
					case "STOR" -> storCommand(userAnswer);
					case "TOBE" -> tobeCommand(userAnswer);
					default -> "-Invalid Command";
				};


				if (clientSentence.contains("SIZE")) {
					if (!isLoggedIn()) {
						outToClient.writeBytes("-Please login" + "\0");
					}

					if (systemType == 0) {
						outToClient.writeBytes("-Please specify filename and store type" + "\0");
					}
					try { // Get the size
						size = Long.parseLong(userAnswer);
					} catch (Exception e) {
						// Any errors + "\0"
						systemName = null;
						outToClient.writeBytes(("-Invalid parameters") + "\0");
					}

					// Check free space
					File file = new File(currentDir);
					if (size > file.getFreeSpace()) {
						systemType = 0;
						systemName = null;
						outToClient.writeBytes("-Not enough room, don’t send it" + "\0");
					} else {
						fileSize = size;
							outToClient.writeBytes("+ok, waiting for file" + "\0");

						// Receiving file from client and writing into a new file in directory
						if (fileSize != 0) {

							// Processing the byte array
							byte[] fileFromClient = new byte[(int) fileSize];
							for (int i = 0; i < fileSize; i++) {
								fileFromClient[i] = (byte) connectionSocket.getInputStream().read();
							}
							try {
								// Writing byte array into the file
								if ((systemType == 1) || (systemType == 3)) {
									FileOutputStream stream = new FileOutputStream(currentDir + "/" + systemName);
									stream.write(fileFromClient);
									stream.close();
								} else if (systemType == 2) {
									FileOutputStream stream = new FileOutputStream(currentDir + "/" + "new_" + systemName);
									stream.write(fileFromClient);
									stream.close();
								} else {
									FileOutputStream stream = new FileOutputStream(currentDir + "/" + systemName, true);
									stream.write(fileFromClient);
									stream.close();
								}
							} catch (Exception e) {
								systemName = null;
								systemType = 0;
								outToClient.writeBytes("-Couldn’t save\0");
							}
							// Displaying saved file message
							outToClient.writeBytes("+Saved " + systemName + "\0");
							systemName = null;
							systemType = 0;
						}
					}

				}

			} else if (clientSentence.equals("DONE")) {
				serverStatus = false;
				serverAnswer =  "+Server has shut down";
			} else if (clientSentence.equals("SEND")) {
				serverAnswer = sendCommand();
			} else if (clientSentence.equals("STOP")) {
				serverAnswer = stopCommand();
			} else {
				serverAnswer = "-Invalid Command";
			}

			outToClient.writeBytes(serverAnswer + "\n\0");

		}
	}

	private static String storCommand(String userAnswer) {
		String filePath;
		String system;

		if (!isLoggedIn()) {
			return "-Please login";
		}

		try {
			system = userAnswer.substring(0, 3);
			filePath = userAnswer.substring(4);
		} catch (Exception e) {
			return "-Invalid parameters";
		}
		File path = new File(currentDir + "/" + filePath);

		switch (system) {
			case "NEW":
				if (path.exists()) {
					if (supportsGenerations) {
						systemName = filePath;
						systemType = 2;
						return "+File exists, will create new generation of file";
					} else {
						systemType = 0;
						return "-File exists, but system doesn’t support generations";
					}
				} else {
					systemType = 1;
					systemName = filePath;
					return "+File does not exist, will create new file";
				}

			case "OLD": // OLD system
				systemName = filePath;
				if (path.exists()) {
					systemType = 3;
					return "+Will write over old file";
				} else {
					systemType = 1;
					return "+Will create new file";
				}
			case "APP": // APP system
				systemName = filePath;
				if (path.exists()) {
					systemType = 4;
					return "+Will append to file";
				} else {
					systemType = 1;
					return "+Will create file";
				}
			default:
				return "-Invalid parameters";
		}
	}

	public static String tobeCommand(String userAnswer) {
		if (isLoggedIn()) {
			if (renamedFile == null) {
				return "-File wasn’t renamed because NAME not specified";
			}
			File path = new File(currentDir + "/" + renamedFile);
			File newPath = new File(currentDir + "/" + userAnswer);
			if (path.renameTo(newPath)) {
				return "+" + renamedFile + " renamed to " + userAnswer;
			}
			return "-File wasn't renamed due to an unknown error";
		} else {
			return "-Please log in to rename file";
		}
	}

	private static String retrCommand(String userAnswer) {
		if (isLoggedIn()) {
			File path = new File(currentDir + "/" + userAnswer); // Local files only. Absolute paths don't work
			if (path.exists()) { // Check if file exists
				sendFile = userAnswer;
				return Integer.toString((int) path.length());
			} else {
				return "-File doesn't exist";
			}
		} else {
			return "-Please log in";
		}
	}

	private static String stopCommand() {
		sendFile = null;
		return "+ok, RETR aborted";
	}

	private static String sendCommand() {
		if (sendFile != null) {
			File path = new File(currentDir + "/" + sendFile);
			try {
				byte[] fileContent = Files.readAllBytes(path.toPath());
				outputStream.write(fileContent); // sending to outputStream to client
			} catch (IOException e) {
				e.printStackTrace();
			}
			return "";
		} else {
			return "-No File Specified";
		}
	}

	private static String nameCommand(String userAnswer) {
		if (isLoggedIn()) {
			File path = new File(currentDir + "/" + userAnswer);
			if (path.exists()) {
				renamedFile = userAnswer;
				return "+File exists";
			} else {
				renamedFile = null;
				return "-Can't find " + userAnswer;
			}
		} else {
			return "-Cannot rename file because user is not logged-in";
		}
	}

	private static String killCommand(String userAnswer) {
		if (isLoggedIn()) {
			File path = new File(currentDir + "/" + userAnswer);
			if (path.exists()) {
				if (path.delete()) {
					return "+" + userAnswer + " deleted";
				} else {
					return "-Not deleted because of an unresolved reason ";
				}
			} else {
				return "-Not deleted because file does not exist";
			}
		} else {
			return "-Not deleted because user is not logged in";
		}
	}

	private static String cdirCommand(String userAnswer) {
		if (listDir(userAnswer, false).equals("-Directory path doesn't exist")) {
			return "-Can't connect to directory because: Directory doesn't exist";
		} else {
			changeDir = true;
			if (isLoggedIn()) {
				currentDir = userAnswer;
				return "!Changed working dir to " + userAnswer;
			} else if (user != 0) {
				return "+directory ok, send account/password";
			}
		}
		return "-Can't connect to directory because: user is not found";
	}

	private static String userAuth(String userCommand, String userAnswer) throws FileNotFoundException {
		String line;
		Scanner input = new Scanner("LoginData.txt");
		File file = new File(input.nextLine());
		input = new Scanner(file);
		int index = 0;

		while (input.hasNextLine()) {
			index++;
			line = input.nextLine();
			String[] wordArr = line.split(" ");

			// User ID
			if (userCommand.equals("USER") && userAnswer.equals(wordArr[0])) {
				user = index;
				return "+" + userAnswer + " valid, send account and password";
			}

			if (user != 0) {
				// Account
				if (userCommand.equals("ACCT") && userAnswer.equals(wordArr[1])) {
					acct = index;
					if (pass == user && acct == user) {
						if (changeDir) {
							return "!Changed working dir to " + currentDir;
						} else {
							return "! Logged in";
						}
					} else if (acct == user) {
						if (changeDir) {
							return  "+account ok, send password";
						} else {
							return "+Account valid, send password";
						}
					}
				}

				// Password
				if (userCommand.equals("PASS") && userAnswer.equals(wordArr[2])) {
					pass = index;
					if (acct == user && pass == user) {
						if (changeDir) {
							return "!Changed working dir to " + currentDir;
						} else {
							return "! Account valid, logged-in";
						}
					} else if (pass == user) {
						if (changeDir) {
							return "+password ok, send account";
						} else {
							return "+Send account";
						}
					}
				}
			}
		}

		if (userCommand.equals("USER")) {
			// Reset credentials when user entering another username
			user = 0;
			acct = user;
			pass = user;
			return "-Invalid user-id, try again";
		} else if (userCommand.equals("ACCT")) {
			if (changeDir) {
				if (user != 0) {
					return "-invalid account";
				} else {
					return "-Invalid Command";
				}
			} else {
				if (user != 0) {
					return "-Invalid account, try again";
				} else {
					return "-Invalid Command";
				}
			}
			// Password
		} else {
			if (changeDir) {
				if (user != 0) {
					return "-invalid password";
				} else {
					return "-Invalid Command";
				}
			} else {
				if (user != 0) {
					return "-Wrong password, try again";
				} else {
					return "-Invalid Command";
				}
			}
		}
	}

	private static Boolean isLoggedIn() {
		return user != 0 && user == acct && acct == pass;
	}

	private static String typeCommand(String userAnswer) {
		if (isLoggedIn()) {
			switch (userAnswer) {
				case "A":
					fileType = 1;
					return "+Using Ascii mode";
				case "B":
					fileType = 2;
					return "+Using Binary mode";
				case "C":
					fileType = 3;
					return "+Using Continuous mode";
				default:
					return "-Type not valid";
			}
		}
		return "-Please Login";
	}

	private static String listCommand(String userAnswer) {
		String command = userAnswer;

		if (isLoggedIn()) {
			if (userAnswer.contains(" ")) {
				String[] userInput = userAnswer.split(" ");
				command = userInput[0];
				if (userInput.length > 1) {
					currentDir = userInput[1];
				}
			}

			return switch (command) {
				case "F" -> listDir(currentDir, false);
				case "V" -> listDir(currentDir, true);
				default -> "-Format not valid";
			};

		}
		return "-Please Login";
	}

	private static String listDir(String currentDir, boolean verbose) {
		// Display directory information
		StringBuilder dirOutput = new StringBuilder();
		File path = new File(currentDir);

		try {
			File[] files = path.listFiles(); // listing all files
			dirOutput.append("+").append(currentDir).append("\r\n"); // get directory name
			assert files != null;
			for (File file : files) {
				if (verbose) {
					dirOutput.append(file.getName()).append("\tSize:").append(file.length()).append("\tisHidden:").append(file.isHidden()) // get file information
							.append("\tLast Modified:").append(new Date(file.lastModified())).append("\n");
				} else {
					dirOutput.append(file.getName()).append("\n");
				}
			}
		} catch (Exception e) {
			return "-Directory path doesn't exist";
		}
		return dirOutput.toString();
	}
}
