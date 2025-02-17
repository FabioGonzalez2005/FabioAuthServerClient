package authclient;

import java.io.*;
import java.net.*;

public class AuthClient {
    public static void main(String[] args) {
        final String SERVER_ADDRESS = "127.0.0.1";
        final int SERVER_PORT = 5000;

        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Conectado al servidor de autenticación.");

            String serverResponse;
            while ((serverResponse = input.readLine()) != null) {
                System.out.println(serverResponse);
                
                if (serverResponse.contains("Espere")) {
                    int waitTime = 30;
                    String[] parts = serverResponse.split(" ");
                    
                    for (String part : parts) {
                        try {
                            waitTime = Integer.parseInt(part);
                            break;
                        } catch (NumberFormatException ignored) {
                        }
                    }

                    try {
                        Thread.sleep(waitTime * 1000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                if (serverResponse.contains("Bienvenido") || serverResponse.contains("Opciones")) {
                    String userInput = console.readLine();
                    output.println(userInput);
                } else if (serverResponse.contains("Ingrese") || serverResponse.contains("Opción no válida")) {
                    String userInput = console.readLine();
                    output.println(userInput);
                } else if (serverResponse.contains("cerrada")) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
