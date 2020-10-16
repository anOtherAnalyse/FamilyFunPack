## Family Fun Pack

### Summary

Minecraft 1.12.2 utility mod for anarchy servers, 2b2t - 9b9t. Family Fun Pack is not a complete anarchy utility mod, but instead offers a few features that are missing or are improved from what offer others utility mods.

### Usage
Use the ```backslash``` key to open the GUI (by default). Change it from Minecraft keybinds options.

### Modules

##### True Durability
Show tools & armour true durability. Used to know the real durability of "unbreakable" items that have an abnormally high durability.   
Display the real durability in the toolTip showed when hovering an item in the inventory.  
"Unbreakable" items are displayed in red enchant, to be able to spot it on other players.

##### Silent Close
Don't tell the server when the client is closing a container - be able to reopen it later when trying to open player inventory. If the server forces the closing you will be warned.

##### Packets Canceling
All in title, prevent client from sending/receiving specified network packets.

##### Portal Invulnerability
Be invulnerable after going through a portal, but you won't be able to move by yourself.

##### Pig POV
Pig point of view -  When using portal invulnerability you can use a pig to travel 1x1 tunnels without taking damage. Use this to lower your point of view and see where you are going.

##### Search
Improved search module - For every block in the game that you want to search, be able to specify which color you want it to be highlighted and if you want to enable a tracer.

##### Stalker
Stalk a player: know when they connect/disconnect/change their gamemode/display name...

### Commands
Few commands, use them in chat (Command module must be enabled):
##### Manage stalked players
```.stalk add <player_name>```  
```.stalk list```  
```.stalk del <player_name>```

##### Entity desync
```.vanish dismount```  
```.vanish remount```  

##### Horizontal / Vertical teleport
```.hclip <blocks_amount>```  
```.vclip <blocks_amount>```

##### Use entity / block
```.use <entity_id>```  
```.use <block_x> <block_y> <block_z>```

##### get entity id / block position while looking at it
```.raytrace```

##### get vanished players (online but not shown in the player tab)
```.diff```
