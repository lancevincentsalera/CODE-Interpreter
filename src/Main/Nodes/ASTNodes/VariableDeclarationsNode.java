package Main.Nodes.ASTNodes;
import Main.Nodes.ASTNodes.ASTNode;
import Main.Nodes.ASTNodes.EvaluableNode;
import Main.Nodes.ASTNodes.SingleVariableDeclaration;
import Main.Nodes.SymbolTable;

import java.util.*;

public class VariableDeclarationsNode extends ASTNode {
    private final List<SingleVariableDeclaration> variableDeclarations;

    public VariableDeclarationsNode(List<SingleVariableDeclaration> variableDeclarations) {
        this.variableDeclarations = variableDeclarations;
    }

    public List<SingleVariableDeclaration> getVariableDeclarations() {
        return variableDeclarations;
    }

}


