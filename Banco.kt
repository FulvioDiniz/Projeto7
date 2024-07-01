package com.example.projeto7_1806

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object Banco {
    private val database = FirebaseDatabase.getInstance()

    fun getReference(path: String) = database.getReference(path)
}
