import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ShareFile {

	DatagramSocket socket;

	ServerSocket srvSocket;
	ArrayList<Socket> TCPlist = new ArrayList<Socket>();

	public ShareFile() throws SocketException {
		socket = new DatagramSocket(9998);

	}

	// TCP Server
	public void EstabServer(int port) throws IOException {
		srvSocket = new ServerSocket(port);

		while (true) {
			Socket cSocket = srvSocket.accept();

			synchronized (TCPlist) {
				TCPlist.add(cSocket);
				// System.out.printf("Total %d clients are connected.\n", TCPlist.size());
			}

			Thread t = new Thread(() -> {
				try {
					serve(cSocket);
				} catch (IOException e) {
					// System.err.println("connection dropped.");
				}
				synchronized (TCPlist) {
					TCPlist.remove(cSocket);
				}
			});
			t.start();
		}
	}

	private void serve(Socket clientSocket) throws IOException {
		byte[] buffer = new byte[1024];
		// System.out.printf("Established a connection to host %s:%d\n\n",
		// clientSocket.getInetAddress().toString().substring(1),
		// clientSocket.getPort());

		DataInputStream in = new DataInputStream(clientSocket.getInputStream());
		DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
		while (true) {
			int len = in.readInt();
			in.read(buffer, 0, len);

			String[] spliter = new String(buffer, 0, len).split(" ");

			switch (spliter[0]) {
			case "dir":
				if (spliter.length > 1)
					listFileSub(spliter[1], clientSocket);
				else
					listFileSub(".", clientSocket);
				break;

			case "md":
				if (spliter.length > 1)
					createSub(spliter[1], clientSocket);
				else {

					String mdCommand = "Missing arugment";
					out.writeInt(mdCommand.length());
					out.write(mdCommand.getBytes(), 0, mdCommand.length());
				}
				break;

			case "del":
				if (spliter.length > 1)
					deleteFile(spliter[1], clientSocket);
				else {

					String delCommand = "Missing arugment";
					out.writeInt(delCommand.length());
					out.write(delCommand.getBytes(), 0, delCommand.length());
				}
				break;

			case "rd":
				if (spliter.length > 1)
					deleteDir(spliter[1], clientSocket);
				else {

					String rdCommand = "Missing arugment";
					out.writeInt(rdCommand.length());
					out.write(rdCommand.getBytes(), 0, rdCommand.length());
				}
				break;

			case "upload":

				DownloadFile(clientSocket);
				break;

			case "download":
				String dlCommand = "";

				if (!new File(spliter[1]).exists()) {

					dlCommand = "File does not exist on the server";
					out.writeInt(dlCommand.length());
					out.write(dlCommand.getBytes(), 0, dlCommand.length());
					break;
				}

				dlCommand = "DownloadtoClient";
				out.writeInt(dlCommand.length());
				out.write(dlCommand.getBytes(), 0, dlCommand.length());
				UploadFile(spliter[1], clientSocket);
				break;

			case "cn":
				changeTargetName(spliter[1], spliter[2], clientSocket);
				break;

			case "gfi":
				getFileInfo(spliter[1], clientSocket);
				break;

			case "gdi":
				getDirInfo(spliter[1], clientSocket);
				long modT = new File(spliter[1]).lastModified();

				String data = "Directory size: " + getDirInfo(spliter[1], clientSocket) + " bytes" + "   "
						+ "Last Modified Time: " + new Date(modT);
				out.writeInt(data.length());
				out.write(data.getBytes(), 0, data.length());
				break;

			case "search":

				searchFile(spliter[1], spliter[2], clientSocket);
				break;
			default:
				// System.out.println("Unknown Command");
				String Command = "Unknown Command";
				out.writeInt(Command.length());
				out.write(Command.getBytes(), 0, Command.length());

			}

		}

	}

	// Search the file address
	public static void searchFile(String fileName, String dir, Socket clientSocket) throws IOException {

		Socket socket = clientSocket;
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());

		File file = new File(dir);

		String fp = "";

		if (!file.exists()) {
			fp = "The searched rang does not exist";
			// System.out.println(fp);
			out.writeInt(fp.length());
			out.write(fp.getBytes(), 0, fp.length());

			return;
		}

		if (file.isFile()) {
			fp = "The searched rang is the file. Please Update";
			// System.out.println(fp);
			out.writeInt(fp.length());
			out.write(fp.getBytes(), 0, fp.length());

			return;
		}

		File[] list = file.listFiles();

		if (list == null)
			return;

		for (File f : list) {
			if (f.isDirectory()) {
				searchFile(fileName, f.getAbsolutePath(), clientSocket);
				if (f.getAbsolutePath().endsWith(fileName)) {

					fp = "Dir:" + f.getAbsoluteFile();
					// System.out.println(fp);
					out.writeInt(fp.length());
					out.write(fp.getBytes(), 0, fp.length());

				}
			} else {

				if (f.getAbsoluteFile().toString().endsWith(fileName)) {

					fp = "File:" + f.getAbsoluteFile();
					// System.out.println(fp);
					out.writeInt(fp.length());
					out.write(fp.getBytes(), 0, fp.length());

				}

			}
		}

	}

	// change File/Dir Name
	public static void changeTargetName(String s, String d, Socket clientSocket) throws IOException {

		Socket socket = clientSocket;
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());

		File f = new File(s);

		File dest = new File(d);

		String data = "";

		if (f.renameTo(dest)) {
			data = "Target is renamed";
			out.writeInt(data.length());
			out.write(data.getBytes(), 0, data.length());

		} else {
			data = "Target cannot be renamed";
			out.writeInt(data.length());
			out.write(data.getBytes(), 0, data.length());

		}
	}

	// get File Information
	public static void getFileInfo(String filePath, Socket clientSocket) throws IOException {

		Socket socket = clientSocket;
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());

		File file = new File(filePath);
		String data = "";

		if (!file.exists()) {
			data = "File does not exist";
			out.writeInt(data.length());
			out.write(data.getBytes(), 0, data.length());
			return;
		}

		if (!file.isFile()) {
			data = "Target is not file, youcan choose Get Directory Information";
			out.writeInt(data.length());
			out.write(data.getBytes(), 0, data.length());
			return;
		}

		long size = file.length();
		long modT = file.lastModified();

		data = "File size: " + size + " bytes" + "   " + "Last Modified Time: " + new Date(modT);
		out.writeInt(data.length());
		out.write(data.getBytes(), 0, data.length());

	}

	// get Directory Information: Calculate the dir size by adding each file's size
	public long getDirInfo(String dirPath, Socket clientSocket) throws IOException {

		Socket socket = clientSocket;
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());

		File dir = new File(dirPath);

		String data = "";

		long size = 0;

		if (!dir.exists()) {
			data = "File does not exist";
			out.writeInt(data.length());
			out.write(data.getBytes(), 0, data.length());
			return size;
		}

		if (dir.isFile()) {
			data = "Target is not file, you can choose Get Directory Information service";
			out.writeInt(data.length());
			out.write(data.getBytes(), 0, data.length());
			return size;
		}

		File[] files = dir.listFiles();

		int count = files.length;

		for (int i = 0; i < count; i++) {
			if (files[i].isFile()) {
				size += files[i].length();
			} else {
				size += getDirInfo(files[i].toString(), clientSocket);

			}
		}

		return size;
	}

	// UPload Files
	public void UploadFile(String sourceFile, Socket socket) {

		File file = new File(sourceFile);

		try {

			DataOutputStream out = new DataOutputStream(socket.getOutputStream());

			FileInputStream in = new FileInputStream(file);

			out.writeInt(file.getName().length());
			out.write(file.getName().getBytes());

			long size = file.length();
			out.writeLong(size);

			byte[] buffer = new byte[1024];

			while (true) {
				int len = in.read(buffer, 0, buffer.length);
				if (len <= 0) {
					// in.close();
					// out.close();
					// System.out.println("File Uploaded");
					break;
				}
				out.write(buffer, 0, len);
			}

		} catch (UnknownHostException e) {
			System.out.println("Server Not Found "); // To handle incorrect ip address
		} catch (IllegalArgumentException e) {
			System.out.println("Port Number is incorrect: " + e.getMessage());
		} catch (ConnectException e) {
			System.out.println("Cannot connect with the server: " + e.getMessage()); // Need to wait a while to get the
																						// exception message
		} catch (IOException e) {
			System.out.println("Unable to upload the file: " + e.getMessage());

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}

	// Download Files
	public static void DownloadFile(Socket socket) {
		byte[] buffer = new byte[1024];
		try {
			DataInputStream in = new DataInputStream(socket.getInputStream());

			int nameLen = in.readInt();
			in.read(buffer, 0, nameLen);
			String name = new String(buffer, 0, nameLen);

			// System.out.println("Downloading file " + name);

			long size = in.readLong();
			// System.out.printf("(%d)", size);

			File file = new File(name);
			FileOutputStream out = new FileOutputStream(file);

			while (size > 0) {
				int len = in.read(buffer, 0, buffer.length);
				out.write(buffer, 0, len);
				size -= len;
				// System.out.print(".");
			}
			// System.out.println("\nDownload completed.");

			// in.close();
			out.close();
		} catch (IOException e) {
			System.err.println("unable to download file.");
		}
	}

	// Read File List
	public static void listFileSub(String pathname, Socket clientSocket) throws IOException {

		Socket socket = clientSocket;

		DataOutputStream out = new DataOutputStream(socket.getOutputStream());

		String data = "";

		File path = new File(pathname);

		if (!path.exists()) {

			data = "File not Found";
			out.writeInt(data.length());
			out.write(data.getBytes(), 0, data.length());
			return;
		}

		File[] filelist = path.listFiles();

		if (filelist.length == 0) {

			data = "File Empty";
			out.writeInt(data.length());
			out.write(data.getBytes(), 0, data.length());
			return;
		}

		for (File file : filelist) {

			data = "";

			if (file.isFile()) {
				data = file.getName() + " " + file.length() + " bytes";
			} else {
				data = file.getName() + ": [dir]";
			}

			out.writeInt(data.length());
			out.write(data.getBytes(), 0, data.length());

		}

	}

	// Create Subdirectories
	public static void createSub(String pathname, Socket clientSocket) throws IOException {

		Socket socket = clientSocket;
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());

		String data = "";

		File dir = new File(pathname);

		if (dir.exists()) {

			data = "File/Directory exists";
			out.writeInt(data.length());
			out.write(data.getBytes(), 0, data.length());

			return;
		}
		dir.mkdirs();

		data = "Create Subdirectory Successfully";
		out.writeInt(data.length());
		out.write(data.getBytes(), 0, data.length());
	}

	// Delete File
	public static void deleteFile(String pathname, Socket clientSocket) throws IOException {

		Socket socket = clientSocket;
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());

		String data = "";

		File tarFile = new File(pathname);

		if (!tarFile.exists()) {

			data = "File does not exist";
			out.writeInt(data.length());
			out.write(data.getBytes(), 0, data.length());
			return;
		}

		if (!tarFile.isFile()) {

			data = "To delete a directory, you should use Delete subdirectories service.";
			out.writeInt(data.length());
			out.write(data.getBytes(), 0, data.length());
			return;
		}

		if (tarFile.delete()) {

			data = "File deleted successfully ";
			out.writeInt(data.length());
			out.write(data.getBytes(), 0, data.length());
		} else {

			data = "Failed to delete the file";
			out.writeInt(data.length());
			out.write(data.getBytes(), 0, data.length());
		}

	}

	// Delete Subdirectories
	public static void deleteDir(String pathname, Socket clientSocket) throws IOException {

		Socket socket = clientSocket;
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());

		String data = "";

		File dir = new File(pathname);
		File[] dirlist = dir.listFiles();

		if (!dir.exists()) {

			data = "Directory does not exist";
			out.writeInt(data.length());
			out.write(data.getBytes(), 0, data.length());
			return;
		}

		if (dir.isFile()) {

			data = "To delete a file, you should use Delete files service.";
			out.writeInt(data.length());
			out.write(data.getBytes(), 0, data.length());
			return;
		}

		if (dirlist.length != 0) {

			data = "The directory " + pathname + " is not empty!";
			out.writeInt(data.length());
			out.write(data.getBytes(), 0, data.length());
			return;
		}

		if (dir.delete()) {

			data = "Directory deleted successfully ";
			out.writeInt(data.length());
			out.write(data.getBytes(), 0, data.length());

		} else {

			data = "Failed to delete the directory";
			out.writeInt(data.length());
			out.write(data.getBytes(), 0, data.length());

		}

	}

	// TCP Client (Establish the connection to the server)
	public void ClientlinkServer(String server, int port, ArrayList<String> UDPlist, ArrayList<String> NameList)
			throws IOException, InterruptedException {
		Socket socket = new Socket(server, port);
		DataInputStream in = new DataInputStream(socket.getInputStream());
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());

		System.out.println("Established a connection to server  " + server);

		String s = "";

		Thread t = new Thread(() -> {

			byte[] buffer = new byte[1024];

			try {

				while (true) {

					int infolen = in.readInt();
					in.read(buffer, 0, infolen);
					String infoM = new String(buffer, 0, infolen);

					if (!infoM.equals("DownloadtoClient")) {
						System.out.println(infoM);
					}

					while (infoM.equals("DownloadtoClient")) {

						DownloadFile(socket);
						break;

					}

				}
			} catch (IOException ex) {
				System.out.println(ex.getMessage());
				System.err.println("Connection dropped!");
				// System.exit(-1);
			}
			// System.out.println("Testing");

		});

		t.start();

		while (!s.equals("0")) {

			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println("Please select the service:");
			System.out.println(" 0. Choose Another Member  1. Display the Member List  2. List the File  ");
			System.out.println(" 3. Create Subdirectories    4. Delete files.              5. Delete subdirectories. ");
			System.out.println(" 6. Upload Files             7. Donwolad Files             8. Change File/Target Name");
			System.out.println(" 9. Get File Information     10. Get Directory Information 11. Search File");

			Scanner scanner = new Scanner(System.in);
			String service = scanner.nextLine();

			switch (service) {

			case "0":
				s = "0";
				break;

			case "1":
				System.out.println(NameList);
				break;

			case "2":
				String LFcommand = "dir";
				System.out.println("Please input the File address:");
				String listCommand = LFcommand + " " + scanner.nextLine();
				out.writeInt(listCommand.length());
				out.write(listCommand.getBytes(), 0, listCommand.length());
				break;

			case "3":
				String CDcommand = "md";
				System.out.println("Please input the subdirectory address:");
				String CreateCommand = CDcommand + " " + scanner.nextLine();
				out.writeInt(CreateCommand.length());
				out.write(CreateCommand.getBytes(), 0, CreateCommand.length());
				break;

			case "4":
				String DFcommand = "del";
				System.out.println("Please input the file address:");
				String delFileCommand = DFcommand + " " + scanner.nextLine();
				out.writeInt(delFileCommand.length());
				out.write(delFileCommand.getBytes(), 0, delFileCommand.length());
				break;

			case "5":
				String DDcommand = "rd";
				System.out.println("Please input the directory address:");
				String delDirCommand = DDcommand + " " + scanner.nextLine();
				out.writeInt(delDirCommand.length());
				out.write(delDirCommand.getBytes(), 0, delDirCommand.length());
				break;

			case "6":

				System.out.println("Please input the file address:");
				String uploadFileCommand = scanner.nextLine();

				File file = new File(uploadFileCommand);

				if (!file.exists()) {

					String data = "The file does not exist on your computer ";
					System.out.println(data);
					break;
				}

				if (!file.isFile()) {
					System.out.println("The path input is not a file");

					if (file.isDirectory())
						System.out.println("The path input is a directory");

					break;
				}

				String command = "upload file";
				out.writeInt(command.length());
				out.write(command.getBytes(), 0, command.length());

				UploadFile(uploadFileCommand, socket);
				break;

			case "7":

				System.out.println("Please input the download file address:");
				String downloadFileCommand = "download" + " " + scanner.nextLine();
				out.writeInt(downloadFileCommand.length());
				out.write(downloadFileCommand.getBytes(), 0, downloadFileCommand.length());
				break;

			case "8":
				String CNcommand = "cn";

				System.out.println("Please input the original file/directory address:");
				String orgAd = scanner.nextLine();
				System.out.println("Please input the changed file/directory address:");
				String curAd = scanner.nextLine();
				String changeNameCommand = CNcommand + " " + orgAd + " " + curAd;
				out.writeInt(changeNameCommand.length());
				out.write(changeNameCommand.getBytes(), 0, changeNameCommand.length());
				break;

			case "9":
				String gfIcommand = "gfi";
				System.out.println("Please input file address:");
				String getFileInfoCommand = gfIcommand + " " + scanner.nextLine();
				out.writeInt(getFileInfoCommand.length());
				out.write(getFileInfoCommand.getBytes(), 0, getFileInfoCommand.length());
				break;

			case "10":
				String gdIcommand = "gdi";
				System.out.println("Please input directory address:");
				String getDirInfoCommand = gdIcommand + " " + scanner.nextLine();
				out.writeInt(getDirInfoCommand.length());
				out.write(getDirInfoCommand.getBytes(), 0, getDirInfoCommand.length());
				break;

			case "11":
				String searchcommand = "search";
				System.out.println("Please input filename:");
				String fileN = scanner.nextLine();
				System.out.println("Please input searched range (if empty, will search ynder C:\\Users) :");
				String searchR = scanner.nextLine();
				if (searchR.isEmpty()) {
					searchR = "C:\\Users";
				}
				String searchInfoCommand = searchcommand + " " + fileN + " " + searchR;
				// System.out.println(searchInfoCommand);
				out.writeInt(searchInfoCommand.length());
				out.write(searchInfoCommand.getBytes(), 0, searchInfoCommand.length());
				break;

			default:
				System.out.println("Unknown Command");

			}

		}

	}

	// UDP Send
	public void sendMsg(String str, String destIP, int port) throws IOException {

		InetAddress destination = InetAddress.getByName(destIP);
		DatagramPacket packet = new DatagramPacket(str.getBytes(), str.length(), destination, port);
		socket.send(packet);

	}

	// UDP Receive
	public String[] receive(ArrayList<String> list, ArrayList<String> NameList) throws IOException {

		DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);

		socket.receive(packet);

		byte[] data = packet.getData();
		String str = new String(data, 0, packet.getLength());
		String srcAddr = packet.getAddress().toString();

		String[] msg = new String[2];
		msg[0] = str;
		msg[1] = srcAddr;

		String localIP = InetAddress.getLocalHost().toString().split("/")[1];
		String receiveIP = packet.getAddress().toString().substring(1);

		boolean addIP = true;
		for (String item : list) {
			if (item.equals(receiveIP)) {
				addIP = false;
				break;
			}
		}

		if (!receiveIP.equals(localIP) && addIP == true) {
			synchronized (list) {
				list.add(receiveIP);
				NameList.add(str);
			}
		}
		return msg;
	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		String localIP = InetAddress.getLocalHost().toString().split("/")[1];

		System.out.println("Local IP Address: " + localIP);

		ShareFile sf = new ShareFile();

		// Login with UserName and Password

		HashMap<String, String> IdPwTest = new HashMap<String, String>();
		File file = new File("UserList.txt");
		FileInputStream in = new FileInputStream(file);

		Scanner sc = new Scanner(in);

		while (sc.hasNextLine()) {

			String userInfo = sc.nextLine();
			if (!userInfo.isEmpty()) {
				String[] IDPW = userInfo.split(" ");
				IdPwTest.put(IDPW[0], IDPW[1]);
			}

		}

		sc.close();

		System.out.println("Please Input Username: ");

		Scanner scanner_ = new Scanner(System.in);
		String userName = scanner_.nextLine();
		while (!IdPwTest.containsKey(userName)) {
			System.out.println("No such User. Please Input UserName Again: ");
			userName = scanner_.nextLine();
		}

		System.out.println("Please Input Password: ");
		String passWordTest = scanner_.nextLine();

		while (!passWordTest.equals(IdPwTest.get(userName))) {
			System.out.println("Password is not correct. Please Input Password Again: ");
			passWordTest = scanner_.nextLine();
		}

		final String UN = userName;

		// TCP Server Start
		Thread t0 = new Thread(() -> {
			while (true) {
				try {
					sf.EstabServer(9999);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		t0.start();

		ArrayList<String> UDPlist = new ArrayList<String>();
		ArrayList<String> NameList = new ArrayList<String>();

		// UDP BoardCast
		Thread t1 = new Thread(() -> {

			while (true) {
				try {
					// sf.sendMsg("Is anyone here?", "255.255.255.255", 9998);
					sf.sendMsg(UN, "255.255.255.255", 9998);
					Thread.sleep(5000);

				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		t1.start();

		// UDP BoardCast Response
		Thread t2 = new Thread(() -> {
			while (true) {
				try {

					String[] infoRece = sf.receive(UDPlist, NameList);
					String msgText = infoRece[0].toString();
					String msgIP = infoRece[1].toString().substring(1);

					if (!msgText.equals(UN)) {

						// System.out.println("sent by:\t" + msgIP + " " + msgText);
						sf.sendMsg(UN, msgIP, 9998);

					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});

		t2.start();

		// TCP ClientConnect Server
		Thread t3 = new Thread(() -> {
			int a = 0;

			while (true) {

				if (a == 0) {
					try {
						TimeUnit.SECONDS.sleep(30);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				if (UDPlist.size() != 0) {

					System.out.println("Please choose member by the serial number : ");

					int i = 0;

					for (String memberName : NameList) {
						System.out.println(i + " -> " + memberName);
						i++;
					}

/////
					Scanner scanner = new Scanner(System.in);
					String compID = scanner.nextLine();

	
					try {
						Integer.parseInt(compID);
					} catch (NumberFormatException e) {
						System.out.println("Input is invalid. Please input the number: ");
					
						compID = scanner.nextLine();
					}
					
					
					try {
						NameList.get(Integer.parseInt(compID));
					}catch(IndexOutOfBoundsException e) {
						System.out.println("No Such Member. Please input the correct number: ");
						compID = scanner.nextLine();
					}
					

					System.out.println("PLease Input Password: ");
					String passWord = scanner.nextLine();

					// while (!passWord.equals(ipPW.get(UDPlist.get(Integer.parseInt(compID))))) {
					while (!passWord.equals(IdPwTest.get(NameList.get(Integer.parseInt(compID))))) {
						System.out.println("PLease Input Password Again: ");
						passWord = scanner.nextLine();
					}

					try {
						sf.ClientlinkServer(UDPlist.get(Integer.parseInt(compID)), 9999, UDPlist, NameList);
						a++;
						// System.out.println("Jump Out");
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				} else {
					// System.out.println("No Computer Can Connect");
				}
			}

		});

		t3.start();

	}

}
