package wsMercado;

import java.time.Instant;

public class Falha {
    private boolean falhou;
    private Instant timeStampFalha;

    public Falha(boolean falhou, Instant ts) {
        this.falhou = falhou;
        this.timeStampFalha = ts;
    }

    public boolean isFalhou() {
        return falhou;
    }

    public void setFalhou(boolean falhou) {
        this.falhou = falhou;
        if (falhou) {
            this.timeStampFalha = Instant.now();
        }
    }

    public Instant getTimeStampFalha() {
        return timeStampFalha;
    }

    @Override
    public String toString() {
        return "Falha[falhou=" + falhou + ", ts=" + timeStampFalha + "]";
    }
}
