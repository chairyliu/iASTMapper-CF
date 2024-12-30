package cs.model.algorithm.utils;

import com.github.gumtreediff.tree.ITree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeclarationUtil {

    public static List<String> getMethodDeclarations(ITree root) {
        List<ITree> children = root.getChildren();
        Set<ITree> typeDeclarationSet = new HashSet<>();
        for (ITree child : children) {
            if ((child.getType().name).equals("TypeDeclaration")) {
                typeDeclarationSet.add(child);
            }
        }
        Set<ITree> methodDeclarationSet = new HashSet<>();
        for (ITree typeDeclaration : typeDeclarationSet) {
            List<ITree> typeDeclarationChildren = typeDeclaration.getChildren();
            for (ITree typeDeclarationChild : typeDeclarationChildren) {
                if (typeDeclarationChild.getType().name.equals("MethodDeclaration")) {
                    methodDeclarationSet.add(typeDeclarationChild);
                }
            }
        }

        List<String> methods = new ArrayList<>();
        for (ITree methodDeclaration : methodDeclarationSet){
            String method = TreePrinter(methodDeclaration);
            methods.add(method);
        }
        return methods;
    }

    public static String TreePrinter(ITree node){
        StringBuilder builder = tree2String(node);
        String res = builder.toString();
        return res;
    }

    public static StringBuilder tree2String(ITree tree){
        StringBuilder content = new StringBuilder();

        for (ITree child : tree.getChildren()) {
            if (!((child.getType().name).equals("Block") ||
                    (child.getType().name).equals("MarkerAnnotation") ||
                    (child.getType().name).equals("SingleMemberAnnotation") ||
                    (child.getType().name).equals("NormalAnnotation")||
                    (child.getType().name).equals("Javadoc")
            )
            ){
                if ((child.getType().name).equals("SingleVariableDeclaration")) {
                    content.append(extractDeclaration(child));
                }
                else{
                    content.append(extractDeclaration(child));
                }
            }

        }
        return content;
    }

    public static String extractDeclaration(ITree tree){
        StringBuilder content = new StringBuilder();
        if (tree.getChildren().size() == 0){
            content.append(tree.getLabel());
        }
        else{
            List<ITree> children = tree.getChildren();
            for (int i = 0; i < children.size(); i++) {
                content.append(extractDeclaration(children.get(i)));
            }
        }
        content.append(" ");
        return content.toString();
    }
}


