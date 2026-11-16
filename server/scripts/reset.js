const { restoreDatabase, restorePhotos } = require('./seed')

// Back to the seed, with a new databaseId so apps resync from scratch
restoreDatabase({ newDatabaseId: true })
restorePhotos()
console.log('Database and photos restored from the seed with a new databaseId')
