# Fulton Recovery in Minecraft

An attempt to replicate [Fulton Recovery](https://youtu.be/Ww3lMDg16Nc?t=20) in Minecraft 1.14 using the Spigot API.

## Usage

1. Use the /fulton_home console command while looking at the ground to designate your drop point.
2. Carry string and a wooden sword.

### String

The string is used to extract entities and send them back to your drop point. While sneaking (holding shift), right click an entity with string in your hand. If there are no blocks above them, they will be sent skyward.

This plugin will try to move them to your designated drop point. You may want to build a fence or walls around your drop point to keep collected entities around.

Some entities will despawn if they get too far from you (many enemies). If this happens, the entity will be stored in 'stasis' (saved to a file).

Entities will be given a custom name when they are recovered, to prevent them from despawning in many cases.

### Wooden Sword

The wooden sword is used to summon any entities near your drop point that have custom names. It can also summon entities in 'stasis'.

While holding the sword, right click without sneaking to cycle through all of the entities near your drop point and in stasis.

Right click while sneaking and pointing at a block to summon the selected entity to you.

At the time of writing, if you summon an entity in stasis it will simply appear at the destination block. This is temporary, I plan for them to drop the same as other entities.

## Notes

* If you recover a vehicle, you may ride it as it is being recovered.
* If there's a bug in the plugin and something goes wrong, any passengers will be teleported immediately to the destination. So hopefully you won't die from fall damage.
* Minecarts with chests are handy to be able to recover
* Horses are also very handy.


## Videos
A polished, trailer-y video: https://youtu.be/baQHAmsxeec

Some WIP videos:
* https://www.youtube.com/watch?v=dagX8nclyOA&feature=youtu.be
* https://www.youtube.com/watch?v=nRIB5DMpp30&feature=youtu.be
