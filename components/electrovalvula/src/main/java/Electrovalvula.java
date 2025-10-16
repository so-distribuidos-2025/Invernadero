public class Electrovalvula{

    private boolean estaAbierta;

    /**
     * Las valvulas en principio se encuentran cerradas
     */
    public Electrovalvula() {
        estaAbierta = false;
    }

    /**
     * Abre la valvula
     */
    public void abrir() {
        estaAbierta = true;
    }

    /**
     * Cierra la valvula
     */
    public void cerrar() {
        estaAbierta = false;
    }


    @Override
    public String toString() {
        return "Electrovalvula{" + "estaAbierta=" + estaAbierta + '}';
    }

}