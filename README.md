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

##### Disconnect from server
```.disconnect```

##### Open mount inventory
```.open```  
Will create a ghost donkey if your mount does not exist client side (server desync).

##### Use entity from unloaded chunk
While standing near an unloaded chunk, be able to load it and use an entity (minecart, horse, ...) in the chunk by guessing its entity id. You still need to be near the entity to use it.

How it works (actions performed sequentially):
 - drop one dirt on floor
 - use / break a block from unloaded chunk to load the chunk and its entities.
 - drop one cobblestone on floor
 - After receiving from server the entities ids of the dirt & cobblestone items: try to use every entity id between the dirt id and the cooblestone id (guess the target entity id).
 - If everything worked correctly your action was performed, then the chunk will unload. For example if you tried to mount a horse you can then use the ```.open``` command to open its inventory (dupe ?).

Set up command: register the block to be used to load the chunk (it has to be a block in the [future] unloaded chunk):  
```.ldride reg```

Check the registered block:  
```.ldride get```

Actual command: load the chunk, guess entity id & use the entity:  
```.ldride exe [break] [nb_tries]```

You need at least one dirt & cobblestone item in your inventory to use the ```.ldride exe``` command.

Specify ```break``` if you want to send a break packet instead of a use packet, to load the chunk.  
```nb_tries``` is a number, the maximum number of entity ids to try. This is used to limit the packet spam if the gap between dirt id & cobblestone id is too big. Default is 15, maximum 15 use entity packets sent.
