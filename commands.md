### Commands list

Use these commands in chat, command prefix is ```.```

##### Manage stalked players
```.stalk <player_name>```  
Toggle stalking on a player.

##### Ignore players
```.ignore <player_name>```  
Client-side version of /ignore.

##### Horizontal / Vertical client-side teleport
```.hclip <blocks_amount>```  
```.vclip <blocks_amount>```

##### Use entity / block
```.use [sneak|attack] <entity_id>```  
```.use <block_x> <block_y> <block_z>```

If entity_id, or block positions are not specified it will be applied to what you are looking at.

##### get entity id / block position while looking at it
```.raytrace```

##### get vanished players (online but not shown in the players tab)
```.diff```

##### Disconnect from server
```.disconnect```

##### Send a respawn packet to server
```.respawn```

Used to respawn while on undead form.

##### Get server information
```.info```

Information about gamemode, world type, server difficulty ..

```.info plugins```

Information about server's bukkit plugins that are currently listening for client requests.

##### Get available commands from server
```.commands```

##### Entity desync
```.vanish dismount```  
```.vanish remount```  

##### Client-side mount
```.mount <entity_type>```  
```.mount null``` - dismount

##### Spectate something/somebody while in spectator mode
```.spectate <entity_uuid>```

##### Print in chat all packets received from the server
```.pckdump <on|off>```

##### Sync player position between client & server
```.sync```

##### Open mount inventory
```.open```  
Will create a ghost donkey if your mount does not exist client side (server desync).

##### Peek at shulker/book dropped on ground or held by another entity
```.stare```  
```.stare book```

##### 2b2t queue peek
```.queue <show|hide>```  
Show people joining / leaving queue while being in queue.


```.queue <player_name>```  
Is player in queue ?

##### Nearest stronghold (9b9t seed)
```.nearest```

##### Fill a book with random characters to make it reach a size of 32Kb
```.fill [sign]```

##### Get network size of currently held item (in bytes)
```.size```

##### Read / Edit sign
```.sign```  
Read sign data, possibility to see hidden text  

```.sign line 1 + line 2```  
Edit sign, cancel CPacketUpdateSign when exiting sign edit gui to be able to edit it later.

##### Rollback to last teleport position
```.rollback```  
First use is initialization, second performs the rollback.

##### Use entity from unloaded chunk
While standing near an unloaded chunk, be able to load it and use an entity (minecart, horse, ...) in the chunk by guessing its entity id. You still need to be near the entity to use it.

How it works (actions performed sequentially):
 - drop one dirt on floor
 - use / break a block from unloaded chunk to load the chunk and its entities.
 - drop one cobblestone on floor
 - After receiving from server the entities ids of the dirt & cobblestone items: try to use every entity id between the dirt id and the cobblestone id (guess the target entity id).
 - If everything worked correctly your action was performed, then the chunk will unload. For example if you tried to mount a horse you can then use the ```.open``` command to open its inventory.

Set up command: register the block to be used to load the chunk (it has to be a block in the [future] unloaded chunk, and be reachable):  
```.ldride reg```

Check the registered block:  
```.ldride get```

Actual command: load the chunk, guess entity id & use the entity:  
```.ldride exe [break] [sneak] [nb_tries]```

Requires dirt & cobblestone blocks in your inventory.

```break``` - send a break packet instead of a use packet.  
```sneak``` - sneak before using.  
```nb_tries``` - maximum number of entity ids to try. Default is 15, to limit packet spamming.

##### Block at
```.at <x> <y> <z>```  
What is the block at given positions.

##### Remotely keep a chunk area loaded
```.load <center_x> <center_z> [radius] | off```

```(center_x, center_z)``` are **chunk coords**   (normal coords divided by 16)  
```radius``` in number of chunks around center, default 2

Example of use: remotely load previously set ender pearl.

##### Populate a chunk area
```.populate <corner_x> <corner_z> <width_x> <width_z> | off```

```(corner_x, corner_z)``` north-west chunk corner coords - **chunk coords**    
```width_x``` and ```width_z``` in chunks

Populating an area can prevent new-chunks exploit.

##### Remote world download (slow)
```.capture (<save_name> <corner_x> <corner_z> <width_x> <width_z> [half | surface | full]) | off```

```(corner_x, corner_z)``` north-west chunk corner coords - **chunk coords**     
```width_x``` and ```width_z``` in chunks  
```surface```, ```half```, ```full``` which part of chunk to capture, from fastest to slowest. Default is full (very slow but complete chunk).

##### Detect / track players in a given area - PaperMC only
```.scan (<center_x> <center_z> <radius> [track]) | off```

```(center_x, center_z)``` are **chunk coords**  
```radius``` radius to scan around center chunk, in blocks  
```track``` option to remotely track a player once the scanner finds one

This command will open the scan GUI, when closed can be reopened with ```.scan```.

All scan usage:  
```.scan <radius> [track]``` - square scan around current position  
```.scan <radius_x> <radius_z> [track]``` - rectangle scan around current position, (radius_x, radius_z) in blocks  
```.scan <center_x> <center_z> <radius> [track]``` - square scan around given center  
```.scan <center_x> <center_z> <radius_x> <radius_z> [track]``` - rectangle around with given center  
