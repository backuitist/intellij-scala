package org.jetbrains.plugins.scala
package codeInspection.redundantBlock

import com.intellij.codeInspection.{ProblemDescriptor, ProblemsHolder}
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import codeInspection.{AbstractFix, AbstractInspection}
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScBlock, ScThisReference, ScReferenceExpression, ScBlockExpr}
import lang.refactoring.util.ScalaNamesUtil
import org.jetbrains.plugins.scala.extensions.childOf
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScCaseClause
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory

/**
 * Pavel Fatin
 */

class RedundantBlockInspection extends AbstractInspection {

  def actionFor(holder: ProblemsHolder) = {
    case (block: ScBlock) childOf ((blockOfExpr: ScBlock) childOf (_: ScCaseClause))
      if blockOfExpr.getChildren.length == 1 && block.hasRBrace =>
      holder.registerProblem(block, "Remove redundant braces", new InCaseClauseQuickFix(block))
    case block: ScBlockExpr if block.getChildren.length == 3 =>
      val child: PsiElement = block.getChildren.apply(1)
      val probablyRedundant = child match {
        case ref: ScReferenceExpression if ref.qualifier.isEmpty => true
        case t: ScThisReference if t.reference.isEmpty => true
        case _ => false
      }
      if (probablyRedundant) {
        val next: PsiElement = block.getNextSibling
        val isRedundant =
          if (next == null) true
          else if (child.getText.startsWith("_")) false //SCL-6124
          else {
            val refName: String = child.getText + (if (next.getText.length > 0) next.getText charAt 0 else "")
            !ScalaNamesUtil.isIdentifier(refName) && !refName.exists(_ == '$') 
          }
        if (isRedundant) {
          holder.registerProblem(block, "The enclosing block is redundant", new QuickFix(block))
        }
      }
  }

  private class QuickFix(e: PsiElement) extends AbstractFix("Unwrap the expression", e) {
    def doApplyFix(project: Project, descriptor: ProblemDescriptor) {
      e.replace(e.getChildren.apply(1))
    }
  }
  
  private class InCaseClauseQuickFix(block: ScBlock) extends AbstractFix("Remove redundant braces", block) {
    def doApplyFix(project: Project, descriptor: ProblemDescriptor): Unit = {
      val stmts = block.statements
      if (stmts.length == 1)
        block.replace(stmts(0))
      else {
        for (stmt <- stmts; parent = block.getParent) {
          parent.addBefore(ScalaPsiElementFactory.createNewLine(parent.getManager), block)
          parent.addBefore(stmt, block)
        }
        block.delete()
      }
    }
  }
}
