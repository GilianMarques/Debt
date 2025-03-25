package gmarques.debtv3.especificos;


import android.os.Handler;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import gmarques.debtv3.outros.UIRunnable;

public class ConexaoComAInternet {


    private boolean conectado;
    private Callback callback;

    public void verificar(Callback c) {
        iniciarTimer();
        this.callback = c;
        new Thread(new UIRunnable() {
            @Override
            public void workerThread() {
                try {
                    HttpURLConnection urlc = (HttpURLConnection) (new URL("https://clients3.google.com/generate_204").openConnection());
                    conectado = urlc.getResponseCode() == 204 && urlc.getContentLength() == 0;
                } catch (IOException e) {
                    e.printStackTrace();
                    conectado = false;
                }
            }

            @Override
            public void uiThread() {
                //solicitaçao expirou
                if (callback == null) return;
                callback.conclusao(conectado);
                callback = null;
            }
        }).start();
    }

    /**
     * Dou 10 segundos pra solicitaçao ser concluida, se nao assumo que o usuario esta sem internet
     */
    private void iniciarTimer() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {

                    callback.conclusao(false);
                    callback = null;
                }
            }
        }, 10 * 1000/*101 segundos*/);
    }

    public interface Callback {
        void conclusao(boolean conectado);
    }
}
