package com.serenitysystems.livable.data

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import org.bson.Document

object MongoDBClient {
    private const val connectionString = "mongodb+srv://SerenitySystems:<BwfAH1al8iZ6EjjU>@livable.bqtfktt.mongodb.net/?retryWrites=true&w=majorit"
    private val client = MongoClients.create(connectionString)
    private val database = client.getDatabase("LivableDatabase")
    private val usersCollection: MongoCollection<Document> = database.getCollection("users")

    fun authenticateUser(username: String, password: String): Boolean {
        val query = Document("username", username).append("password", password)
        val user = usersCollection.find(query).first()
        return user != null
    }

    fun registerUser(email: String, username: String, password: String, birthdate: String, gender: String): Boolean {
        val user = Document("email", email)
            .append("username", username)
            .append("password", password)
            .append("birthdate", birthdate)
            .append("gender", gender)

        try {
            usersCollection.insertOne(user)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
