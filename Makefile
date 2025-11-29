# GNU Makefile
JAR = /usr/bin/jar
JAVA = /usr/bin/java
JAVAC = /usr/bin/javac

JFLAGS = -g 
.SUFFIXES: wsMercado/.java .class
.java.class:
	$(JAVAC) $(JFLAGS) wsMercado\$*.java

default: MercadoServidor MercadoServidorImpl MercadoServidorPublisher wsClientMercado Produto Communication

MercadoServidorImpl: wsMercado/MercadoServidorImpl.java
	$(JAVAC) $(JFLAGS) wsMercado/MercadoServidorImpl.java

MercadoServidor: wsMercado/MercadoServidor.java
	$(JAVAC) $(JFLAGS) wsMercado/MercadoServidor.java

MercadoServidorPublisher: wsMercado/MercadoServidorPublisher.java
	$(JAVAC) $(JFLAGS) wsMercado/MercadoServidorPublisher.java

wsClientMercado: wsMercado/wsClientMercado.java
	$(JAVAC) $(JFLAGS) wsMercado/wsClientMercado.java

Produto: wsMercado/Produto.java
	$(JAVAC) $(JFLAGS) wsMercado/Produto.java


Communication: wsMercado/Communication.java
	$(JAVAC) $(JFLAGS) wsMercado/Communication.java
	
clean:
	rm -f wsMercado/*.class 
