package gmarques.debtv3.especificos;

import android.os.Environment;
import android.util.Log;

import org.joda.time.LocalDateTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import gmarques.debtv3.Debt;
import gmarques.debtv3.outros.Tag;
import io.realm.Realm;

public class RealmBackup {

    public File getBackupPasta() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + File.separator + "Debt Backups", "");
    }

    public RealmBackup() {
    }

    public void fazerBackup(String arquivoQueSeraRestaurado) {
        Realm realm = Realm.getDefaultInstance();

        File pasta = getBackupPasta();

        if (!pasta.exists()) pasta.mkdirs();

        String nomeBackup = new LocalDateTime().toString("dd-MM-YYYY HH:mm:ss (SSS)");

        if (Debt.MODO_DE_TESTES)
            nomeBackup = "testes " + nomeBackup;

        if (arquivoQueSeraRestaurado != null)
            nomeBackup = nomeBackup + "\n" + "backup pré restauração para: " + arquivoQueSeraRestaurado.split("\\.")[0];

        nomeBackup += ".realm";
        File arquivoBackup = new File(pasta, nomeBackup);


        Log.d(Tag.AppTag, "ReamBackup.backup: caminho do realm: " + realm.getPath());

        realm.writeCopyTo(arquivoBackup);
        Log.d(Tag.AppTag, "ReamBackup.backup: backup criado.");
        realm.close();
    }

    public void restaurar(File arquivo) {

        String caminhRealm;
        Realm realm = Realm.getDefaultInstance();
        caminhRealm = realm.getPath();
        realm.close();

        Log.d(Tag.AppTag, "RealmBackup.restaurar entrada: " + arquivo.getAbsolutePath());
        Log.d(Tag.AppTag, "RealmBackup.restaurar   saida: " + caminhRealm);

        try {
            File realmArquivo = new File(caminhRealm, "");


            FileOutputStream outputStream = new FileOutputStream(realmArquivo);

            FileInputStream inputStream = new FileInputStream(arquivo);

            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, bytesRead);
            }
            outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
