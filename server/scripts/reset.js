const { restoreDatabase, restorePhotos } = require('./seed')

restoreDatabase({ newDatabaseId: true })
restorePhotos()
console.log('Database and photos restored from the seed with a new databaseId')
