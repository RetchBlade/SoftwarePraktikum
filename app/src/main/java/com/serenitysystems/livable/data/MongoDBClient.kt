package com.serenitysystems.livable.data

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import org.bson.Document

object MongoDBClient {
    private const val connectionString = "mongodb+srv://SerenitySystems:livable@livable.bqtfktt.mongodb.net"
    val client = MongoClients.create(connectionString)
    val database = client.getDatabase("LivableDatabase")
    val usersCollection: MongoCollection<Document> = database.getCollection("users")

    fun authenticateUser(username: String, password: String): Boolean {
        val query = Document("username", username).append("password", password)
        val user = usersCollection.find(query).first()
        return user != null
    }
}
