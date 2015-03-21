nookBrowser - some useful info
<h2>1. Steps for exporting Favorites</h2>
<pre>
Enter adb shell<br>
# cd /data/data/com.nookdevs.browser/databases<br>
# ls<br>
webview.db<br>
favs.out<br>
webviewCache.db<br>
BROWSER<br>
<br>
# sqlite3 BROWSER<br>
sqlite> .output favs.out<br>
sqlite> .dump FAVS<br>
sqlite> .exit<br>
<br>
# ls -l<br>
-rw-rw---- app_1    app_1       21504 2010-02-06 14:31 webview.db<br>
-rw-rw-rw- root     root          332 2010-02-06 14:46 favs.out<br>
-rw-rw---- app_1    app_1      179200 2010-02-06 14:28 webviewCache.db<br>
-rw-rw---- app_1    app_1        5120 2010-02-06 14:31 BROWSER<br>
<br>
You can then save favs.out in your PC using adb pull.<br>
</pre>
<h2>2. Steps for importing favorites</h2>
<pre>
Use adb push to put the file back.<br>
# cd /data/data/com.nookdevs.browser/databases<br>
# sqlite3 BROWSER<br>
sqlite3 BROWSER<br>
sqlite> .read favs.out<br>
.read favs.out<br>
SQL error near line 2: table FAVS already exists<br>
Table already exists error is ok.<br>
<br>
The favs.out file contains a set of sql commands  like these –<br>
BEGIN TRANSACTION;<br>
CREATE TABLE FAVS ( id integer primary key autoincrement, name text not null, va<br>
lue text not null);<br>
INSERT INTO "FAVS" VALUES(1,'Google','http://www.google.com/');<br>
INSERT INTO "FAVS" VALUES(2,'Gmail','https://mail.google.com/mail/s/#tl/Inbox');<br>
INSERT INTO "FAVS" VALUES(3,'NPR.org','http://m.npr.org/');<br>
COMMIT;<br>
<br>
This file can be edited to add a whole bunch of favorites instead of doing it thro’ the  nookBrowser.<br>
</pre>
<h2>3.Playing rtsp streams in nookBrowser </h2>
Just click on the stream link and the media player window will open and audio/video will start. <br>
I tested this with www.npr.org, which has a lot of audio streams. It should work as long as the url starts with rtsp://.<br>
For youtube videos, the User-Agent string in Settings has to be set to desktop, since youtube is sending an android specific URL which we cannot use in nook.<br>
I did not test the video part a lot since it was not that useful and was done just for curiosity.<br>
<h2>4. Dual screen browsing </h2>
In Settings, there is an option to select the primary screen. This will determine where the pages will be loaded normally.<br> This is used for loading URL entered in address, from favorites, back button and Home page links.But any clicks on links will be loaded into the page where is was done.<br>
Sync button at the left top can be used to sync up the 2 screens. If it is clicked from the touchscreen browser view, data will be loaded into the eink.<br>
If it is done from the main menu page, data will be loaded into the touchscreen browser.<br>
Touchscreen browser doesn't allow text input. <br>So, any forms, login, search etc has to be done in the eink. <br>Once the result page is loaded, it can be synced back to touchscreen and the same login credentials/cookies will be used there.<br>
<h2>5. Entering URL in address field</h2>
This field is modified to take keywords. No need for http://. <br>
Typing just google will convert into <a href='http://www.google.com'>http://www.google.com</a>
<h2>6. Readability filter in Settings</h2>
This applies the filter from <a href='http://lab.arc90.com/experiments/readability/'>http://lab.arc90.com/experiments/readability/</a><br> to remove images and ads in the current page and <br>to make it suitable for the eink.<br>
This has to be applied for each page separately. Got this idea from Trook - thanks kbs :).