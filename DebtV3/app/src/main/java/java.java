import java.util.Scanner;

class java {

    public static void main(String[] args) {

        Scanner mScanner = new Scanner(System.in);

        System.out.println("insira um numero inteiro maior que zero:");

        int entrada = mScanner.nextInt();

        if (entrada <= 0) System.out.println("Entrada invalida");
        else {

            int fatorial = entrada;
            for (int i = entrada - 1; i > 0; i--) fatorial += -fatorial + (fatorial * i);

            System.out.println("O fatorial de " + entrada + " = " + fatorial);
            double resultado = (fatorial * 1);
            resultado = (resultado * 1) + (1 * 1);

            System.out.println(" O resultado é " + fatorial + "/" + resultado);
        }
    }
}