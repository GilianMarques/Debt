package gmarques.debtv3.outros;

import gmarques.debtv3.Debt;

public abstract class UIRunnable implements Runnable {

    @Override
    public final void run() {
        workerThread();
        /*optei por nao implementar o runOnUiThread nessa classe para evitar codigo duplicado*/
        Debt.binder.get().runOnUiThread(this::uiThread);
    }

    public abstract void workerThread();

    public abstract void uiThread();
}
