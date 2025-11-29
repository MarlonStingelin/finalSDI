package wsMercado;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import javax.xml.ws.Endpoint;
import wsMercado.Produto;
import java.time.Instant;
import java.time.Duration;
public class MercadoServidorPublisher {

   private static final int portaBase = 8900;
   private static final String host   = "127.0.0.1";

   private static int myId;
	private static boolean isLeader = false;
   private static int currentLeader;
   private static int numberOfPeers;
	
   private static HashMap<String,Produto> produtos = new HashMap<String,Produto>();

   private static HashMap<Integer,Falha> nosFalhos = new HashMap<Integer,Falha>();
   private static HashMap<Integer,Instant> lastPingStamp = new HashMap<Integer,Instant>();
   private static Instant stampUltimaTentativa = null;


   private static Communication mainChannel = new Communication();
   private static Communication faultDetectChannel = new Communication();
   private static Communication consensusChannel = new Communication();


	public static void bindWSendpouint(){
		if(isLeader){
			System.out.println("Publicando serviço super mercado");
			Endpoint.publish("http://127.0.0.1:8900/wsMercado", new MercadoServidorImpl());
			System.out.println("Servico pronto");
		}
	}

   public static void inicializarFalhas(int numberOfPeers) {
      for (int i = 1; i <= numberOfPeers; i++) {
         nosFalhos.put(i, new Falha(false, null)); 
      }
   }

	public static void leArquivo(String arquivoNome){
	   File myObj = new File("estoques/"+arquivoNome);
      System.out.println(myObj.getAbsolutePath());
      try (Scanner myReader = new Scanner(myObj)) {
         boolean primeiraLinha = true;
         while (myReader.hasNextLine()) {
            String data = myReader.nextLine();

            if (primeiraLinha) { 
                  primeiraLinha = false; 
                  continue; 
            }

            String[] separado = data.split(",");
            Produto novoProduto = new Produto(
                  Integer.parseInt(separado[0]),
                  separado[1],
                  Float.parseFloat(separado[2])
            );
            produtos.put(novoProduto.getNome(), novoProduto);
         }
      }catch(FileNotFoundException e) {
         System.out.println("ERRO: Falha na leitura do arquivo " + arquivoNome);
         e.printStackTrace();
      }
      System.out.println("Arquivo Lido");
	}

   public static void printProdutos() {
      if (produtos.isEmpty()) {
         System.out.println("O mapa de produtos está vazio.");
         return;
      }
      for (Map.Entry<String, Produto> entry : produtos.entrySet()) {
         String codigo = entry.getKey();
         Produto produto = entry.getValue();
         System.out.println("Código: " + codigo + " -> " + produto);
      }
   }

   


	public static void atualizaArquivo(String arquivoNome){

	}

   public static void iniciarCanais(){
      try {
         mainChannel.startServer(portaBase + myId);
      } catch (Exception e) {
         
         System.out.println("ERRO: Falha ao inicializar server 'mainChanel' no método iniciar canais");
         e.printStackTrace(); 
      }
      try {
         faultDetectChannel.startServer(portaBase + myId + numberOfPeers);
      } catch (Exception e) {
         System.out.println("ERRO: Falha ao inicializar server 'faultDetectChannel' no método iniciar canais");
         e.printStackTrace(); 
      }
 
   }


