package wsMercado;

import javax.jws.WebService;
import java.util.Vector;
import java.util.Collections;
import java.util.HashMap;
@WebService(endpointInterface = "wsMercado.MercadoServidor")
public class MercadoServidorImpl implements MercadoServidor {
    
    Vector<String> mercadosCadastrados = new Vector<>();
    HashMap<Integer,Vector<String>> pedidos = new HashMap<>();
    public int cadastrarPedido(String restaurante){
            System.out.println("Chamado cadastrar pedido");
            mercadosCadastrados.add(restaurante);
            int maxKey = 0;
            if(pedidos.keySet().isEmpty()){
                maxKey = 1;
            }else{
                maxKey = Collections.max(pedidos.keySet()) + 1;
            }
            pedidos.put(maxKey, new Vector<String>());
            return  maxKey;
    }

    
    public boolean comprarProdutos(int restaurante, String[] produtos){
        System.out.println("Chamado Comprar produtos");
        Vector<String> produtosComprar = new Vector<String>();
        for(String s : produtos){
            produtosComprar.add(s);
        }
        if(!pedidos.containsKey(restaurante)){
            return false;
        }
        pedidos.put(restaurante,produtosComprar);
        return true;
    }

    
    public int tempoEntrega(int restaurante){
        System.out.println("Chamado tempo entrega");
        Vector<String> produtosEntrega = pedidos.get(restaurante);
        if (produtosEntrega == null) return -1; 
        int tempoEntrega = produtosEntrega.size() / 2 + 3;
        return tempoEntrega;
    }
}
