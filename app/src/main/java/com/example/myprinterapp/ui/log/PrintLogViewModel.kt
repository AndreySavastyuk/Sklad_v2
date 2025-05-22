package com.example.myprinterapp.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myprinterapp.data.repo.PrintLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PrintLogViewModel @Inject constructor(
    repo: PrintLogRepository
) : ViewModel() {

    val entries = repo.logFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
}