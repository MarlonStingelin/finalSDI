package wsMercado;


import java.net.URL;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;

import javax.print.DocFlavor.STRING;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
 

public class wsClientMercado {

    static HashMap<Integer,Vector<String>> pedidosMercado = new HashMap<Integer,Vector<String>>();
    
    public static MercadoServidor connect(){
		try {
 
			URL url = new URL("http://localhost:8900/wsMercado?wsdl");
			QName qname = new QName("http://wsMercado/",
					"MercadoServidorImplService");
 
			Service service = Service.create(url, qname);
 
			MercadoServidor server = service.getPort(MercadoServidor.class);
            return server;
		} catch (Exception e) {
            System.out.println("ERRO: Falha no connect");
			e.printStackTrace();
		}
        return null;
    }

    public static void menu(){
        MercadoServidor server = connect();
        int opcao = 0;
        Scanner sc = new Scanner(System.in);

        while(opcao != 4){
            System.out.println("Escolha uma opção:");
            System.out.println("1 - Cadastrar pedido");
            System.out.println("2 - Comprar produtos");
            System.out.println("3 - Tempo entrega");
            System.out.println("4 - Sair");
            opcao = sc.nextInt();
            sc.nextLine();
            switch (opcao) {
                case 1:
                    System.out.println("Cadastrando novo pedido");
                    int codigoNovoPedido = server.cadastrarPedido("meuRestaurante");
                    pedidosMercado.put(codigoNovoPedido, new Vector<String>());
                    System.out.println("Novo pedido criado com codigo " + codigoNovoPedido);
                break;
                case 2:
                    Vector<String> produtosComprar = new Vector<String>();
                    String novoProduto = "";
                    while(!novoProduto.equals("0")){
                        System.out.println("Insira o nome de um produto para comprar (0 para finalizar):");
                        novoProduto = sc.nextLine();
                        if(!novoProduto.equals("0")){
                            produtosComprar.add(novoProduto);
                        }
                    }
                    System.out.println("Escolha um dos pedidos:\n[");
                    for(Integer i : pedidosMercado.keySet()){
                        if(pedidosMercado.get(i).isEmpty()){
                            System.out.print(i);
                        }
                    }
                    System.out.println("\n]");
                    int nPedido = sc.nextInt();
                    sc.nextLine();
                    if(server.comprarProdutos(nPedido, produtosComprar.toArray(new String[produtosComprar.size()]))){
                        pedidosMercado.get(nPedido).addAll(produtosComprar);
                        System.out.println("Pedido de compra cadastrado com sucesso");
                    }else{
                        System.out.println("ERRO: Falha ao cadastrar pedido de compra");
                    }
                break;
                case 3:
                    System.out.println("Escolha um dos pedidos:\n[");
                    for(Integer i : pedidosMercado.keySet()){
                            System.out.print(i);             
                    }
                    System.out.println("\n]");
                    int mPedido = sc.nextInt();
                    sc.nextLine();
                    int tempo = server.tempoEntrega(mPedido);
                    System.out.println("O pedido de:");
                    for(String s : pedidosMercado.get(mPedido)){
                        System.out.print(s + " ");
                    }
                    System.out.println("\nVai demorar " + tempo + " dias para ser entregue");
                break;
                case 4:
                    System.out.println("Saindo....");
                break;
                default:
                System.out.println("Opção inválida");
                    break;
            }
        }

        sc.close();

    }

    public static void main(String[] args){
        menu();
    }

}
