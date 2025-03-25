package gmarques.debtv3.activities.add_edit_categorias.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.math.BigDecimal;
import java.util.ArrayList;




/**
 * Criado por Gilian Marques em 16/01/2017.
 */

public class GridViewColorsAdapter extends BaseAdapter {

    private final Context mContext;
    private final ArrayList<Integer> lista;
    private final GridViewAdapterCallbacks callbacks;

    public GridViewColorsAdapter(Context mContext, ArrayList<Integer> lista, GridViewAdapterCallbacks callbacks) {
        this.mContext = mContext;
        this.lista = lista;
        this.callbacks = callbacks;
    }

    @Override
    public int getCount() {
        return lista.size();
    }

    @Override
    public Object getItem(int i) {
        return lista.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {

        int padding = new BigDecimal("" + mContext.getResources().getDisplayMetrics().density).multiply(new BigDecimal("28.5")).intValue();
        ImageView iv = new ImageView(mContext);
        iv.setPadding(padding, padding, padding, padding);


        iv.setBackgroundColor(lista.get(i));
        iv.setAdjustViewBounds(true); // ajusta o image view de acordo com a coluna do gridview
        // se a coluna for menor ele diminui automaticamente, se for maior, o setAdjustViewBounds corrige o tamanho mantendo o ratio
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callbacks.onCategoriaClique(lista.get(i));
            }
        });
        return iv;
    }

    public interface GridViewAdapterCallbacks {
        void onCategoriaClique(int color);
    }
}
