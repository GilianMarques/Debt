package gmarques.debtv3.utilitarios;

import android.os.Build;

import com.pixplicity.easyprefs.library.Prefs;

import gmarques.debtv3.especificos.Data;

public class GestorId {

    private static long idDoDispositivo;

    public static long getIdDoDispositivo() {
        if (idDoDispositivo == 0) {

            idDoDispositivo = Prefs.getLong("idDisp", 0);

            if (idDoDispositivo == 0) {
                idDoDispositivo = (getId());
                Prefs.putLong("idDisp", idDoDispositivo);
            }


        }
        return idDoDispositivo;
    }


    /**
     * Retorna uma id unica baseada em timeStamp
     * Garante que a id seja unica , segurando a thread até que
     * Data.timeStampPadrao() retorne uma valor diferente do
     * valor da variavel id (agurda 1 mls), porem, sua execução limita a
     * geraçao de ids a 1 id por milissegundo o que pode nao ser bom para
     * app que exigem alta performance.
     */
    public synchronized static long getId() {
        long id = Data.timeStampUTC();
        //noinspection StatementWithEmptyBody
        while (Data.timeStampUTC() == id) ;
        return id;


    }

    /*FirebaseImpl usa esse metodo para agrupar relatorios de erros, evite modificar as informaçoes retornadas por ele*/
    public static String getInfoDoAparelho() {
        return Build.MANUFACTURER + " - " + Build.MODEL + ", OS " + Build.VERSION.RELEASE;
    }
}
