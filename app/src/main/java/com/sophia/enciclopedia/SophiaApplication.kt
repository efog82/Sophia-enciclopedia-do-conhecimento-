package com.sophia.enciclopedia

import android.app.Application
import android.content.Context
import com.sophia.data.*
import com.sophia.domain.*

class SophiaApplication:Application(){val container by lazy{AppContainer(this)}}
class AppContainer(context:Context){
    val database by lazy{SophiaDatabase.open(context)}
    val catalog:CatalogRepository by lazy{RoomCatalogRepository(database)}
    val notes:NotesRepository by lazy{RoomNotesRepository(database)}
    val preferences:PreferencesRepository by lazy{DataStorePreferences(context)}
}
