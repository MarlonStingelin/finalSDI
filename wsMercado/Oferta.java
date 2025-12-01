package wsMercado;

public class Oferta implements Comparable<Oferta> {
    int idFilial;
    float preco;
    int quantidade;

    public Oferta(int idFilial, float preco, int quantidade) {
        this.idFilial = idFilial;
        this.preco = preco;
        this.quantidade = quantidade;
    }

    @Override
    public int compareTo(Oferta o) {
        // ordena pelo menor preço
        return Float.compare(this.preco, o.preco);
    }
}
