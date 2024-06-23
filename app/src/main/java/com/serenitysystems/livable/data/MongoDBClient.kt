package com.serenitysystems.livable.data

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document


object MongoDBClient {
    private const val connectionString = "mongodb+srv://SerenitySystems:<livable>@livable.bqtfktt.mongodb.net"
    val client = MongoClients.create(connectionString)
    val database: MongoDatabase = client.getDatabase("LivableDatabase")

    val usersCollection: MongoCollection<Document> = database.getCollection("users")



}