   public static void rotinaInicial(){
      inicializarFalhas(numberOfPeers);
      
      Falha f = null;
      
      for(int i = 1; i <= numberOfPeers; i++){
         //TODO: Adicionar todas as conexoes
         if(i==myId || nosFalhos.get(i).isFalhou()){
            continue;
         }
         try {
            mainChannel.connectToPeer(i, host, portaBase+i);
         } catch (Exception e) {
            f =nosFalhos.get(i);
            if (f != null) {
               f.setFalhou(true);
            }
            System.out.println("ERRO: Falha rotina inical conexao 'mainchannel': peer " + i);
            e.printStackTrace();
            continue;
         }
         try {
            faultDetectChannel.connectToPeer(i, host, portaBase+i+numberOfPeers);
         } catch (Exception e) {
            f =nosFalhos.get(i);
            if (f != null) {
               f.setFalhou(true); 
            }
            System.out.println("ERRO: Falha rotina inical conexao 'faultDetectChannel' peer " + i);
            e.printStackTrace();
            continue;
         }
      }
   }

public static void tentarReconexao(){
      Falha f = null;
      stampUltimaTentativa = Instant.now();
      for(int i = 1; i <= numberOfPeers; i++){
         //TODO: Adicionar todas as conexoes
         
         if(i==myId || !nosFalhos.get(i).isFalhou()){
            continue;
         }

         try {
            mainChannel.connectToPeer(i, host, portaBase+i);
         } catch (Exception e) {
            f =nosFalhos.get(i);
            if (f != null) {
               f.setFalhou(true);
            }
            System.out.println("ERRO: Falha rotina inical conexao 'mainchannel': peer " + i);
            e.printStackTrace();
            continue;
         }
         try {
            faultDetectChannel.connectToPeer(i, host, portaBase+i+numberOfPeers);
         } catch (Exception e) {
            f =nosFalhos.get(i);
            if (f != null) {
               f.setFalhou(true); 
            }
            System.out.println("ERRO: Falha rotina inical conexao 'faultDetectChannel' peer " + i);
            e.printStackTrace();
            continue;
         }
      }
}


public static void verificarFalhas() {
   Instant agora = Instant.now();

   for (int i = 1; i <= numberOfPeers; i++) {
      if (i == myId) {
         continue;
      }

      Falha f = nosFalhos.get(i);
      if (f == null || f.isFalhou()){
         continue; 
      }

      // ignora nós falhos ou não inicializados

      Instant lastPing = lastPingStamp.get(i);

      if (lastPing == null || Duration.between(lastPing, agora).toMillis() >= 1000) {
         try {
               faultDetectChannel.sendMessage(i, "F:"+myId+":PING");
               lastPingStamp.put(i, agora); // atualiza o timestamp
         } catch (Exception e) {
                f =nosFalhos.get(i);
               if (f != null) {
                  f.setFalhou(true); 
               }
               System.err.println("Falha ao enviar ping para nó " + i + ": " + e.getMessage());
         }
      }
      if(stampUltimaTentativa == null || Duration.between(stampUltimaTentativa, agora).toMillis() >= 3000){
            tentarReconexao();
      }
   
   }
}
	public static void main(String[] args) {
      myId = Integer.parseInt(args[0]);
      numberOfPeers = Integer.parseInt(args[1]);


      String arquivoNome = "estoque"+args[0]+".csv";
      leArquivo(arquivoNome);
      iniciarCanais();
      myId = Integer.parseInt(args[0]);
      try {
               
            Thread.sleep(5000); // roda a cada 1 segundo
      } catch (InterruptedException e) {
           
      }
      
      rotinaInicial();
      
      //Thread detector de falhas
      Thread t = new Thread(() -> {
         while (true) {
            try {
                  // TODO: lógica do detector de falhas
                  verificarFalhas();
                  Thread.sleep(1000); // roda a cada 1 segundo
            } catch (InterruptedException e) {
                  break; // encerra a thread
            }
         }
      });


	   t.start();
      //Main loop
      while(true){         
         mainChannel.sendToAllPeers("Olá sou " + myId);
         try {
               
               Thread.sleep(1000); // roda a cada 1 segundo
         } catch (InterruptedException e) {
                  break; // encerra a thread
         }
      }
   }
}


/* 
Args:
[0] -> Numero do processo
[1] -> Número maximo de processos
Métodos:
   printProdutos -> Print todos os produtos no map produtos para stdout
   inicializarFalhas -> Inicializa o map de falhas com valores false e timestamp null

variaveis:
   mainChannel -> Esse é o canal resposavel por trocar as informações com o lider sobre os pedidos do restaurante (é possivel que não precise ser peer to peer lol)

Portas:
A porta do web service entre mercado e restaurante é: 8900:
As portas do main channel para o servior devem ser 8900 + myId


Canais:
ideia até agora
Canal principal               -> Vai ser ultilizado para realizar eleição e tratar as requisições do restaurante
As portas desse canal devem ser calculadas com portaBase + myId
Canal de detecção de falhas   -> Vai ser ultilizado para mandar pings e registrar falhas
as portas desse canal devem ser calculadas com portaBase + numberOfPeers + myId
Canal de consenseso           -> vai ser ultilizado para realizar o conseso quando alguem que era lider volta a ter comunicação e em outro momento que eu esqueci lol
as portas desse canal devem ser calculadas com portaBase + numberOfPeers + numberOfPeers + myId





Mensagem de falha


Protocolos:

protocolo detector de falhas
Mensagens do detector de falhas seguem o seguinte padrão -> F:X:PING out F:X:PONG onde X é o id do destinatario
*/