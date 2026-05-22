package com.example.accomlink.models

enum class UserRole(val firestoreValue: String, val displayName: String) {
    Student("student", "Student"),
    Landlord("landlord", "Landlord");

    companion object {
        fun from(value: String?): UserRole =
            entries.firstOrNull { it.firestoreValue.equals(value, ignoreCase = true) } ?: Student
    }
}
