package gmarques.debtv3.outros;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import gmarques.debtv3.outros.Broadcaster;

public abstract class MyFragment extends Fragment {
    private Callback callback;


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        inicializar();
        String açao = getAçaoBroadcast();
        Broadcaster.registrar(açao, new Broadcaster.Callback() {
            @Override
            public void execute() {
                atualizar();
            }
        });
    }

    protected abstract void atualizar();

    protected abstract String getAçaoBroadcast();

    protected abstract void inicializar();

    @Override
    public void onDestroyView() {
        if (callback != null) callback.fechado();
        super.onDestroyView();
    }

    public void setOnDestroyCallback(Callback callback) {
        this.callback = callback;
    }

    public interface Callback {
        void fechado();
    }


}
