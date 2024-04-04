    package Main.Token.Lexer.Parser;

    import Main.ExceptionHandlers.*;
    import Main.Nodes.*;
    import Main.Token.Token;

    import java.util.*;

    public class Parser {
        private final List<Token> tokens;
        private int currentTokenIndex;
        private final VariableInitializerNode variableInitializer = new VariableInitializerNode();


        public Parser(List<Token> tokens) {
            this.tokens = tokens;
            currentTokenIndex = 0;
        }



        // call parse method to start parsing
        public ASTNode parse() throws VariableDeclarationException, VariableInitializationException, DisplayException, BeginContainerMissingException, EndContainerMissingException, SyntaxErrorException {
            return program();
        }

        private boolean match(Token.Type expectedType) {
            if (currentTokenIndex < tokens.size() && tokens.get(currentTokenIndex).getType() == expectedType) {
                currentTokenIndex++;
                return true;
            }
            return false;
        }



    // Program --> BEGIN CODE VariableDeclarations ExecutableCode END CODE
    private ASTNode program() throws VariableDeclarationException, VariableInitializationException, DisplayException, BeginContainerMissingException, EndContainerMissingException, InputMismatchException, SyntaxErrorException {
        if(match(Token.Type.BeginContainer)) {
            ASTNode variableDeclarations = variableDeclarations();
            reinitializeVariable((VariableDeclarationsNode) variableDeclarations);
            scanFunction();
            ASTNode executableCode = executableCode();

            //checking the index
            //System.out.println("Current token index: " + currentTokenIndex);
            //System.out.println("Current token type: " + tokens.get(currentTokenIndex).getType());
            //System.out.println("Current token text: " + tokens.get(currentTokenIndex).getText());
            if(match(Token.Type.EndContainer)) {
                if(tokens.size() == currentTokenIndex) {
                    return new ProgramNode(variableDeclarations, executableCode);
                } else {
                    throw new EndContainerMissingException("END CODE reached but found more tokens.");
                }
            } else {
                throw new EndContainerMissingException("Code should end with an \"END CODE\" syntax.");
            }
        } else {
            throw new BeginContainerMissingException("Code should begin with a \"BEGIN CODE\" syntax.");
        }
    }


        private void scanFunction() throws InputMismatchException, VariableInitializationException, SyntaxErrorException, VariableDeclarationException {
            if (match(Token.Type.Scan)) {
                if (match(Token.Type.Colon)) {
                    List<String> variableNames = variableList();
                    Scanner scanner = new Scanner(System.in);

                    // Prompt for input once, for all variables
                    System.out.print("Enter the values for " + String.join(" and ", variableNames) + ": ");
                    String inputLine = scanner.nextLine();
                    String[] userInputValues = inputLine.split(",");

                    if (userInputValues.length != variableNames.size()) {
                        throw new InputMismatchException("Error: The number of values provided does not match the number of variables.");
                    }

                    for (int i = 0; i < variableNames.size(); i++) {
                        String variableName = variableNames.get(i);
                        String userInput = userInputValues[i].trim(); // Remove any leading/trailing whitespace
                        Object parsedValue = parseInput(userInput);
                        if (parsedValue != null) {
                            LiteralNode valueNode = new LiteralNode(parsedValue);
                            initializeVariable(variableName, valueNode);
                        } else {
                            throw new VariableInitializationException("Error: Invalid input format for variable '" + variableName + "'.");
                        }
                    }
                } else {
                    throw new SyntaxErrorException("Expected colon (:) after SCAN keyword.");
                }
            }
        }

        private Object parseInput(String userInput) {
            // Try to parse user input to various data types
            if (userInput.matches("^\\d+$")) {
                return Integer.parseInt(userInput); // Integer
            } else if (userInput.matches("^\\d*\\.\\d+$")) {
                return Float.parseFloat(userInput); // Float
            } else if (userInput.equalsIgnoreCase("true") || userInput.equalsIgnoreCase("false")) {
                return Boolean.parseBoolean(userInput); // Boolean
            } else if (userInput.length() == 1) {
                return userInput.charAt(0); // Character
            }
            return null; // Invalid input
        }



        public static final Set<String> RESERVED_WORDS = new HashSet<>(Arrays.asList(
                "BEGIN", "CODE", "END", "INT", "CHAR", "BOOL", "FLOAT", "DISPLAY",
                "THEN", "ELSE", "IF", "WHILE", "AND", "OR", "NOT", "TRUE", "FALSE", "SCAN"
        ));


        private boolean isReservedWord(String token) {
            return RESERVED_WORDS.contains(token.toUpperCase());
        }

        // Method to check if a variable name is a reserved word
        private boolean isReservedVariable(String variableName) {
            return isReservedWord(variableName);
        }


        // VariableDeclarations -> VariableDeclaration VariableDeclarations | ε
        private ASTNode variableDeclarations() throws VariableDeclarationException, VariableInitializationException {
            List<SingleVariableDeclaration> variables = new ArrayList<>();

            while (true) {
                try {
                    SingleVariableDeclaration variable = (SingleVariableDeclaration) variableDeclaration();
                    if (variable != null) {
                        variables.add(variable);
                    } else {
                        break;
                    }
                } catch (VariableDeclarationException | VariableInitializationException v) {
                    v.printStackTrace();
                    throw v;
                }
            }
            return new VariableDeclarationsNode(variables);
        }




        // VariableDeclaration -> DataType VariableList
        private ASTNode variableDeclaration() throws VariableDeclarationException, VariableInitializationException {
            String dataType = tokens.get(currentTokenIndex).getText();
            ASTNode variableDeclaration = null;
            if (!dataType.isEmpty() && DataType()) {
    //            System.out.println("dtype:" + dataType);
                List<String> variables = variableList();
    //            System.out.println("variables:" + variables);
                for(String variable : variables) {
                    if(isReservedVariable(variable.toLowerCase())) {
                        throw new VariableDeclarationException("Error: Variable name '" + variable + "' is a reserved word.");
                    }
                    validateAssignmentType(dataType, variable);
                }
                if(variables.isEmpty()) {
                    throw new VariableDeclarationException("Error: Found Data Type token but variable list is empty.");
                }
                variableDeclaration = new SingleVariableDeclaration(dataType, variables);
            }
            return variableDeclaration;
        }



        // VariableList -> VariableName VariableList'
        private List<String> variableList() throws VariableDeclarationException, VariableInitializationException {
            List<String> variableNames = new ArrayList<>();
            String variableName = variableName();
            if (variableName != null) {
                if(isReservedVariable(variableName.toLowerCase())) {
                    throw new VariableDeclarationException("Error: Variable name '" + variableName + "' is a reserved word.");
                }
                /*
                    Check first for the first variable
                    Also include whether it is initialized
                    on declaration
                */
                Object assignmentValue = assignment();
                LiteralNode value;
                if(assignmentValue != null) {
    //                System.out.println("not null");
                    value = new LiteralNode(assignmentValue);
                    initializeVariable(variableName, value);
                }
                /*
                *  */
                variableNames.add(variableName);
                /*     */

                while (match(Token.Type.Comma)) {
                    variableName = variableName();
                    if (variableName != null) {
                        if(isReservedVariable(variableName.toLowerCase())) {
                            throw new VariableDeclarationException("Error: Variable name '" + variableName + "' is a reserved word.");
                        }
                        assignmentValue = assignment();
                        if(assignmentValue != null) {
    //                        System.out.println(variableName + "=" + assignmentValue);
                            value = new LiteralNode(assignmentValue);
                            initializeVariable(variableName, value);
                        }
                        variableNames.add(variableName);
                    } else {
                        throw new VariableDeclarationException("Error parsing variable declarations. " +
                                "Found a comma token without a succeeding variable declaration");
                    }

                }
            } else {
                // If variableName is null, it means no variable was declared.
                // Check if it's because a reserved word was encountered.
                if (match(Token.Type.Identifier) && isReservedVariable(tokens.get(currentTokenIndex - 1).getText().toLowerCase())) {
                    throw new VariableDeclarationException("Error: Variable name '" + tokens.get(currentTokenIndex - 1).getText() + "' is a reserved word.");
                }
            }
            return variableNames;
        }



        // DataType -> INT | CHAR | BOOL | FLOAT
        private boolean DataType() {
            return match(Token.Type.Int) || match(Token.Type.Char) || match(Token.Type.Bool) || match(Token.Type.Float);
        }




        // VariableName -> Identifier
        private String variableName() {
            String variableName = null;
            if (match(Token.Type.Identifier)) {
                variableName = tokens.get(currentTokenIndex - 1).getText();
            }
            return variableName;
        }




        // Assignment -> '=' Expression
        private Object assignment() throws VariableInitializationException {
            if (match(Token.Type.Assign)) {
    //            System.out.println("assignment");
                Object assignedValue = value();
                if(assignedValue != null) {
                    return assignedValue;
                }
                throw new VariableInitializationException("Assignment operator found but " +
                        "value token is missing. Please check again");
            }
            return null;
        }




        // Expression -> Num | NumFloat | CharLiteral | BooleanLiteral
        private Object value() throws VariableInitializationException {
            String value = tokens.get(currentTokenIndex).getText();
            if(match(Token.Type.Identifier) ) {
                String variableName = tokens.get(currentTokenIndex - 1).getText();
                if(!match(Token.Type.Assign)) {
                    currentTokenIndex--;
                    ASTNode expr = expr();
                    if(expr instanceof ArithmeticExpressionNode arithmeticExpressionNode) {
                        LiteralNode val = arithmeticExpressionNode.evaluateExpression();
                        return val.getValue();
                    } else if (variableInitializer.getValue(variableName) != null) {
                        return variableInitializer.getValue(variableName).getValue();
                    } else {
                        throw new VariableInitializationException("Variable " + variableName + " has not been declared nor initialized.");
                    }
                } else {
                    currentTokenIndex--;
                    return "continue";
                }
            } else if (((tokens.get(currentTokenIndex + 1).getType() == Token.Type.Plus || tokens.get(currentTokenIndex + 1).getType() == Token.Type.Minus ||
                    tokens.get(currentTokenIndex + 1).getType() == Token.Type.Times || tokens.get(currentTokenIndex + 1).getType() == Token.Type.Divide ||
                    tokens.get(currentTokenIndex + 1).getType() == Token.Type.Modulo) && (match(Token.Type.Num) || match(Token.Type.NumFloat))) ||
                    match(Token.Type.Parentheses) && tokens.get(currentTokenIndex-1).getText().equals("(") ) {
                currentTokenIndex--;
                ASTNode expr = expr();
                if(expr instanceof ArithmeticExpressionNode arithmeticExpressionNode) {
                    LiteralNode val = arithmeticExpressionNode.evaluateExpression();
                    return val.getValue();
                }
            } else if(match(Token.Type.Num)) {
                return Integer.parseInt(value);
            } else if(match(Token.Type.NumFloat)) {
                return Float.parseFloat(value);
            } else if(match(Token.Type.CharLiteral)) {
                return value.charAt(0);
            } else if (match(Token.Type.BooleanLiteral)) {
                return value;
            }
            return null;
        }




        private void validateAssignmentType(String dataType, String variable) throws VariableInitializationException {
            LiteralNode valueNode = variableInitializer.getValue(variable);
            if (valueNode != null) {
                Object assignedValue = valueNode.getValue();
                if (assignedValue != null) {
                    // Check if the assigned value matches the data type
                    switch (dataType) {
                        case "INT" -> {
                            try {
                                int intValue = Integer.parseInt(String.valueOf(assignedValue));
                                // The assigned value can be parsed as an int, so it matches the data type INT
                            } catch (NumberFormatException e) {
                                throw new VariableInitializationException("Error: Assigned value for variable '" + variable + "' is not a valid integer.");
                            }
                        }
                        case "CHAR" -> {
                            if (!(assignedValue instanceof Character)) {
                                throw new VariableInitializationException("Error: Assigned value for variable '" + variable + "' does not match data type CHAR.");
                            }
                        }
                        case "BOOL" -> {
                            if (!(assignedValue.equals("FALSE") || assignedValue.equals("TRUE"))) {
                                throw new VariableInitializationException("Error: Assigned value for variable '" + variable + "' does not match data type BOOL.");
                            }
                        }
                        case "FLOAT" -> {
                            try {
                                double floatValue = Double.parseDouble(String.valueOf(assignedValue));
                                // The assigned value can be parsed as a double, so it matches the data type FLOAT
                            } catch (NumberFormatException e) {
                                throw new VariableInitializationException("Error: Assigned value for variable '" + variable + "' is not a valid float.");
                            }
                        }
                        case null, default ->
                                throw new VariableInitializationException("Error: Unsupported data type '" + dataType + "'.");
                    }
                }
            }
        }






    // variable initializer
    private void initializeVariable(String varName, LiteralNode value) {
        variableInitializer.setValue(varName, value);
    }





    private ASTNode executableCode() throws DisplayException {
        return displayFunction();
    }





    private void reinitializeVariable(VariableDeclarationsNode declarationStatements) throws VariableInitializationException {
        for(SingleVariableDeclaration declaration : declarationStatements.getVariableDeclarations() ) {
            String dataType = declaration.getDataType();
            List<String> dominoInitializedVariables = new ArrayList<>();
            while(match(Token.Type.Identifier)) {
                String varName = tokens.get(currentTokenIndex-1).getText();
                if(declaration.getVariableNames().contains(varName)) { //true
                    dominoInitializedVariables.add(varName); // x
                    Object assignedValue = assignment(); //
                    LiteralNode literalNode;
                    if(assignedValue != null) {
                        while(assignedValue == "continue") {
                            dominoInitializedVariables.add(tokens.get(currentTokenIndex-1).getText());
                            assignedValue = assignment();
                        }
                        literalNode = new LiteralNode(assignedValue);

                        for (String dominoInitializedVariable : dominoInitializedVariables) {
                            initializeVariable(dominoInitializedVariable, literalNode);
                        }
                    }
                    validateAssignmentType(dataType, varName);
                } else {
                    currentTokenIndex--;
                    break;
                }

            }
        }
    }




    private DisplayNode displayFunction() throws DisplayException {
        if (match(Token.Type.Print)) {
            StringBuilder stringBuilder = new StringBuilder();
            if (match(Token.Type.Colon)) {
                while (true) {
                    if (match(Token.Type.Identifier)) {
                        // Check if the variable is initialized
                        String variableName = tokens.get(currentTokenIndex - 1).getText();
                        LiteralNode value = variableInitializer.getValue(variableName);

                        if (value != null) {
                            stringBuilder.append(value.getValue()); // Append the value to the StringBuilder
                        } else {
                            throw new DisplayException("Variable '" + variableName + "' is not initialized.");
                        }
                    } else if (match(Token.Type.Escape)) {
                        // Handle escape character
                        String escapeSequence = tokens.get(currentTokenIndex - 1).getText();
                        // Append the escape character to the output string
                        stringBuilder.append(escapeSequence);
                    } else if (match(Token.Type.Num) || match(Token.Type.NumFloat) ||
                            match(Token.Type.CharLiteral) || match(Token.Type.BooleanLiteral) ||
                            match(Token.Type.StringLiteral)) {
                        stringBuilder.append(tokens.get(currentTokenIndex - 1).getText());
                    } else if (match(Token.Type.NewLine)) {
                        stringBuilder.append(System.lineSeparator());
                    } else if (match(Token.Type.Concat)) {
                        continue;
                    } else {
                        break;
                    }
                }
                return new DisplayNode(stringBuilder.toString());
            }
        }
        return null;
    }


        private Object parseExpression() throws VariableInitializationException {
        ASTNode expr = expr();
        if(expr instanceof ArithmeticExpressionNode arithmeticExpressionNode) {
            LiteralNode val = arithmeticExpressionNode.evaluateExpression();
            return val.getValue();
        }
        return null;
    }



    private ASTNode expr() throws VariableInitializationException {
        ASTNode term = term();

        if (term == null) {
            throw new VariableInitializationException("Invalid expression format.");
        }
        while (match(Token.Type.Plus) || match(Token.Type.Minus)) {
            Token.Type operator = tokens.get(currentTokenIndex - 1).getType();
            ASTNode rightOperand = term();
            term = new ArithmeticExpressionNode(term, operator, rightOperand);
        }
        return term;
    }


    private  ASTNode term() throws VariableInitializationException {
        ASTNode factor = factor();
        while (match(Token.Type.Times) || match(Token.Type.Divide) || match(Token.Type.Modulo)) {
            Token.Type operator = tokens.get(currentTokenIndex - 1).getType();
            ASTNode rightOperand = factor();
            factor = new ArithmeticExpressionNode(factor, operator, rightOperand);
        }
        return factor;
    }


    private ASTNode factor() throws VariableInitializationException {
        if(match(Token.Type.Num)) {
            return new LiteralNode(Integer.parseInt(tokens.get(currentTokenIndex - 1).getText()));
        } else if(match(Token.Type.Identifier)) {
            return new LiteralNode(variableInitializer.getValue(tokens.get(currentTokenIndex - 1).getText()).getValue());
        }else if(match(Token.Type.NumFloat)) {
            return new LiteralNode(Float.parseFloat(tokens.get(currentTokenIndex - 1).getText()));
        } else if(match(Token.Type.Parentheses) && tokens.get(currentTokenIndex-1).getText().equals("(") ) {
            ASTNode expression = expr();
            if(tokens.get(currentTokenIndex).getText().equals(")") && match(Token.Type.Parentheses)) {
                return expression;
            }
        }
        return null;
    }


}
