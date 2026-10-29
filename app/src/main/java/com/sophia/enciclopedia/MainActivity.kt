package com.sophia.enciclopedia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sophia.enciclopedia.ui.SophiaApp

class MainActivity:ComponentActivity(){
    val model:SophiaViewModel by viewModels{viewModelFactory{initializer{
        val container=(application as SophiaApplication).container
        SophiaViewModel(container.catalog,container.notes,container.preferences,createSavedStateHandle())
    }}}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{SophiaApp(model)}}
}
