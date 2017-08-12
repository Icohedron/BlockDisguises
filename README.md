# BlockDisguises
Disguise as a block!

## Permissions
```
# Grants the use of '/bd disguise <block> [player]'
blockdisguises.command.disguise

# Grants the use of '/bd undisguise [player]'
blockdisguises.command.undisguise

# Grants the use of '/bd undisguiseall'
blockdisguises.command.undisguiseall

# Grants the use of '/bd list'
blockdisguises.command.list

# Grants the use of '/bd reload'
blockdisguises.command.reload

# Allows the use of the [player] arugment in '/bd disguise <block> [player]'
blockdisguises.disguiseothers

# Allows the use of the [player] argument in '/bd undisguise [player]'
blockdisguises.unidsguiseothers
```

## Commands
```
# Disguise a player as a block (if [player] isn't specified, it will disguise the command executor instead)
/bd disguise <block> [player] [-v variant] [-f facing] [-a axis]

# Undisguise a player (if [player] isn't specified, it will undisguise the command executor instead)
/bd undisguise [player]

# Undisguise all players (also undisguises disconnected players)
/bd undisguiseall

# List all players that are currently disguised
/bd list

# Reload BlockDisguises configuration
/bd reload
```

## Default Configuration
```
# The amount of delay in seconds before the disguised player turns into a solid block
# Moving will reset this delay and unsolidify them if they were already solidified
solidify_delay: 2

# Amount of damage dealth to disguised players per hit, regardless of what the player was hit with (hand, cooked porkchop, diamond sword -- they will all do the same damage) (arrows do not deal damage)
damage_to_disguised: 5.0

allowed_actions_while_disguised: {

    # Toggles the ability for disguised players to turn into a solid block
    turn_solid: true

    # Whether or not the disguised player gets unsolidified if their solid block gets punched (with anything, not just an empty hand)
    unsolidify_when_attacked: true

    # Whether or not disguised players can attack other entities (includes players, but not disguised players)
    attack_other_entities: true

    # Whether or not disguised players can attack other disguised players
    attack_other_disguised: false

}
```