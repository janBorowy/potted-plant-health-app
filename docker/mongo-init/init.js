const sensorDb = db.getSiblingDB("sensors")
if (sensorDb.user.countDocuments({"username": "admin"}) === 0) {
    console.log("Creating root account");
    sensorDb.user.insertOne({
        "username": "admin",
        "hashedPassword": "$2a$12$mAH0P14dp16S4TIeyPV0DOS2VVAGgwHiE0m3NORxjJgUGG4k3iJcm"
    })
}

