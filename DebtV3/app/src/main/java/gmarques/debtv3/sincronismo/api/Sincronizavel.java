package gmarques.debtv3.sincronismo.api;

public interface Sincronizavel {

    long getUltimaAtt();

    /**
     * Use um valor constante independente de fuso-horario para atualizar
     * a variavel atravez deste metodo, ou seja, salve timestamps em UTC.
     */
    void setUltimaAtt();

    boolean estaRemovido();

    void setRemovido(boolean removido);

    long getOrigem();

    /*Mude a id origem do objeto para indicar que foi criadao no aparelho atual*/
    void resetarOrigem();

    long getId();

    /*serve para apenas para mostrar o nome dos itens que estao sincronizando no momento...
     * nao vi problema por esse metodo na interface pq todos os objetos sincronizaves
     * (que hj 02/05/20 sao apenas Item e ShopList) tem um campo nome*/
    String getNome();


}
