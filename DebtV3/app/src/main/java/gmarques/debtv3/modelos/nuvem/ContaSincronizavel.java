package gmarques.debtv3.modelos.nuvem;

public  class ContaSincronizavel {


    public ContaSincronizavel(String nome, String email, String foto) {
        this.nome = nome;
        this.email = email;
        this.foto = foto;
    }

    public String nome;
    public String email;
    public String foto;

    public ContaSincronizavel setAnfitriao() {
        this.anfitriao = true;
        return this;
    }

    /*se true, significa que o usuario local sincroniza dados no com o usuario desta classe*/
    public boolean anfitriao;

}
