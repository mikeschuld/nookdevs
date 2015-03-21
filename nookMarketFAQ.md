nookMarket

**IMPORANT INFO**
you need the latest softroot to enable upgrades.
For the old softroot, do the following-

Create a file - market.xml and put the following in there-
<?xml version="1.0" encoding="utf-8"?>


&lt;allowUpgrades&gt;

n

&lt;/allowUpgrades&gt;



This will force the market app to uninstall apps before installing. But all your app data will be retained.

This file has to go in the root ( either internal or external sd card).



The idea here is to mimic the android app store & list all the apps available for nook, allow install/upgrades, uninstall, checking for new versions of installed apps etc.

Steps to Add a new app to the store:

If your app is maintained under nookdevs project, just upload the apk in the downloads section & update updatesfeed.xml with app details. Make sure to include version and pkg tags.

Example entry:
<pre>
<entry><br>
<title>nookLibrary<br>
<br>
Unknown end tag for </title><br>
<br>
<br>
<link type="application/vnd.android.package-archive"<br>
href="http://nookdevs.googlecode.com/files/nookLibrary.apk"/><br>
<version>0.1.0<br>
<br>
Unknown end tag for </version><br>
<br>
<br>
<pkg>com.nookdevs.library<br>
<br>
Unknown end tag for </pkg><br>
<br>
<br>
<content type="text">Browse, sort and search books on the Nook.<br>
<br>
<br>
Unknown end tag for </content><br>
<br>
<br>
<br>
<br>
Unknown end tag for </entry><br>
<br>
<br>
</pre>

If your app is in a different repository,
create an xml file with the following syntax, put it in a place accessible without userid/pass and send us the link (create a task under Issues section).

<pre>
<?xml version="1.0"?><br>
<feed xmlns="http://www.w3.org/2005/Atom"><br>
<title>nookDevs<br>
<br>
Unknown end tag for </title><br>
<br>
  <!-- optional --><br>
<author><name>nookDevs<br>
<br>
Unknown end tag for </name><br>
<br>
<br>
<br>
Unknown end tag for </author><br>
<br>
 <!-- optional --><br>
<entry><br>
... <!-- same as above example --><br>
<br>
<br>
Unknown end tag for </entry><br>
<br>
<br>
<br>
<br>
Unknown end tag for </feed><br>
<br>
<br>
</pre>

Again, make sure to update the contents of this file whenever the version changes. A single file can have multiple apps.