const sensorDb = db.getSiblingDB("sensors")
if (sensorDb.user.countDocuments({"username": "admin"}) === 0) {
    console.log("Creating root account");
    sensorDb.user.insertOne({
        "username": "admin",
        "hashedPassword": "$2a$12$LyP8i.Q.VpjHVeMUZGfDfOvZiyPrdCcN/F5Am6.A7XMFP68eAVhYa"
    })
}

