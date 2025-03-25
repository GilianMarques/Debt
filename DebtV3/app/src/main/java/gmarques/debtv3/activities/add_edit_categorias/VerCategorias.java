package gmarques.debtv3.activities.add_edit_categorias;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.databinding.DataBindingUtil;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import gmarques.debtv3.R;
import gmarques.debtv3.activities.MyActivity;
import gmarques.debtv3.databinding.ActivityGerenciarCategoriasBinding;
import gmarques.debtv3.gestores.Categorias;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.modelos.Categoria;
import gmarques.debtv3.novo_bsheet.Sheettalogo;


public class VerCategorias extends MyActivity {
    private CategoriasAdapter adapter;
    private ActivityGerenciarCategoriasBinding ui;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = DataBindingUtil.setContentView(this, R.layout.activity_gerenciar_categorias);
        setSupportActionBar(ui.toolbar);
        ActionBar actBar = getSupportActionBar();
        assert actBar != null;
        actBar.setTitle(getString(R.string.Vercategorias));
        inicializarBotoes();
        new Handler().postDelayed(this::inicializarRecyclerView, tempoDeEspera);

    }

    private void inicializarRecyclerView() {
        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this);
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setJustifyContent(JustifyContent.SPACE_AROUND);

        ui.rv.setLayoutManager(layoutManager);

        adapter = new CategoriasAdapter(new CategoriasAdapter.Callback() {
            @Override
            public void onClick(Categoria categoria, int adapterPosition) {
                startActivity(new Intent(VerCategorias.this, AddEditCategoria.class).putExtra("id", categoria.getId()));
            }

            @Override
            public void onLongClick(Categoria categoria, int adapterPosition) {
                if (Categorias.categoriaEstaEmUso(categoria)) {
                    UIUtils.erroToasty(getString(R.string.Estacategoriaestaemusoportantonaopodeserremovida));
                } else confirmarRemocao(categoria, adapterPosition);
            }
        });
        ui.rv.setAdapter(adapter);
        adapter.update();
    }


    private void inicializarBotoes() {
        ui.fabAdd.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                startActivity(new Intent(VerCategorias.this, AddEditCategoria.class));
            }
        });

    }

    private void confirmarRemocao(final Categoria categoria, final int adapterPosition) {
        new Sheettalogo(this)
                .titulo(getString(R.string.Porfavorconfirme))
                .mensagem(getString(R.string.Desejamesmoremoverestacategoria))
                .botaoPositivo(getString(R.string.Cancelar), null)
                .botaoNegativo(getString(R.string.Remover), v -> {
                    Categorias.remover(categoria);
                    adapter.remove(adapterPosition);
                })
                .show();
    }

    @Override
    protected void onResume() {
        if (adapter != null) adapter.update();
        super.onResume();
    }
}
