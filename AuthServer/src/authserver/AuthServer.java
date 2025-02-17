package authserver;

import java.io.*;
import java.net.*;
import java.util.*;

public class AuthServer {
    private static final int PORT = 5000;
    private static final HashMap<String, String> credentials = new HashMap<>();
    private static final Set<String> connectedUsers = Collections.synchronizedSet(new HashSet<>());
    private static final Map<String, Integer> loginAttempts = new HashMap<>();
    private static final Map<String, Long> lockoutTime = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("Servidor iniciado en el puerto " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado desde " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private String username = null;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            boolean authenticated = false;
            
            try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) {

                output.println("Bienvenido. Escriba '1' para crear usuario o '2' para iniciar sesión.");

                while (!authenticated) {
                    String option = input.readLine();

                    if ("1".equals(option)) {
                        output.println("Ingrese nombre de usuario:");
                        String newUser = input.readLine();
                        output.println("Ingrese contraseña:");
                        String newPassword = input.readLine();

                        synchronized (credentials) {
                            if (credentials.containsKey(newUser)) {
                                output.println("Error: el usuario ya existe.");
                            } else {
                                credentials.put(newUser, newPassword);
                                output.println("Usuario creado correctamente.");
                                System.out.println("Nuevo usuario: " + newUser + " Contraseña: " + newPassword);
                            }
                        }
                    } else if ("2".equals(option)) {
                        output.println("Ingrese nombre de usuario:");
                        username = input.readLine();

                        while (true) {
                            if (lockoutTime.containsKey(username)) {
                                long remainingTime = lockoutTime.get(username) - System.currentTimeMillis();
                                if (remainingTime > 0) {
                                    output.println("Acceso denegado. Espere " + (remainingTime / 1000) + " segundos.");
                                    Thread.sleep(remainingTime);
                                    lockoutTime.remove(username);
                                    loginAttempts.remove(username);
                                    output.println("Tiempo de espera finalizado. Intente nuevamente con la contraseña:");
                                } else {
                                    lockoutTime.remove(username);
                                    loginAttempts.remove(username);
                                }
                            }

                            output.println("Ingrese contraseña:");
                            String password = input.readLine();

                            synchronized (credentials) {
                                if (credentials.containsKey(username) && credentials.get(username).equals(password)) {
                                    output.println("Acceso permitido.");
                                    System.out.println("Inicio de sesión exitoso: " + username);
                                    connectedUsers.add(username);
                                    loginAttempts.remove(username);
                                    authenticated = true;
                                    break;
                                } else {
                                    int attempts = loginAttempts.getOrDefault(username, 0) + 1;
                                    loginAttempts.put(username, attempts);

                                    if (attempts >= 3) {
                                        output.println("Acceso denegado. Espere 30 segundos.");
                                        lockoutTime.put(username, System.currentTimeMillis() + 30000);
                                        Thread.sleep(30000);
                                        lockoutTime.remove(username);
                                        loginAttempts.remove(username);
                                        output.println("Tiempo de espera finalizado. Intente nuevamente con la contraseña:");
                                    } else {
                                        output.println("Acceso denegado. Intento " + attempts + " de 3.");
                                    }
                                }
                            }
                        }
                    } else {
                        output.println("Opción no válida. Intente de nuevo.");
                    }
                }

                while (authenticated) {
                    output.println("Opciones: 1. Cambiar contraseña 2. Ver usuarios conectados 3. Cerrar sesión");
                    String choice = input.readLine();

                    if ("1".equals(choice)) {
                        output.println("Ingrese nueva contraseña:");
                        String newPassword = input.readLine();

                        synchronized (credentials) {
                            credentials.put(username, newPassword);
                        }
                        output.println("Contraseña cambiada correctamente.");
                        System.out.println("Usuario " + username + " cambió su contraseña.");
                    } else if ("2".equals(choice)) {
                        synchronized (connectedUsers) {
                            output.println("Usuarios conectados: " + connectedUsers);
                        }
                    } else if ("3".equals(choice)) {
                        output.println("Sesión cerrada.");
                        synchronized (connectedUsers) {
                            connectedUsers.remove(username);
                        }
                        System.out.println("Usuario " + username + " cerró sesión.");
                        break;
                    } else {
                        output.println("Opción no válida.");
                    }
                }
            } catch (IOException | InterruptedException ex) {
            } finally {
                try {
                    socket.close();
                    System.out.println("Cliente desconectado.");
                } catch (IOException e) {
                }
            }
        }
    }
}
