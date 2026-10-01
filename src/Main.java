import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public class Main {
    private static Path vfsPath;
    private static Path startupPath;
    private static VfsNode vfsRoot;
    private static VfsNode currentDirectory;
    private static String rootName;
    private static String vfsHash;

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

        if(!Files.exists(vfsPath)){
            System.out.println("VFS не найдена");
            return;
        }

        vfsRoot = loadVfs();
        if (vfsRoot == null) {
            return;
        }
        currentDirectory = vfsRoot;

        String vfsData;
        try {
            vfsData = Files.readString(vfsPath);
            vfsHash = calculateSha256(vfsData);
        }catch(IOException e){
            System.out.println("Ошибка чтения VFS");
            return;
        }

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

        //Чтение команд из стартового скрипта
        for(String input : commands){
            if(input.isEmpty()){
                continue;
            }

            System.out.println(getCurrentPath() + "> " + input);

            String[] parts = input.split("\\s+");

            if(!executeCommand(parts)){
                return;
            }
            System.out.println();
        }

        //Чтение команд из консоли
        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.print(getCurrentPath() + "> ");

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
                ls(parts);
                return true;

            case "cd":
                cd(parts);
                return true;

            case "head":
                head(parts);
                return true;

            case "tac":
                tac(parts);
                return true;

            case "exit":
                System.out.println("Выход из эмулятора.");
                return false;

            case "conf-dump":
                System.out.println("vfs-path: " + vfsPath);
                System.out.println("startup-script: " + startupPath);
                return true;

            case "vfs-info":
                System.out.println("name: " + rootName);
                System.out.println("sha256: " + vfsHash);
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

    private static VfsNode loadVfs(){
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(vfsPath.toFile());

            Element rootElement = document.getDocumentElement();

            rootName = rootElement.getAttribute("name");
            return parseElement(rootElement);
        }catch (ParserConfigurationException e){
            System.out.println("Ошибка создания XML-парсера");
        }catch (IOException e){
            System.out.println("Ошибка чтения VFS");
        }catch (SAXException e){
            System.out.println("Некорректный формат XML");
        }
        return null;
    }

    private static VfsNode parseElement(Element element){
        VfsNode node;
        if(element.getTagName().equals("file")){
            String name = element.getAttribute("name");
            String content = element.getTextContent();
            node = new VfsNode(name, content);
        }
        else if(element.getTagName().equals("directory") ||
        element.getTagName().equals("vfs")){
            String name = element.getAttribute("name");
            node = new VfsNode(name);

            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++){
                Node child = children.item(i);

                if(child.getNodeType() == Node.ELEMENT_NODE){
                    Element childElement = (Element) child;

                    VfsNode childNode = parseElement(childElement);
                    if(childNode != null) {
                        node.addChild(childNode);
                    }
                }
            }
        }
        else {
            System.out.println("Некорректный тег: " + element.getTagName());
            return null;
        }
        return node;
    }

    private static void printTree(VfsNode node, int level){
        String indent = "    ".repeat(level);

        if(node.isDirectory()){
            System.out.println(indent + node.getName() + '/');
            for(VfsNode child : node.getChildren()){
                printTree(child, level + 1);
            }
        }
        else{
            System.out.println(indent + node.getName());
        }
    }

    private static String calculateSha256(String data){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for(byte b : hash){
                result.append(String.format("%02x", b));
            }
            return result.toString();
        }catch (NoSuchAlgorithmException e){
            System.out.println("Алгоритм SHA-256 недоступен");
        }
        return null;
    }

    private static void ls(String[] parts){
        VfsNode current;
        if(parts.length == 1){
            current = currentDirectory;
        }
        else if(parts.length == 2){
            current = resolvePath(parts[1]);
            if (current == null) {
                System.out.println("Ошибка: директория " + parts[1] + " не найдена");
                return;
            }

            if (!current.isDirectory()) {
                System.out.println("Ошибка: " + parts[1] + " не является директорией");
                return;
            }
        }
        else{
            System.out.println("Ошибка: неверное число параметров");
            return;
        }

        for (VfsNode node : current.getChildren()) {
            if(node.isDirectory()){
                System.out.println(node.getName() + '/');
            }
            else{
                System.out.println(node.getName());
            }
        }
    }

    private static void cd(String[] parts){
        if(parts.length != 2){
            System.out.println("Ошибка: cd принимает 1 аргумент");
            return;
        }

        VfsNode target = resolvePath(parts[1]);

        if(target == null){
            System.out.println("Ошибка: директория " + parts[1] + " не найдена");
            return;
        }

        if(!target.isDirectory()){
            System.out.println("Ошибка: " + parts[1] + " не является директорией");
            return;
        }

        currentDirectory = target;
    }

    private static void head(String[] parts) {
        String target;
        int linesCount = 10;
        if(parts.length == 2){
            target = parts[1];
        }
        else if(parts.length == 3){
            try{
                linesCount = Integer.parseInt(parts[1]);
            }catch (NumberFormatException e){
                System.out.println("Ошибка: количество строк должно быть целым числом");
                return;
            }

            if (linesCount < 0) {
                System.out.println("Ошибка: количество строк не может быть отрицательным");
                return;
            }

            target = parts[2];
        }
        else {
            System.out.println("Ошибка: неверное число параметров");
            return;
        }

        VfsNode file = resolvePath(target);

        if (file == null) {
            System.out.println("Ошибка файл " + target + " не найден");
            return;
        }

        if(file.isDirectory()){
            System.out.println("Ошибка: " + target + " не является файлом");
            return;
        }

        if(file.getContent().isEmpty()){
            return;
        }

        String[] lines = file.getContent().split("\\R");
        int n = Math.min(linesCount, lines.length);

        for (int i = 0; i < n; i++) {
            System.out.println(lines[i]);
        }
    }

    private static void tac(String[] parts){
        if(parts.length != 2){
            System.out.println("Ошибка: tac принимает один параметр");
            return;
        }

        String target = parts[1];
        VfsNode file = resolvePath(target);

        if (file == null) {
            System.out.println("Ошибка файл " + target + " не найден");
            return;
        }

        if(file.isDirectory()){
            System.out.println("Ошибка: " + target + " не является файлом");
            return;
        }

        if (file.getContent().isEmpty()){
            return;
        }

        String[] lines = file.getContent().split("\\R");

        for(int i = lines.length - 1; i >= 0; i--){
            System.out.println(lines[i]);
        }
    }

    private static VfsNode findChild(String target, VfsNode current){
        for(VfsNode node : current.getChildren()){
            if(node.getName().equals(target)){
                return node;
            }
        }
        return null;
    }

    private static String getCurrentPath(){
        Deque<String> stack = new ArrayDeque<>();
        VfsNode currentNode = currentDirectory;

        while(currentNode != null){
            stack.push(currentNode.getName());
            currentNode = currentNode.getParent();
        }

        StringBuilder path = new StringBuilder();
        while(!stack.isEmpty()){
            path.append(stack.pop());

            if(!stack.isEmpty()){
                path.append("/");
            }
        }

        return path.toString();
    }

    private static VfsNode resolvePath(String path) {
        if (path.isEmpty()) {
            return currentDirectory;
        }

        VfsNode current;

        if (path.startsWith("/")) {
            current = vfsRoot;
        } else {
            current = currentDirectory;
        }

        String[] pathParts = path.split("/");

        for (String part : pathParts) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }

            if (part.equals("..")) {
                if (current.getParent() != null) {
                    current = current.getParent();
                }
                continue;
            }

            if (!current.isDirectory()) {
                return null;
            }

            current = findChild(part, current);

            if (current == null) {
                return null;
            }
        }

        return current;
    }
}