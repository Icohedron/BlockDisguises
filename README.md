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
```

## Commands
```
# Disguise a player as a block (if [player] isn't specified, it will disguise the command executor instead)
# Can also be followed up with any of these flags to create more types of blocks: [--variant=<variant>] [--facing=<facing>] [--color=<color>] [--half=<half>] [--type=<type>] [--wet=<wet>] [--powered=<powered>] [--delay=<delay>] [--shape=<shape>] [--conditional=<conditional>] [--axis=<axis>]
/bd disguise <block> [player]

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
# The amount of delay in ticks before the disguised player turns into a solid block
# Moving will reset this delay and unsolidify them if they were already solidified
solidify_delay: 60
```
