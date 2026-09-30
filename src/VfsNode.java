import java.util.ArrayList;
import java.util.List;

public class VfsNode {
    private String name;
    private boolean isDirectory;
    private String content;
    private List<VfsNode> children;

    public VfsNode(String name, String content){
        this.name = name;
        this.isDirectory = false;
        this.content = content;
    }

    public VfsNode(String name){
        this.name = name;
        this.isDirectory = true;
        this.children = new ArrayList<>();
    }

    public void addChild(VfsNode child){
        children.add(child);
    }

    public String getName() {
        return name;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public List<VfsNode> getChildren() {
        return children;
    }
}
