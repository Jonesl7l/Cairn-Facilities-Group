const fs = require('node:fs')
const path = require('node:path')
const { root, seedFile } = require('./seed')

// The app's bundled reference data, used before its first sync
const target = path.join(root, '..', 'android', 'app', 'src', 'main', 'assets', 'reference.json')
const db = JSON.parse(fs.readFileSync(seedFile, 'utf8'))
// Same shape as GET /reference
const reference = {
  sites: db.sites,
  buildings: db.buildings,
  floors: db.floors,
  rooms: db.rooms,
  assets: db.assets,
  technicians: db.users.filter((user) => user.role === 'TECHNICIAN').map(({ password, ...user }) => user),
}

// Writes one object per line, the format the app's file already uses
const line = (item) => `{ ${Object.entries(item).map(([key, value]) => `${JSON.stringify(key)}: ${JSON.stringify(value)}`).join(', ')} }`
const section = ([name, items]) => `  ${JSON.stringify(name)}: [\n${items.map((item) => `    ${line(item)}`).join(',\n')}\n  ]`
fs.writeFileSync(target, `{\n${Object.entries(reference).map(section).join(',\n')}\n}\n`)
console.log(`Wrote ${path.relative(process.cwd(), target)}`)
