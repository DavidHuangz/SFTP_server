# Notes
* The default server directory is C:/
* Port number:6789

## Logins
* Logins stored in LoginData.txt
* Format of login is: [ username account password ]

# Testing
## USER
* You must login with user with before entering password or account, or do any of the other activities except "DONE".

* Example of success login
- USER Bob -->  +Bob valid, send account and password
- ACCT ac1 --> +Account valid, send password
- PASS 123 --> ! Account valid, logged-in

 * Example of fail login (Wrong account and/or password)
- USER Bob -->  +Bob valid, send account and password
- ACCT 123 --> -Invalid account, try again
- PASS 321 --> -Wrong password, try again

 * Example of fail login (Wrong account but correct password)
- USER Bob -->  +Bob valid, send account and password
- PASS 123 --> +Send account
- ACCT 123 --> -Invalid account, try again

## DONE
* This command will shutdown the server
* Example of Done command
- DONE --> +Server has shut down


## Commands below will require login

## TYPE
* Will change transfer type
* Example of Type command
- TYPE A --> +Using Ascii mode
- TYPE B --> +Using Binary mode
- TYPE C --> +Using Continuous mode

## LIST
# Displays the files and file descriptions in the directory
* Example of LIST command
 - LIST F
 - LIST V

## CDIR
# Change directory
* Example of CDIR command
- CDIR D:/

## KILL
# Delete a file or folder
* Example of KILL command
- Create a file called "delete.txt" in C:/
- KILL delete.txt


## NAME
# rename a file
* Example of NAME command
- Create a file called "name.txt" in C:/
- TOBE renamed.txt

## RETR
# Creates a file to write some text into it
* Example of RETR command
- Create a file called "send.txt" in C:/
- RETR send.txt
- SEND
- STOP

## STOR
# Creates file in project folder to write some text into it
* Example of STOR command
- Create a file called "stor.txt" in directory
- STOR NEW
- SIZE X  --> X = Please check how big the fle is by clicking on the file's properities

* OLD will replace a existing file
* APP will append to the file






