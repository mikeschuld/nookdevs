0.1.5 version -

What's new?

Cover Screensaver option has 3 settings to select nbr. of covers to display.

bug fixes


0.1.4 version-

What's new?

Modified app to handle more books.

Cover Screensaver option.

bug fixes

0.1.3 version-

View by menu to change the display to Folders or Tags.

Ability to archive and delete B&N books, including samples

Improved cover flow

bug fixes

0.0.9 version - Supports B&N 1.3. This doesn't work in older versions.

Following changes are included in 0.0.8 version - updated on 04/05 with few more bug fixes.<br>
Option to download books from fictionwise and smashwords libraries directly via WIFI<br>
Ability to filter books by author<br>
Option to archive books - Doesn't work for B&N books as of now.<br>
Fixed Series sorting issue and few other bugs<br>

Following changes are included in 0.0.7 version- updated on 03/15 with bug fixes.<br>
<h2>1.Series info included in title.</h2>
Series name/nbr from calibre tags included in title. Sorting by title/author will keep series books together.<br>
<h2>2.B&N books & covers</h2>
All B&N books are included in the listing. Covers for them are downloaded automatically and there is an option to download these books now.<br>
<h2>3.Scanning logic optimizations</h2>
Moved some of the scanning parsing to background and also added a db to store more details about the books.<br>
<br>
Following changes are included in 0.0.6 version-<br>
<h2>1.Tags in pdf files </h2>
Added ability to read description and keyword tags from pdf files. Now, both epub and pdf files can have tags. pdb files still won't work.<br>
<h2>2.<a href='https://code.google.com/p/nookdevs/issues/detail?id=34'>Issue 34</a>:</h2> Handling unexpected failures from IECMScannerService<br>
<h2>3.<a href='https://code.google.com/p/nookdevs/issues/detail?id=38'>Issue 38</a>:</h2> Tags sort based on upper/lower case<br>

Following changes are included in 0.0.5 version-<br>
<h2>1. Show menu keywords</h2>
Added option to read standard keywords from mybooks.xml file from sdcard root folder.<br>
Here is a sample file:<br>
<pre>
<?xml version="1.0" encoding="utf-8"?><br>
<keywords><br>
<br>
<keyword>Fiction<br>
<br>
Unknown end tag for </keyword><br>
<br>
<br>
<br>
<keyword>Nonfiction<br>
<br>
Unknown end tag for </keyword><br>
<br>
<br>
<br>
<keyword>Adventure<br>
<br>
Unknown end tag for </keyword><br>
<br>
<br>
<br>
<keyword>Sports<br>
<br>
Unknown end tag for </keyword><br>
<br>
<br>
<br>
<br>
<br>
Unknown end tag for </keywords><br>
<br>
<br>
<br>
</pre>
If this file is present, Keywords listed in the file will be included in the Show menu before other keywords automatically read from books.<br>
<h2>2. Skipping folders</h2>
if you want to exclude a folder from the listing, create a file named .skip in the folder.<br>
Any folder with this file & all it's sub folders will be excluded from the scan.<br>
Also, Files starting with . will be excluded from the listing.<br>
<h2>3. longpress on up/down arrow keys</h2>
Pressing on these buttons for 3-4 secs will move the cursor to the top/bottom of the page.<br>
<h2>4. Sorting</h2>
Fixed a problem with most_recent sort option. pdf files are sorted properly now.<br>
Also, books opened last will be listed at the top. This will not work if the file is opened from my library or other apps.