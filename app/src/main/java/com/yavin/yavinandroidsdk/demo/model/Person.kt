package com.yavin.yavinandroidsdk.demo.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Person(
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    @SerialName("age")
    val age: Int
)


fun Person.toText(): String {
    return "$firstName $lastName $age"
}