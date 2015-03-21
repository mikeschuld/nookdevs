nookLauncher 0.1.0
Includes option to add Sub-folders like Games, tools etc.

More support for custom icons - Uses the same logic as Mynook.ru launcher now and Icons can be updated automatically from my icons folder.

Ability to remove Launcher Settings and B&N Home buttons. Long-pressing on any of the icons will start the Settings module.

Other bug fixes.

Steps for creating a sub menu:
In Launcher Settings, enter Add apps & select sub menu app. You can change the icon by clicking on it.

To add apps to sub-menu, pick the app in Launcher Settings and long-press on the sub-menu icon.

Inside the sub-menu, Launcher settings can be started by long-pressing on any of the app icons.


nookLauncher 0.0.8

Includes option to modify icons ( refer to Issues 72 and 74).
Copy the new icons to "my icons" folder.


#summary Making the nookLauncher your default Home

UPDATE!

A new item in the default menu called "set as default launcher" will now add this default setting for you. All you have to do is hit the button and reboot your nook. You can then remove this button like all the others in the launcher settings menu.

# Bugged by the nookLauncher selector menu? #

At the moment, there are two ways to force your nookLauncher of choice to become the default selector:

## Using vncserver ##

Download androidvncserver from http://code.google.com/p/android-vnc-server/

In terminal run:
```
adb push androidvncserver /data
adb shell chmod 755 /data/androidvncserver
```
In a ADB Shell session (NOT like previous commands, just run adb shell) run
```
/data/androidvncserver -k /dev/input/mice &
```
> Open your favorite VNC client and connect to NOOK\_IP:5901

Now you can select the default home application.

## By editing /data/system/packages.xml ##

Android stores the preferences in the file
```
/data/system/packages.xml
```

You can directly edit this file to force the package of your choice to be preferred as the default Home app. Add this section
```
      <preferred-activities>
      <item name="com.nookdevs.launcher/.NookLauncher" match="100000" set="2">
      <set name="com.bravo.home/.HomeActivity" />
      <set name="com.nookdevs.launcher/.NookLauncher" />
      <filter>
      <action name="android.intent.action.MAIN" />
      <cat name="android.intent.category.HOME" />
      <cat name="android.intent.category.DEFAULT" />
      </filter>
      </item>
      </preferred-activities>
```

right after the <pre><preferred-packages /></pre> tag in packages.xml

Please make careful note of the activity (com.nookdevs.launcher/.NookLauncher in the above example, and ensure it is the same as the package you actually want to use.)

Make a backup copy just in case, edit, replace, and verify that the owner/group is system, and that the file is writeable. Then, reboot the nook.