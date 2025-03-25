package gmarques.debtv3.activities;

import android.os.Bundle;
import android.widget.CompoundButton;

import androidx.databinding.DataBindingUtil;

import com.pixplicity.easyprefs.library.Prefs;

import gmarques.debtv3.R;
import gmarques.debtv3.databinding.ActivityADMBinding;
import gmarques.debtv3.utilitarios.C;

public class ADM extends MyActivity {
    private ActivityADMBinding ui;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = DataBindingUtil.setContentView(this, R.layout.activity_a_d_m);
        setSupportActionBar(ui.toolbar);
        inicializarSwitches();
    }

    private void inicializarSwitches() {
        ui.sAdm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Prefs.putBoolean(C.administrador, isChecked);
            }
        });

        ui.sAds.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Prefs.putBoolean(C.desligarAnuncios, isChecked);
            }
        });

        ui.sAdm.setChecked(Prefs.getBoolean(C.administrador, false));
        ui.sAds.setChecked(Prefs.getBoolean(C.desligarAnuncios, false));
    }
}