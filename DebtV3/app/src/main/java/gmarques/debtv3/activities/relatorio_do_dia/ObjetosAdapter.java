package gmarques.debtv3.activities.relatorio_do_dia;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.AlignSelf;
import com.google.android.flexbox.FlexboxLayoutManager;

import java.util.ArrayList;

import gmarques.debtv3.Debt;
import gmarques.debtv3.R;
import gmarques.debtv3.sincronismo.api.Sincronizavel;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.outros.Tag;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.FormatUtils;

/**
 * Criado por Gilian Marques
 * Quinta-feira, 25 de Julho de 2019  as 19:45:06.
 */
class ObjetosAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Callback callback;
    private ArrayList<Sincronizavel> objetos = new ArrayList<>();

    ObjetosAdapter(ArrayList<Sincronizavel> pendencias, final Callback callback) {
        this.objetos = pendencias;
        this.callback = callback;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(Debt.binder.get().activity().getLayoutInflater().inflate(R.layout.layout_relatorio_do_dia, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder hdr, int position) {
        MyViewHolder viewHolder = (MyViewHolder) hdr;
        Sincronizavel sObj = objetos.get(position);

        String nome = sObj.getNome();
        String valor = "?";

        if (sObj instanceof Despesa) {
            viewHolder.ivIcon.setImageResource(R.drawable.vec_despesa);

            valor = FormatUtils.emReal(((Despesa) sObj).getValor());

            if (((Despesa) sObj).estaPaga()) viewHolder.itemView.setAlpha(0.8f);
            else viewHolder.itemView.setAlpha(1f);


        } else if (sObj instanceof Receita) {
            viewHolder.ivIcon.setImageResource(R.drawable.vec_receita);

            valor = FormatUtils.emReal(((Receita) sObj).getValor());

            if (((Receita) sObj).estaRecebido())
                viewHolder.itemView.setAlpha(0.8f);
            else viewHolder.itemView.setAlpha(1f);

        }


        viewHolder.tvName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        Spannable span = new SpannableString(valor + "\n" + nome);
        span.setSpan(new RelativeSizeSpan(0.80f), valor.length(), valor.length() + nome.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        viewHolder.tvName.setText(span);


        ViewGroup.LayoutParams lp = viewHolder.parent.getLayoutParams();
        if (lp instanceof FlexboxLayoutManager.LayoutParams) {
            FlexboxLayoutManager.LayoutParams flexboxLp = (FlexboxLayoutManager.LayoutParams) lp;
            flexboxLp.setFlexGrow(1.0f);
            flexboxLp.setAlignSelf(AlignSelf.AUTO);
        }

    }

    @Override
    public int getItemCount() {
        return objetos.size();
    }

    public void remove(int adapterPosition) {
        objetos.remove(adapterPosition);
        notifyItemRemoved(adapterPosition);
    }

    public void atualizarItem(int adapterPosition) {
        notifyItemChanged(adapterPosition);
        Log.d(Tag.AppTag, "ObjetosAdapter.atualizarItem: aaaaa " + adapterPosition + "    " + objetos.get(adapterPosition).toString());
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        final ImageView ivIcon;
        final TextView tvName;
        public View parent;
        public View parent2;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            parent = itemView.findViewById(R.id.parent);
            parent2 = itemView.findViewById(R.id.parent2);
            ivIcon = itemView.findViewById(R.id.ivIcone);
            tvName = itemView.findViewById(R.id.tvNome);
            itemView.setOnClickListener(new AnimatedClickListener() {
                @Override
                public void onClick(View view) {
                    super.onClick(view);
                    callback.onClick(objetos.get(getAdapterPosition()), getAdapterPosition());
                }
            });

        }

    }

    public interface Callback {
        void onClick(Sincronizavel sObj, int adapterPosition);
    }
}
