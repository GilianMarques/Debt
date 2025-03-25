package gmarques.debtv3.activities.add_edit_categorias.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import java.math.BigDecimal;

import gmarques.debtv3.Debt;
import gmarques.debtv3.R;
import gmarques.debtv3.interface_.UIUtils;


/**
 * Criado por Gilian Marques em 16/01/2017.
 */

public class GridViewIconsAdapter extends BaseAdapter {

    private final Context mContext;
    private final int[] lista;
    private final GridViewAdapterCallbacks callbacks;
    private int color = ContextCompat.getColor(Debt.binder.get(), R.color.colorPrimaryDark);

    public GridViewIconsAdapter(Context mContext, int[] lista, GridViewAdapterCallbacks callbacks) {
        this.mContext = mContext;
        this.lista = lista;
        this.callbacks = callbacks;
    }

    @Override
    public int getCount() {
        return lista.length;
    }

    @Override
    public Object getItem(int i) {
        return lista[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {

        int padding = new BigDecimal("" + mContext.getResources().getDisplayMetrics().density).multiply(new BigDecimal("18.5")).intValue();
        ImageView iv = new ImageView(mContext);
        iv.setPadding(padding, padding, padding, padding);

        Drawable draw = UIUtils.aplicarTema(lista[i], color);


        iv.setImageDrawable(draw);
        iv.setAdjustViewBounds(true); // ajusta o image view de acordo com a coluna do gridview
        // se a coluna for menor ele diminui automaticamente, se for maior, o setAdjustViewBounds corrige o tamanho mantendo o ratio
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callbacks.onCategoriaClique(lista[i]);
            }
        });
        return iv;
    }

    public interface GridViewAdapterCallbacks {
        void onCategoriaClique(int icone);
    }
}
