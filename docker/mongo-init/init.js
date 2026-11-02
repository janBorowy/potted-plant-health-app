const sensorDb = db.getSiblingDB("sensors")
if (sensorDb.user.countDocuments({"username": "admin"}) === 0) {
    console.log("Creating root account");
    sensorDb.user.insertOne({
        "username": "admin",
        "hashedPassword": "$2a$12$uVPb0oTKzhwCen637394CuCRIDyjGAMDbndi5P3gqWmHOD5Fm56Z."
    })
}

