![NoCheatPlus](http://dev.bukkit.org/media/images/41/144/NoCheatPlus_doughnut_full.png)
===========

NoCheatPlus is an unofficial fork of the famous anti-cheat plugin [NoCheat] (http://dev.bukkit.org/server-mods/nocheat/) created by [Evenprime] (https://github.com/Evenprime). NoCheatPlus attempts to enforce "vanilla Minecraft" mechanics, as well as preventing players from abusing weaknesses in Minecraft or its protocol, making your server more safe. Organized in different sections, various checks are performed to test players doing, covering a wide range including flying and speeding, fighting hacks, fast block breaking and nukers, inventory hacks, chat spam and other types of malicious behaviour. For a more complete list have a look at the always outdated [Features Page] (https://github.com/asofold/NCPDocs/blob/master/Features.creole).

Installation
---------
* Get [CraftBukkit] (http://wiki.bukkit.org/Setting_up_a_server)
* Download NoCheatPlus from either [BukkitDev (staff approved)] (http://dev.bukkit.org/server-mods/nocheatplus/files/) or [Jenkins (development versions)] (http://ci.ecocitycraft.com/job/NoCheatPlus/).
* Drop the NoCheatPlus.jar in to your plugins folder.
* Start or /reload your CraftBukkit server. (Using /reload can have unwanted side effects with players still online.)

Tips
---------
* Be sure that your CraftBukkit and NoCheatPlus versions match together.
* Don't use tabs in the config.yml file.
* For compatibility with other plugins such as mcMMO, citizens and more check out [CompatNoCheatPlus] (http://dev.bukkit.org/server-mods/compatnocheatplus-cncp/).

Compiling NoCheatPlus
---------
* We use [Maven] (http://maven.apache.org/download.cgi) 3 to handle the dependencies.
* You can compile it with this Maven goal: `mvn clean package`
* NoCheatPlus is compiled for Java 6.
* If you run into trouble with dependencies, because they have been removed from the repository for obscure reasons, or if the repository prevents you from accessing it, you can install the dependency manually.
Example for Eclipse with embedded maven:
Add a new maven build run configuration, name it appropriately, e.g. ```Install CB 1.7.5```.
Set goals to: ```install:install-file -Dfile=<PATH TO JAR> -DgroupId=org.bukkit -DartifactId=craftbukkit -Dversion=1.7.5-R0.1-SNAPSHOT -Dpackaging=jar```
On Windows the <PATH TO JAR> might look like:  ```X:\...\craftbukkit\3042\craftbukkit-1.7.5-R0.1-20140408.020329-16.jar```
To let it run you might have to set the base directory, e.g. to ```${workspace_loc}```, it does not seem to have significance.
Do set the correct version alongside the file name. On newer version of maven, you might do with much simplified goals, because the pom inside the jars are parsed. 

Links
---------

Project
* [Project at BukkitDev] (http://dev.bukkit.org/server-mods/nocheatplus/)

Download
* [BukkitDev (staff approved)] (http://dev.bukkit.org/server-mods/nocheatplus/files/)
* [Jenkins (development versions)] (http://ci.ecocitycraft.com/)

Support and Documentation
* [Tickets] (http://dev.bukkit.org/server-mods/nocheatplus/tickets/)
* [Configuration] (https://github.com/asofold/NCPDocs/tree/master/wiki/configuration)
* [Permissions] (https://github.com/asofold/NCPDocs/blob/master/wiki/permissions/permissions.creole)
* [Commands] (https://github.com/asofold/NCPDocs/blob/master/wiki/Commands.creole)

Developers
* [License] (https://github.com/NoCheatPlus/NoCheatPlus/blob/master/LICENSE.txt)
* [API] (https://github.com/asofold/NCPDocs/blob/master/wiki/development/api.creole)

Related
* [CompatNoCheatPlus at BukkitDev] (http://dev.bukkit.org/server-mods/compatnocheatplus-cncp/)
