## Requirements
1. Install (babashka)[https://github.com/babashka/babashka?tab=readme-ov-file#brew]

## Use scripts via `bbt`
To use scripts globally across the file system:
1. Copy `bbt` shell script into a exec directory on your `$PATH`, like `~/.local/bin`.
2. Make sure the script matches your system (check that it uses your shell, and has the path of this directory in the command)

### `login`
Create a default creds file in your home directory (`~/.asu-creds.edn`). Use edn, ie:
```clojure
{:username "Me Mate" :password "passy" }
```
You can put in empty values and use the parameters instead if storing them on your machine seems risky.

Usage:
```bash
bbt login --username "foo" --password "bar" --env "test"
```

If you have a creds file no username of password is required, env defaults to `cloud`.
