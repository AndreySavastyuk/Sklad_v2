package com.example.myprinterapp.data

// В com.example.myprinterapp.data или аналогичном месте
enum class TaskStatus { NEW, IN_PROGRESS, COMPLETED, CANCELED }

data class PickDetail(
    val id: Int, // Или String
    val partNumber: String,
    val partName: String,
    var quantityToPick: Int,
    val location: String, // Место хранения детали
    var picked: Int = 0
)

data class PickTask(
    val id: String,
    val date: String, // Или LocalDate
    val description: String,
    val status: TaskStatus,
    val details: List<PickDetail>
)

