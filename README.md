# File_Share_System
The application can be installed on each member’s computer. The member can specify a directory in the hard disk of his/her computer as the shared root directory, a list of members who can read files, etc., through setting the parameters of the configuration.
When member X starts the application on PC X, PC X will listen to UDP port 9998 and TCP port 9999. When member A executes the discovery process on PC A, PC A will send a broadcast packet to UDP port 9998. After PC X receives the packet, it then sends an UDP packet back to PC A. The replied packet contains PC X's IP address, computer name, etc.
After receiving the responses from different computers - PC X, Y, Z, ..., the application of PC A provides a list for member A to choose from. If member A selects PC X, member A is in the user list and can provide the password, the application of PC A establishes a connection to the application of PC X and performs the following activities in PC X:
1. Read file list.
2. Create subdirectories
3. Upload and download files.
4. Delete files.
5. Delete subdirectories.
6. Change file/target name.
7. Read the file’s detail information including the size, last modified time, etc.
8. Get the directory size and last modified time.
9. Get the file location.

### I used the picture & video of Landlord's Cat,my favoutite band,for testing. Love them !!!

## System Figure
![This is an image](https://github.com/Phoenix-JI/File_Share_System/blob/main/System.png)
