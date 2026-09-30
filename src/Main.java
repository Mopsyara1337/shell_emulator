import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
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
    private static final String VFS_NAME = "my_vfs";
    private static Path vfsPath;
    private static Path startupPath;
    private static VfsNode vfsRoot;
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

            System.out.println(VFS_NAME + "> " + input);

            String[] parts = input.split("\\s+");

            if(!executeCommand(parts)){
                return;
            }
            System.out.println();
        }

        //Чтение команд из консоли
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

                //СПРОСИТЬ ЧТО ЭТО ДЕЛАЕТ
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
}