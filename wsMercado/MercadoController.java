package wsMercado;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MercadoController {

    private static MercadoController instance;
    private Communication communication;
    private int myId;

    private boolean emEleicao = false;
    private int currentLeaderId = -1;
    
    // armazena o estoque local (ID_PRODUTO -> Produto)
    private Map<String, Produto> estoqueLocal;

    // mapa de espera 
    private ConcurrentHashMap<String, List<Oferta>> bufferOfertas = new ConcurrentHashMap<>();

    private MercadoController() {
        estoqueLocal = new HashMap<>(); 
    }

    public static synchronized MercadoController getInstance() {
        if (instance == null) {
            instance = new MercadoController();
        }
        return instance;
    }

    public void init(Communication comm, int id, Map<String, Produto> produtosIniciais) {
        this.communication = comm;
        this.myId = id;
        this.estoqueLocal = produtosIniciais;
    }

    // metodos chamados pelo soap

    public boolean processarCompraCliente(String nomeProduto) {
        System.out.println("[CONTROLLER] Iniciando compra distribuída para: " + nomeProduto);
        
        // gera um ID único para essa transação
        String reqId = UUID.randomUUID().toString();
        
        // prepara o buffer para receber respostas
        bufferOfertas.put(reqId, Collections.synchronizedList(new ArrayList<>()));

        // protocolo: QUERY:REQ_ID:NOME_PRODUTO
        if(communication != null) {
            communication.sendToAllPeers("QUERY:" + reqId + ":" + nomeProduto);
        }

        // verifica o estoque
        verificarEstoqueLocalEAdicionar(reqId, nomeProduto);

        // coleta
        try {
            Thread.sleep(2000); // Espera 2 segundos
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<Oferta> ofertas = bufferOfertas.get(reqId);
        
        if (ofertas == null || ofertas.isEmpty()) {
            System.out.println("[CONTROLLER] Nenhuma oferta encontrada para " + nomeProduto);
            bufferOfertas.remove(reqId);
            return false;
        }

        // Ordem de menor preço
        Oferta melhorOferta = null;
        synchronized(ofertas) {
            if(!ofertas.isEmpty()){
                Collections.sort(ofertas);
                melhorOferta = ofertas.get(0);
            }
        }
        
        if (melhorOferta == null) return false;

        System.out.println("[CONTROLLER] Vencedor: Filial " + melhorOferta.getIdFilial() + " Valor: " + melhorOferta.getPreco());

        boolean sucesso = false;
        
        if (melhorOferta.getIdFilial() == this.myId) {
            sucesso = efetivarVendaLocal(nomeProduto);
        } else {
            try {
                // Protocolo: BUY:REQ_ID:NOME_PRODUTO
                communication.sendMessage(melhorOferta.getIdFilial(), "BUY:" + reqId + ":" + nomeProduto);
                sucesso = true; 
            } catch (Exception e) {
                e.printStackTrace();
                sucesso = false;
            }
        }

        bufferOfertas.remove(reqId); 
        return sucesso;
    }

    // metodo chamado pela rede 

    public void processarMensagemP2P(String msg) {
        // Formato esperado: TIPO:DADO1:DADO2...
        try {
            String[] parts = msg.trim().split(":");
            if (parts.length < 2) return;

            String tipo = parts[0];

            switch (tipo) {
                case "QUERY":
                    // Alguém quer saber se tenho produto. QUERY:REQ_ID:PRODUTO
                    if(parts.length >= 3) handleQuery(parts[1], parts[2]);
                    break;

                case "RESP":
                    // Alguém respondeu minha pergunta. RESP:REQ_ID:ID_FILIAL:PRECO:QTD
                    if(parts.length >= 4) handleResponse(parts[1], Integer.parseInt(parts[2]), Float.parseFloat(parts[3]), 1);
                    break;

                case "BUY":
                    // O Líder mandou eu vender. BUY:REQ_ID:PRODUTO
                    if(parts.length >= 3) handleBuy(parts[2]);
                    break;

                case "ELECTION":
                    // Recebi ELECTION de alguém. ELECTION:ID_REMETENTE
                    int senderId = Integer.parseInt(parts[1]);
                    
                    if (senderId < myId) {
                        // Sou maior que ele. Mando ele ficar quieto (OK) e começo minha eleição
                        communication.sendMessage(senderId, "OK:" + myId);
                        iniciarEleicao(); 
                    }
                    break;

                case "OK":
                    // Recebi um OK de alguém maior. Desisto de ser líder. OK:ID_MAIOR
                    System.out.println("[ELEIÇÃO] Recebi OK de " + parts[1] + ". Aguardando novo líder...");
                    emEleicao = false; 
                    // Fico quieto esperando a msg COORDINATOR
                    break;

                case "COORDINATOR":
                    // Novo líder definido. COORDINATOR:ID_NOVO_LIDER
                    int newLeader = Integer.parseInt(parts[1]);
                    System.out.println("!!! [NOVO LIDER] O nó " + newLeader + " é o novo coordenador !!!");
                    currentLeaderId = newLeader;
                    emEleicao = false;
                    
                    // Se eu era líder antes e recebi isso de outro, devo renunciar (desligar WS)
                    if (myId != newLeader) {
                        MercadoServidorPublisher.renunciarLideranca();
                    }
                    break;  

                default:
                    // Ignora mensagens de PING/ELEIÇÃO aqui
                    break;
            }
        } catch (Exception e) {
            System.err.println("Erro processar msg: " + msg);
        }
    }

    // LÓGICA INTERNA 

    private void handleQuery(String reqId, String produto) {
        Produto p = estoqueLocal.get(produto);
        // Se eu tenho o produto e preço > 0
        if (p != null && p.getValor() > 0) { // Assumir que ter o produto é ter estoque
             // Respondo ao líder
             // Protocolo: RESP:REQ_ID:MEU_ID:PRECO:QTD
             communication.sendToAllPeers("RESP:" + reqId + ":" + myId + ":" + p.getValor() + ":1");
        }
    }

    private void handleResponse(String reqId, int idFilial, float preco, int qtd) {
        // Só me importo se eu for o Líder que iniciou esse reqId
        // Se chegou resposta atrasada de um pedido que já expirou, ignora.
        List<Oferta> lista = bufferOfertas.get(reqId);
        if (lista != null) {
            lista.add(new Oferta(idFilial, preco, qtd));
        }
    }

    private void handleBuy(String nomeProduto) {
        System.out.println("[ESTOQUE] Efetuando baixa no produto: " + nomeProduto);
        // Ainda é preciso implementa a lógica de remover do CSV ou atualizar a memória
        // remover do mapa ou mudar quantidade
        Produto p = estoqueLocal.get(nomeProduto);
        if(p != null) {
            // p.setQuantidade(p.getQuantidade() - 1);
            System.out.println("[ESTOQUE] Venda realizada na filial local!");
        }
    }

    private void verificarEstoqueLocalEAdicionar(String reqId, String produto) {
        Produto p = estoqueLocal.get(produto);
        if (p != null) {
             List<Oferta> lista = bufferOfertas.get(reqId);
             if(lista != null) {
                 lista.add(new Oferta(myId, p.getValor(), 1));
             }
        }
    }
    
    private boolean efetivarVendaLocal(String produto) {
        handleBuy(produto);
        return true;
    }

    public void iniciarEleicao() {
        if (emEleicao) return;
        
        System.out.println("!!! [ELEIÇÃO] Iniciando processo de eleição... !!!");
        emEleicao = true;

        // Envia para todos (idealmente seria só para os maiores, mas broadcast funciona)
        if(communication != null) communication.sendToAllPeers("ELECTION:" + myId);

        // espera um tempo por respostas "OK" de IDs maiores
        final long TIMEOUT = 2000; // 2 segundos
        
        new Thread(() -> {
            try {
                Thread.sleep(TIMEOUT);
                // Se ninguém maior mandou OK, eu ganhei
                if (emEleicao) {
                    tornarSeLider();
                }
            } catch (InterruptedException e) { e.printStackTrace(); }
        }).start();
    }

    private void tornarSeLider() {
        System.out.println("[LÍDER] Eu (" + myId + ") sou o novo Líder!");
        currentLeaderId = myId;
        emEleicao = false;
        
        // avisa todo mundo
        communication.sendToAllPeers("COORDINATOR:" + myId);
        
        // publica o WebService (Chama método estático no Main)
        MercadoServidorPublisher.assumirLideranca();
    }

}
