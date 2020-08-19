## True Item Durability Mod

#### Summary

Minecraft 1.12.2 mod showing true item durability inside item tooltip (shown when item is hovered).  
Useful to distinguish "unbreakable" & over broke items.

#### Possible issues
- Server desync when manipulating "unbreakable" items.
- May not be compatible with mods modifying minecraft network stack.

#### To do
- Don't save true durability in nbt tag as it may cause synchronisation errors
- take Windows Items packet into account
- Test if durability decreases correctly
