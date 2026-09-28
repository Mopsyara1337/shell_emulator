import java.util.Scanner;

public class Main {
    private static final String VFS_NAME = "my_vfs";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true){
            System.out.print(VFS_NAME + "> ");

            String input = scanner.nextLine().trim();

            if(input.isEmpty()){
                continue;
            }

            String[] parts = input.split("\\s+");

            if(!executeCommand(parts)){
                break;
            }
        }
        scanner.close();
    }

    private static boolean executeCommand(String[] parts){
        String command = parts[0];

        switch (command){
            case "ls":
                printStubCommand("ls", parts);
                return true;

            case "cd":
                printStubCommand("cd", parts);
                return true;

            case "exit":
                System.out.println("Выход из эмулятора.");
                return false;

            default:
                System.out.println("Неизвестная команда: " + command);
                return true;
        }
    }

    private static void printStubCommand(String command, String[] parts){
        System.out.println("Команда: " + command);

        System.out.print("Аргументы:");
        if(parts.length == 1){
            System.out.println(" отсутствуют");
            return;
        }

        for(int i = 1; i < parts.length; i++){
            System.out.print(" " + parts[i]);
        }

        System.out.println();
    }
}
