package util;

/**
 * Clase estatica para el calculo del INR
 */
public class INR {
    private static final double W1 = 0.5;
    private static final double W2 = 0.3;
    private static final double W3 = 0.2;
    private static final double T_MAX = 40.0;
    private static final double R_MAX = 1000.0;

    public static double calcularInr(double humedad, double radiacion, double temperatura){
        return (W1 * (1 - humedad / 100.0))
                + (W2 * (temperatura / T_MAX))
                + (W3 * (radiacion / R_MAX));
    }
}
