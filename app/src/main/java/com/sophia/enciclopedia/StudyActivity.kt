package com.sophia.enciclopedia

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sophia.domain.StudySource
import com.sophia.domain.ThemeMode
import com.sophia.enciclopedia.ui.*

class StudyActivity:ComponentActivity(){
    val model:StudyViewModel by viewModels{viewModelFactory{initializer{StudyViewModel((application as SophiaApplication).container.study,createSavedStateHandle())}}}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(savedInstanceState==null&&intent.hasExtra("sourceExcerpt"))model.new(StudySource(intent.getStringExtra("sourceTitle").orEmpty(),intent.getStringExtra("sourceLocator").orEmpty(),intent.getStringExtra("sourceExcerpt").orEmpty()));setContent{SophiaTheme(ThemeMode.SYSTEM){Surface{Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()){TextButton(onClick={finish()},modifier=Modifier.padding(horizontal=8.dp)){Text("Voltar ao leitor")};StudyScreen(model)}}}}}
    companion object{fun intent(context:Context,source:StudySource)=Intent(context,StudyActivity::class.java).putExtra("sourceTitle",source.title.take(300)).putExtra("sourceLocator",source.locator.take(1000)).putExtra("sourceExcerpt",source.excerpt.take(6500))}
}
