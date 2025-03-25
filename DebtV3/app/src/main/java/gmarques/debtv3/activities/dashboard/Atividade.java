package gmarques.debtv3.activities.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;

import org.jetbrains.annotations.NotNull;

import gmarques.debtv3.R;
import gmarques.debtv3.activities.MyActivity;
import gmarques.debtv3.activities.Transacoes;
import gmarques.debtv3.activities.ver_despesas.VerDespesas;
import gmarques.debtv3.activities.ver_receitas.VerReceitas;
import gmarques.debtv3.databinding.FragmentAtividadeBinding;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.outros.MyFragment;


public class Atividade extends MyFragment {
    private FragmentAtividadeBinding ui;
    private MyActivity activity;

    public Atividade() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ui = DataBindingUtil.inflate(inflater, R.layout.fragment_atividade, container, false);
        activity = (MyActivity) getActivity();
        return ui.getRoot();
    }


    @Override
    protected void atualizar() {
        inicializar();
    }

    @Override
    protected String getAçaoBroadcast() {
        return "null";
    }

    @Override
    protected void inicializar() {

        ui.cvDespesa.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);


                activity.startActivity(new Intent(activity, VerDespesas.class));

            }
        });

        ui.cvReceita.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);

                activity.startActivity(new Intent(activity, VerReceitas.class));
            }
        });

        ui.cvTransacoes.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);

                activity.startActivity(new Intent(activity, Transacoes.class));
            }
        });


    }


}
