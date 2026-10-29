package com.sophia.enciclopedia

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sophia.enciclopedia.ui.ReaderScreen

class ReaderActivity:ComponentActivity(){
    val model:ReaderViewModel by viewModels{viewModelFactory{initializer{
        val c=(application as SophiaApplication).container
        ReaderViewModel(c.library,c.notes,c.preferences,intent.getStringExtra("fileId")?:"",intent.getStringExtra("locator"))
    }}}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{ReaderScreen(model){finish()}}}
    companion object{fun intent(context:Context,id:String,locator:String?=null)=Intent(context,ReaderActivity::class.java).putExtra("fileId",id).putExtra("locator",locator)}
}
