const fs = require('node:fs')
const { spawn } = require('node:child_process')
const { root, dbFile, uploadsDir, restoreDatabase, restorePhotos } = require('./seed')

if (!fs.existsSync(dbFile)) restoreDatabase({ newDatabaseId: false })
if (!fs.existsSync(uploadsDir)) restorePhotos()

const args = [
  require.resolve('json-server/lib/cli/bin'),
  'db.json',
  '--middlewares', 'middlewares.js',
  '--port', process.env.PORT ?? '3000',
  '--host', process.env.HOST ?? 'localhost',
  '--watch',
]
spawn(process.execPath, args, { cwd: root, stdio: 'inherit' }).on('exit', (code) => process.exit(code ?? 0))
