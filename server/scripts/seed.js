const fs = require('node:fs')
const path = require('node:path')
const { randomUUID } = require('node:crypto')

// Paths, all relative to the server folder
const root = path.join(__dirname, '..')
const seedFile = path.join(root, 'db.seed.json')
const dbFile = path.join(root, 'db.json')
const seedPhotosDir = path.join(root, 'seed-photos')
const uploadsDir = path.join(root, 'public', 'uploads')

// Copies the committed seed over the live db.json
function restoreDatabase({ newDatabaseId }) {
  const db = JSON.parse(fs.readFileSync(seedFile, 'utf8'))
  // A new databaseId tells connected apps to throw away their copy and resync
  if (newDatabaseId) db.meta.databaseId = randomUUID()
  fs.writeFileSync(dbFile, JSON.stringify(db, null, 2) + '\n')
}

// Replaces the live uploads with the committed seed photos
function restorePhotos() {
  fs.rmSync(uploadsDir, { recursive: true, force: true })
  fs.cpSync(seedPhotosDir, uploadsDir, { recursive: true })
}

module.exports = { root, seedFile, dbFile, uploadsDir, restoreDatabase, restorePhotos }
