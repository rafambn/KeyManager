package com.rafambn.keymanager.ui.main

sealed interface AppEvent

data class ShowError(val message: String) : AppEvent