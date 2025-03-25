package gmarques.debtv3.broadcast_e_servicos;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_NO_CREATE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

import gmarques.debtv3.outros.Tag;


/**
 * Criado por  Gilian Marques em 11/12/2016.
 */

public class GestorDeAlarmes {

    private final Context mContext;
    private AlarmManager alarmManager;

    public GestorDeAlarmes(Context mContext) {
        this.mContext = mContext;
        alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
    }

    public void ligarNotificaçoes() {
        Calendar calendar = Calendar.getInstance();

        PendingIntent pendingIntent = getNotificaçaoI();


        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 6);  // seta o horario em uma Instancia de Calendar
        calendar.set(Calendar.MINUTE, 0);

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);

        //
        pendingIntent = getTardeI();

        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 16);
        calendar.set(Calendar.MINUTE, 0);

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
        //

        pendingIntent = getRelatorioDoDiaI();

        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 19);
        calendar.set(Calendar.MINUTE, 30);

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);

    }

    /**
     * Desliga as notificaçoes
     */

    @SuppressWarnings("unused")
    public void desligarNotificaçoes() {
        alarmManager.cancel(getNotificaçaoI());
        getNotificaçaoI().cancel();
        alarmManager.cancel(getTardeI());
        getTardeI().cancel();
        alarmManager.cancel(getRelatorioDoDiaI());
        getRelatorioDoDiaI().cancel();
    }

    /**
     * Checa o estado das notificaçoes
     *
     * @return true se a notificaçoes estiverem ligadas
     */
    public boolean estaoAsNotificaçoesLigadas() {

        boolean a = (PendingIntent.getBroadcast(mContext, 1001, new Intent(mContext, AppBroadcast.class).setAction("notificar_manha"), FLAG_NO_CREATE | FLAG_IMMUTABLE) != null);//just changed the flag
        boolean b = (PendingIntent.getBroadcast(mContext, 1002, new Intent(mContext, AppBroadcast.class).setAction("notificar_tarde"), FLAG_NO_CREATE | FLAG_IMMUTABLE) != null);//just changed the flag
        boolean c = (PendingIntent.getBroadcast(mContext, 1003, new Intent(mContext, AppBroadcast.class).setAction("notificar_relatorio_do_dia"), FLAG_NO_CREATE | FLAG_IMMUTABLE) != null);//just changed the flag


        Log.d(Tag.AppTag + "MyAlarmManager", "isNotificationsEnabled: Notificaçoes estão " + (b ? "Habilitadas" : "Desabilitadas"));
        return a && b && c;
    }


    //


    public void alternarAutoSinc(Boolean enable) {
        if (!enable) alarmManager.cancel(getSincronismoI());
        else {
            int intervalHours = 3/*horas*/ * 60 * 60 * 1000;
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), intervalHours, getSincronismoI());
        }
    }

    //


    private PendingIntent getNotificaçaoI() {
        Intent notifyIntent = new Intent(mContext, AppBroadcast.class).setAction("notificar_manha");
        return PendingIntent.getBroadcast(mContext, 1001, notifyIntent, FLAG_CANCEL_CURRENT | FLAG_IMMUTABLE);
    }

    private PendingIntent getTardeI() {
        Intent notifyIntent = new Intent(mContext, AppBroadcast.class).setAction("notificar_tarde");
        return PendingIntent.getBroadcast(mContext, 1002, notifyIntent, FLAG_CANCEL_CURRENT | FLAG_IMMUTABLE);
    }

    private PendingIntent getRelatorioDoDiaI() {
        Intent notifyIntent = new Intent(mContext, AppBroadcast.class).setAction("notificar_relatorio_do_dia");
        return PendingIntent.getBroadcast(mContext, 1003, notifyIntent, FLAG_CANCEL_CURRENT | FLAG_IMMUTABLE);
    }

    private PendingIntent getSincronismoI() {
        Intent notifyIntent = new Intent(mContext, AppBroadcast.class).setAction("sinc");
        return PendingIntent.getBroadcast(mContext, 1004, notifyIntent, FLAG_CANCEL_CURRENT | FLAG_IMMUTABLE);
    }

}