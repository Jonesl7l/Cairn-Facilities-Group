const fs = require('node:fs')
const { spawn } = require('node:child_process')
const { root, dbFile, uploadsDir, restoreDatabase, restorePhotos } = require('./seed')

// 1. First run: create the live database and uploads from the seed
if (!fs.existsSync(dbFile)) restoreDatabase({ newDatabaseId: false })
if (!fs.existsSync(uploadsDir)) restorePhotos()

// 2. Run json-server's command line with our middlewares file
const args = [
  require.resolve('json-server/lib/cli/bin'),
  'db.json',
  '--middlewares', 'middlewares.js',
  '--port', process.env.PORT ?? '3000',
  // IPv4 loopback: the emulator's 10.0.2.2 reaches 127.0.0.1, and localhost can resolve to IPv6 only
  '--host', process.env.HOST ?? '127.0.0.1',
  // Reloads the database when db.json changes, so npm run reset works while running
  '--watch',
]
// 3. Run it in this terminal and exit when it exits
spawn(process.execPath, args, { cwd: root, stdio: 'inherit' }).on('exit', (code) => process.exit(code ?? 0))
