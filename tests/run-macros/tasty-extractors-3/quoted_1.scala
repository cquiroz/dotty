import scala.quoted._

import given scala.quoted.autolift._

object Macros {

  implicit inline def printTypes[T](x: => T): Unit =
    ${impl('x)}

  def impl[T](x: Expr[T]) given (qctx: QuoteContext): Expr[Unit] = {
    import qctx.tasty._

    val buff = new StringBuilder
    val traverser = new TreeTraverser {
      override def traverseTree(tree: Tree)(implicit ctx: Context): Unit = tree match {
        case IsTypeBoundsTree(tree) =>
          buff.append(tree.tpe.showExtractors)
          buff.append("\n\n")
          traverseTreeChildren(tree)
        case IsTypeTree(tree) =>
          buff.append(tree.tpe.showExtractors)
          buff.append("\n\n")
          traverseTreeChildren(tree)
        case _ =>
          super.traverseTree(tree)
      }
    }

    val tree = x.unseal
    traverser.traverseTree(tree)
    '{print(${buff.result()})}
  }
}
