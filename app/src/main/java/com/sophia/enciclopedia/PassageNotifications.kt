package com.sophia.enciclopedia

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sophia.domain.Passage
import com.sophia.domain.PassageSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/** Notificação diária com uma passagem dos livros-base. Agenda com AlarmManager e reage ao arranque do telemóvel. */
object PassageNotifications {
    const val CHANNEL_ID="passagens-diarias"
    private const val REQUEST_CODE=4201
    private const val NOTIFICATION_ID=4202
    private const val PREFS="passage_schedule"

    fun createChannel(context:Context){
        val manager=context.getSystemService(NotificationManager::class.java)
        if(manager.getNotificationChannel(CHANNEL_ID)==null){
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID,"Passagem diária",NotificationManager.IMPORTANCE_DEFAULT).apply{description="Uma passagem curta dos livros-base, uma vez por dia, à hora que escolheres."})
        }
    }
    /** Guarda o espelho das definições (leitura síncrona para receivers) e (re)agenda. Chamado sempre que as definições mudam e quando a app arranca. */
    fun sync(context:Context,settings:PassageSettings){
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
            .putBoolean("enabled",settings.enabled).putBoolean("automatic",settings.automatic)
            .putInt("hour",settings.hour).putInt("minute",settings.minute).apply()
        if(settings.enabled) scheduleNext(context) else cancel(context)
    }
    fun currentSettings(context:Context):PassageSettings {
        val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        return PassageSettings(prefs.getBoolean("enabled",false),prefs.getBoolean("automatic",true),prefs.getInt("hour",9),prefs.getInt("minute",0))
    }
    private fun effectiveTime(settings:PassageSettings):Pair<Int,Int> = if(settings.automatic) 9 to 0 else settings.hour to settings.minute
    fun nextTriggerMillis(settings:PassageSettings,now:Long=System.currentTimeMillis()):Long {
        val (hour,minute)=effectiveTime(settings)
        val cal=Calendar.getInstance().apply{timeInMillis=now;set(Calendar.HOUR_OF_DAY,hour);set(Calendar.MINUTE,minute);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}
        if(cal.timeInMillis<=now) cal.add(Calendar.DAY_OF_YEAR,1)
        return cal.timeInMillis
    }
    fun scheduleNext(context:Context){
        val settings=currentSettings(context)
        if(!settings.enabled) return
        val alarm=context.getSystemService(AlarmManager::class.java)
        val intent=PendingIntent.getBroadcast(context,REQUEST_CODE,Intent(context,PassageReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val at=nextTriggerMillis(settings)
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S&&alarm.canScheduleExactAlarms()) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,intent)
        else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,intent)
    }
    fun cancel(context:Context){
        val alarm=context.getSystemService(AlarmManager::class.java)
        val intent=PendingIntent.getBroadcast(context,REQUEST_CODE,Intent(context,PassageReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarm.cancel(intent)
    }
    fun canUseExactAlarms(context:Context):Boolean {
        val alarm=context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT<Build.VERSION_CODES.S||alarm.canScheduleExactAlarms()
    }
    fun post(context:Context,passage:Passage){
        createChannel(context)
        if(!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        // Tanto tocar na notificação como no botão "Ler texto completo" abre a app já no
        // capítulo da passagem (MainActivity entrega o destino ao SophiaApp via PassageDeepLink).
        val target=Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(PassageDeepLink.EXTRA_BOOK,passage.bookId).putExtra(PassageDeepLink.EXTRA_UNIT,passage.unitIndex)
        val open=PendingIntent.getActivity(context,0,target,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val readFull=PendingIntent.getActivity(context,4203,target,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=NotificationCompat.Builder(context,CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_passage)
            .setContentTitle("Passagem para refletir · ${passage.bookTitle}")
            .setContentText(passage.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${passage.text}\n\n— ${passage.bookTitle} · ${passage.unitTitle}"))
            .addAction(android.R.drawable.ic_menu_view,"Ler texto completo",readFull)
            .setContentIntent(open).setAutoCancel(true).build()
        try{NotificationManagerCompat.from(context).notify(NOTIFICATION_ID,notification)}catch(e:SecurityException){/* permissão revogada entretanto */}
    }
}

class PassageReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        val settings=PassageNotifications.currentSettings(context)
        if(!settings.enabled) return
        val pending=goAsync()
        CoroutineScope(Dispatchers.IO).launch{
            try{
                val app=(context.applicationContext as SophiaApplication).container
                app.passages.dailyPassage()?.let{PassageNotifications.post(context,it)}
            }catch(e:Exception){/* sem passagem hoje; reagenda na mesma */}
            finally{PassageNotifications.scheduleNext(context);pending.finish()}
        }
    }
}

class PassageBootReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        PassageNotifications.scheduleNext(context)
    }
}
