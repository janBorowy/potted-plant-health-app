db.user.createIndex({ username: 1 }, { unique: true })
db.series.createIndex({ name: 1 }, { unique: true })
db.jwt_tokens.createIndex({ jti: 1 }, { unique: true })
