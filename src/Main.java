import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final String VFS_NAME = "my_vfs";
    private static Path vfsPath;
    private static Path startupPath;

    public static void main(String[] args) {
        if(args.length != 2){
            System.out.println("Неверное число аргументов");
            return;
        }

        vfsPath = Path.of(args[0]);
        startupPath = Path.of(args[1]);

        System.out.println("VFS path: " + vfsPath);
        System.out.println("Startup path: " + startupPath);
        System.out.println();

        if(!Files.exists(startupPath)){
            System.out.println("Стартовый скрипт не найден");
            return;
        }

        List<String> commands;
        try {
            commands = Files.readAllLines(startupPath);
        }catch (IOException e){
            System.out.println("Ошибка чтения стартового скрипта");
            return;
        }

        for(String input : commands){
            if(input.isEmpty()){
                continue;
            }

            System.out.println(VFS_NAME + "> " + input);

            String[] parts = input.split("\\s+");

            if(!executeCommand(parts)){
                return;
            }
            System.out.println();
        }

        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.print(VFS_NAME + "> ");

            String input = scanner.nextLine().trim();
            if(input.isEmpty()){
                continue;
            }

            String[] parts = input.split("\\s+");

            if(!executeCommand(parts)){
                break;
            }
            System.out.println();
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

            case "conf-dump":
                System.out.println("vfs-path: " + vfsPath);
                System.out.println("startup-script: " + startupPath);
                return true;

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