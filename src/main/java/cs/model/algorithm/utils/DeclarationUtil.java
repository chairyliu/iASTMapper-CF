package cs.model.algorithm.utils;

import com.github.gumtreediff.tree.ITree;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeclarationUtil {

    public static Set<ITree> getMethodDeclarations(ITree root) {
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
        return methodDeclarationSet;
    }
}
