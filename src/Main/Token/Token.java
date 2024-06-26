package Main.Token;

public class Token {
  private final Type tpe;
  private final String text;
  private final int startPos;

  public Token(Type tpe, String text, int startPos) {
      this.tpe = tpe;
      this.text = text;
      this.startPos = startPos;
  }

  public Type getType() {
      return tpe;
  }

  public String getText() {
      return text;
  }

  public int getStartPos() {
      return startPos;
  }

  @Override
  public String toString() {
      return "Token{" +
              "type=" + tpe +
              ", text='" + text + '\'' +
              ", startPos=" + startPos +
              '}';
  }

  public enum Type {
      Num, Plus, Times, Divide, Modulo, Minus, Greater, Less, GreaterEqual, LessEqual,
      NotEqual, Assign, And, Or, Not, Identifier, BeginContainer, EndContainer, BeginIf, EndIf,
      BeginWhile, EndWhile, Int, Bool, Char, Float, If, IfElse, Else, Equals, Comma, Print, 
      Parentheses, Concat, NewLine, BooleanLiteral, Scan, NumFloat,
      CharLiteral, Colon, Escape, StringLiteral, Negation, While, Break, For, EndFor, BeginFor, Continue,
      Comment, BlankLine
  }
}
