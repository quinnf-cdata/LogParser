# LogParser
This application is intended to allow users to parse and sort large log files into more readable and understandable data.


## GUI
The GUI contains the basic functions for operating the application. Each button is essentially a command that triggers an operation.
When the application is first started, the buttons shown are:
-   Choose file - lets the user pick a file with a dialog window. The user can also paste a file path with the text box to the right of the button.
-   Load file - loads the file that was picked by the user.
-   Reset - resets the entire application.
-   Decode Hex - decodes the hex values in strings as they are read by the application.
-   Split by - splits the loaded log into smaller logs based on the selected dropdown value.
-   Sort by - sorts the loaded log based on the selected dropdown value.
-   Refresh - refreshes the loaded log data.

![image](https://github.com/qkfreas/LogParser/assets/12768804/3b25aa16-1622-4640-bbcd-671c1e79e7e2)

### Other Buttons
When a file is loaded, new buttons will appear at the bottom of the application:
-   Back - navigates to the previous page.
-   Next - navigates to the next page.
-   Go - jumps to the user defined page in the text box.
-   Search - searches for user defined keywords in the loaded file.

![image](https://github.com/qkfreas/LogParser/assets/12768804/1cfc1462-b9db-4bf3-9b79-441e6ebbfe38)

## Performance
Performance is always a concern when working with data. This application strives to reduce the resource requirements. By loaded only the data in view of the user, the application only requires a small amount of RAM when reading a file.
The application uses Apache Lucene to handle the indexing. Lucene is a light weight file indexing package. Indexing is required to process splitting and sorting a file. 
The drawback of using indexing to split and sort a file is the time it takes to split files with large indexes.

[//]: # (```jpackage --name "CData Log Analyzer" --main-jar LogParser-1.0-SNAPSHOT-jar-with-dependencies.jar --main-class com.example.logparser.Launcher --input C:\Users\QuinnFreas\IdeaProjects\LogParser\target\ --type exe --win-shortcut --description "CData Log Analyzer Version 8753" --win-dir-chooser --win-shortcut-prompt```)