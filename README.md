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