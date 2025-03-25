package gmarques.debtv3.callbacks;

public interface EncerrarSincronismoCallback {

    void sincronismoEncerrado();

    void falhaAoEncerrarSincronismo(String erro);

    void sincronismoEncerradoComRessalva(String erro);
}
