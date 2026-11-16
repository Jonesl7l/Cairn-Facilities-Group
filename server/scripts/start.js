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
  '--host', process.env.HOST ?? 'localhost',
  // Reloads the database when db.json changes, so npm run reset works while running
  '--watch',
]
// 3. Run it in this terminal and exit when it exits
spawn(process.execPath, args, { cwd: root, stdio: 'inherit' }).on('exit', (code) => process.exit(code ?? 0))
