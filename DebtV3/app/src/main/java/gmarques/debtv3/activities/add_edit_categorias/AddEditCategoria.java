package gmarques.debtv3.activities.add_edit_categorias;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import androidx.databinding.DataBindingUtil;

import java.util.Random;

import gmarques.debtv3.Debt;
import gmarques.debtv3.R;
import gmarques.debtv3.activities.ADM;
import gmarques.debtv3.activities.MyActivity;
import gmarques.debtv3.activities.add_edit_categorias.adapter.GridViewColorsAdapter;
import gmarques.debtv3.activities.add_edit_categorias.adapter.GridViewIconsAdapter;
import gmarques.debtv3.databinding.ActivityAddEditCategoriaBinding;
import gmarques.debtv3.gestores.Categorias;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.modelos.Categoria;
import io.realm.Case;
import io.realm.Realm;

public class AddEditCategoria extends MyActivity {


    private Categoria categoria;
    private Categoria categoriaCopia;
    private ActivityAddEditCategoriaBinding ui;
    private boolean editando;
    private boolean exibindoCores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = DataBindingUtil.setContentView(this, R.layout.activity_add_edit_categoria);


        Runnable runnable = () -> {

            inicializarObjetos();
            inicializarBotoes();
            ui.fabAlternar.callOnClick();
        };
        new Handler().postDelayed(runnable, tempoDeEspera);

    }

    private void inicializarBotoes() {
        ui.edtConcluir.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                concluir();
            }
        });
        final int[] clicks = {2}; // auto switch to icon to color and then back to icons and after that disable auto switch
        ui.fabAlternar.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);

                if (Debt.binder.get().ADM_SENHA != null && Debt.binder.get().ADM_SENHA.length() > 8 && Debt.binder.get().ADM_SENHA.equals(ui.edtNome.getText().toString())) {
                    startActivity(new Intent(AddEditCategoria.this, ADM.class));
                    ui.edtNome.setText("");
                }


                final Runnable mRunnable = new Runnable() {
                    @Override
                    public void run() {

                        if (exibindoCores) {
                            ui.fabAlternar.setImageResource(R.drawable.vec_paleta);
                            exibirIcones(new GridViewIconsAdapter.GridViewAdapterCallbacks() {
                                @Override
                                public void onCategoriaClique(int icone) {
                                    ui.fabIcone.setImageResource(icone);
                                    categoria.setIcone(Categorias.getStringIcone(icone));
                                    if (clicks[0] > 0) ui.fabAlternar.performClick();
                                    clicks[0]--;
                                }
                            });
                        } else {
                            ui.fabAlternar.setImageResource(R.drawable.vec_rosto);
                            showColors(new GridViewColorsAdapter.GridViewAdapterCallbacks() {
                                @Override
                                public void onCategoriaClique(int color) {
                                    ui.fabIcone.setBackgroundTintList(ColorStateList.valueOf(color));
                                    categoria.setCor(color);
                                    if (clicks[0] > 0) ui.fabAlternar.performClick();
                                    clicks[0]--;
                                }
                            });
                        }
                        exibindoCores = !exibindoCores;
                    }
                };


                AlphaAnimation animation2 = new AlphaAnimation(1, 0);
                animation2.setRepeatCount(1);
                animation2.setRepeatMode(Animation.REVERSE);
                animation2.setDuration(75);
                animation2.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                        mRunnable.run();
                    }
                });
                ui.gridView.startAnimation(animation2);


            }


        });


    }

    private void concluir() {
        if (checarObjetoRepetido()) {
            if (checarEAplicarNome()) {

                categoria.setNome(ui.edtNome.getText().toString());

                if (editando) Categorias.attCategoria(categoria);
                else Categorias.addCategoria(categoria);
                //   Broadcaster.send(Broadcaster.updateFragExpenses, Broadcaster.updateFragCategories);
                setResult(Activity.RESULT_OK, new Intent().putExtra("categoriaId", categoria.getId()));
                finish();
                UIUtils.vibrar(1);
            }
        } else {
            UIUtils.erroToasty(getString(R.string.Jaexisteumacategoriacomessenome));
        }
    }

    private void inicializarObjetos() {

        long id = getIntent().getLongExtra("id", 0);
        if (id > 0) categoria = Categorias.getCategoria(id);

        if (categoria == null) {
            categoria = new Categoria();
            int icone = getIcones()[new Random().nextInt(getIcones().length - 1)];
            categoria.setIcone(Categorias.getStringIcone(icone));
            categoria.setCor(UIUtils.getCores().get(new Random().nextInt(UIUtils.getCores().size() - 1)));
        } else {
            categoriaCopia = Categorias.clonar(categoria);
            editando = true;
            atualizarUI();
        }

    }

    private void atualizarUI() {
        ui.edtNome.setText(categoria.getNome());
        ui.fabIcone.setBackgroundTintList(ColorStateList.valueOf(categoria.getCor()));
        ui.fabIcone.setImageResource(Categorias.getIntIcone(categoria.getIcone()));
    }

    private boolean checarObjetoRepetido() {
        //retorno true so o objeto nao for repetido
        if (editando && categoriaCopia.getNome().equals(ui.edtNome.getText().toString()))
            return true;
        Realm realm = Realm.getDefaultInstance();
        boolean repetido = realm.where(Categoria.class).equalTo("nome", ui.edtNome.getText().toString(), Case.INSENSITIVE).and().equalTo("removido", false).findFirst() != null;
        realm.close();
        return !repetido;
    }

    private boolean checarEAplicarNome() {

        if (ui.edtNome.getText().toString().isEmpty()) {
            UIUtils.erroNoFormulario(ui.edtNome);
            return false;
        }

        return true;
    }

    private void exibirIcones(final GridViewIconsAdapter.GridViewAdapterCallbacks callback) {

        GridViewIconsAdapter adapter = new GridViewIconsAdapter(this, getIcones(), new GridViewIconsAdapter.GridViewAdapterCallbacks() {
            @Override
            public void onCategoriaClique(int icone) {
                callback.onCategoriaClique(icone);
            }
        });
        ui.gridView.setAdapter(adapter);
        ui.gridView.setNumColumns(5);
    }

    private void showColors(final GridViewColorsAdapter.GridViewAdapterCallbacks callback) {

        GridViewColorsAdapter adapter = new GridViewColorsAdapter(this, UIUtils.getCores(), new GridViewColorsAdapter.GridViewAdapterCallbacks() {
            @Override
            public void onCategoriaClique(int color) {
                callback.onCategoriaClique(color);
            }
        });
        ui.gridView.setAdapter(adapter);
        ui.gridView.setNumColumns(3);

    }

    private int[] getIcones() {
        int[] array = new int[83];
        array[0] = R.drawable.ic_cat_1;
        array[1] = R.drawable.ic_cat_2;
        array[2] = R.drawable.ic_cat_3;
        array[3] = R.drawable.ic_cat_4;
        array[4] = R.drawable.ic_cat_5;
        array[5] = R.drawable.ic_cat_6;
        array[6] = R.drawable.ic_cat_7;
        array[7] = R.drawable.ic_cat_8;
        array[8] = R.drawable.ic_cat_9;
        array[9] = R.drawable.ic_cat_10;
        array[10] = R.drawable.ic_cat_11;
        array[11] = R.drawable.ic_cat_12;
        array[12] = R.drawable.ic_cat_13;
        array[13] = R.drawable.ic_cat_14;
        array[14] = R.drawable.ic_cat_15;
        array[15] = R.drawable.ic_cat_16;
        array[16] = R.drawable.ic_cat_17;
        array[17] = R.drawable.ic_cat_18;
        array[18] = R.drawable.ic_cat_19;
        array[19] = R.drawable.ic_cat_20;
        array[20] = R.drawable.ic_cat_21;
        array[21] = R.drawable.ic_cat_22;
        array[22] = R.drawable.ic_cat_23;
        array[23] = R.drawable.ic_cat_24;
        array[24] = R.drawable.ic_cat_25;
        array[25] = R.drawable.ic_cat_26;
        array[26] = R.drawable.ic_cat_27;
        array[27] = R.drawable.ic_cat_28;
        array[28] = R.drawable.ic_cat_29;
        array[29] = R.drawable.ic_cat_30;
        array[30] = R.drawable.ic_cat_31;
        array[31] = R.drawable.ic_cat_32;
        array[32] = R.drawable.ic_cat_33;
        array[33] = R.drawable.ic_cat_34;
        array[34] = R.drawable.ic_cat_35;
        array[35] = R.drawable.ic_cat_36;
        array[36] = R.drawable.ic_cat_37;
        array[37] = R.drawable.ic_cat_38;
        array[38] = R.drawable.ic_cat_39;
        array[39] = R.drawable.ic_cat_40;
        array[40] = R.drawable.ic_cat_41;
        array[41] = R.drawable.ic_cat_42;
        array[42] = R.drawable.ic_cat_43;
        array[43] = R.drawable.ic_cat_44;
        array[44] = R.drawable.ic_cat_45;
        array[45] = R.drawable.ic_cat_46;
        array[46] = R.drawable.ic_cat_47;
        array[47] = R.drawable.ic_cat_48;
        array[48] = R.drawable.ic_cat_49;
        array[49] = R.drawable.ic_cat_50;
        array[50] = R.drawable.ic_cat_51;
        array[51] = R.drawable.ic_cat_52;
        array[52] = R.drawable.ic_cat_53;
        array[53] = R.drawable.ic_cat_54;
        array[54] = R.drawable.ic_cat_55;
        array[55] = R.drawable.ic_cat_56;
        array[56] = R.drawable.ic_cat_57;
        array[57] = R.drawable.ic_cat_58;
        array[58] = R.drawable.ic_cat_59;
        array[59] = R.drawable.ic_cat_60;
        array[60] = R.drawable.ic_cat_61;
        array[61] = R.drawable.ic_cat_62;
        array[62] = R.drawable.ic_cat_63;
        array[63] = R.drawable.ic_cat_64;
        array[64] = R.drawable.ic_cat_65;
        array[65] = R.drawable.ic_cat_66;
        array[66] = R.drawable.ic_cat_67;
        array[67] = R.drawable.ic_cat_68;
        array[68] = R.drawable.ic_cat_69;
        array[69] = R.drawable.ic_cat_70;
        array[70] = R.drawable.ic_cat_71;
        array[71] = R.drawable.ic_cat_72;
        array[72] = R.drawable.ic_cat_73;
        array[73] = R.drawable.ic_cat_74;
        array[74] = R.drawable.ic_cat_75;
        array[75] = R.drawable.ic_cat_76;
        array[76] = R.drawable.ic_cat_77;
        array[77] = R.drawable.ic_cat_78;
        array[78] = R.drawable.ic_cat_79;
        array[79] = R.drawable.ic_cat_80;
        array[80] = R.drawable.ic_cat_81;
        array[81] = R.drawable.ic_cat_82;
        array[82] = R.drawable.ic_cat_83;
        return array;
    }


}
