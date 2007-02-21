package org.jetbrains.plugins.scala.lang.surroundWith.surrounders

/**
 * @author: Dmitry Krasilschikov
 */

import com.intellij.psi.PsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange

import org.jetbrains.plugins.scala.lang.psi.impl.expressions._
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElementImpl

class ScalaWithIfBracesSurrounder extends ScalaWithIfSurrounder {
  override def getExpressionTemplateAsString (expr : ASTNode) =
    if (!isNeedBraces(expr)) "if (a) " + "{" + "\n" + expr.getText + "\n" + "}"
    else "(" + "if (a) " + "{" + "\n" + expr.getText + "\n" + "}" + ")"

  override def getTemplateDescription = "if (condition) {expression}"
}




