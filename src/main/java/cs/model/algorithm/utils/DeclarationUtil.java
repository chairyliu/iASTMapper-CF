package cs.model.algorithm.utils;

import com.github.gumtreediff.tree.ITree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeclarationUtil {

    public static List<String> getMethodDeclarations(ITree root) {
        List<ITree> children = root.getChildren();
        // 设置一个集合存储children
        Set<ITree> typeDeclarationSet = new HashSet<>();
        for (ITree child : children) {
            if ((child.getType().name).equals("TypeDeclaration")) {
                typeDeclarationSet.add(child);
            }
        }
        // 设置一个集合储存所有的method
        Set<ITree> methodDeclarationSet = new HashSet<>();
        // 从typeDeclarationSet中取出所有的type的name为MethodDeclaration的ITree
        for (ITree typeDeclaration : typeDeclarationSet) {
            List<ITree> typeDeclarationChildren = typeDeclaration.getChildren();
            for (ITree typeDeclarationChild : typeDeclarationChildren) {
                if (typeDeclarationChild.getType().name.equals("MethodDeclaration")) {
                    methodDeclarationSet.add(typeDeclarationChild);
                }
            }
        }
//        return methodDeclarationSet;
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
            //过滤掉没用的
            if (!((child.getType().name).equals("Block") ||
                    (child.getType().name).equals("MarkerAnnotation") ||
                    (child.getType().name).equals("SingleMemberAnnotation") ||
                    (child.getType().name).equals("NormalAnnotation")||
                    (child.getType().name).equals("Javadoc")
            )
            ){
                if ((child.getType().name).equals("SingleVariableDeclaration")) {
//                    content.append("(");
                    content.append(extractDeclaration(child));
//                    content.append(")");
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
            // 这个地方根本拿不到数据集合类型，因为iASTMapper这部分没有储存！！！
//            content.append(tree.getType().name);
//            content.append("<");
            List<ITree> children = tree.getChildren();
            for (int i = 0; i < children.size(); i++) {
                content.append(extractDeclaration(children.get(i)));
//                content.append(" ");
            }
//            content.append(">");
        }
        content.append(" ");
        return content.toString();
    }
}


