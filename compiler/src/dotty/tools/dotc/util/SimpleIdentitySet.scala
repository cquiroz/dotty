package dotty.tools.dotc.util

import collection.mutable.ListBuffer

/** A simple linked set with `eq` as the comparison, optimized for small sets.
 *  It has linear complexity for `contains`, `+`, and `-`.
 */
abstract class SimpleIdentitySet[+Elem <: AnyRef] {
  def size: Int
  final def isEmpty: Boolean = size == 0
  def + [E >: Elem <: AnyRef](x: E): SimpleIdentitySet[E]
  def - [E >: Elem <: AnyRef](x: E): SimpleIdentitySet[Elem]
  def contains[E >: Elem <: AnyRef](x: E): Boolean
  def foreach(f: Elem => Unit): Unit
  def exists[E >: Elem <: AnyRef](p: E => Boolean): Boolean
  def /: [A, E >: Elem <: AnyRef](z: A)(f: (A, E) => A): A
  def toList: List[Elem]
  final def ++ [E >: Elem <: AnyRef](that: SimpleIdentitySet[E]): SimpleIdentitySet[E] =
    ((this: SimpleIdentitySet[E]) /: that)(_ + _)
  final def -- [E >: Elem <: AnyRef](that: SimpleIdentitySet[E]): SimpleIdentitySet[Elem] =
    (this /: that)(_ - _)
  override def toString: String = toList.mkString("(", ", ", ")")
}

object SimpleIdentitySet {
  object empty extends SimpleIdentitySet[Nothing] {
    def size: Int = 0
    def + [E <: AnyRef](x: E): SimpleIdentitySet[E] =
      new Set1[E](x)
    def - [E <: AnyRef](x: E): SimpleIdentitySet[Nothing] =
      this
    def contains[E <: AnyRef](x: E): Boolean = false
    def foreach(f: Nothing => Unit): Unit = ()
    def exists[E <: AnyRef](p: E => Boolean): Boolean = false
    def /: [A, E <: AnyRef](z: A)(f: (A, E) => A): A = z
    def toList = Nil
  }

  private class Set1[+Elem <: AnyRef](x0: AnyRef) extends SimpleIdentitySet[Elem] {
    def size = 1
    def + [E >: Elem <: AnyRef](x: E): SimpleIdentitySet[E] =
      if (contains(x)) this else new Set2[E](x0, x)
    def - [E >: Elem <: AnyRef](x: E): SimpleIdentitySet[Elem] =
      if (x `eq` x0) empty else this
    def contains[E >: Elem <: AnyRef](x: E): Boolean = x `eq` x0
    def foreach(f: Elem => Unit): Unit = f(x0.asInstanceOf[Elem])
    def exists[E >: Elem <: AnyRef](p: E => Boolean): Boolean =
      p(x0.asInstanceOf[E])
    def /: [A, E >: Elem <: AnyRef](z: A)(f: (A, E) => A): A =
      f(z, x0.asInstanceOf[E])
    def toList = x0.asInstanceOf[Elem] :: Nil
  }

  private class Set2[+Elem <: AnyRef](x0: AnyRef, x1: AnyRef) extends SimpleIdentitySet[Elem] {
    def size = 2
    def + [E >: Elem <: AnyRef](x: E): SimpleIdentitySet[E] =
      if (contains(x)) this else new Set3(x0, x1, x)
    def - [E >: Elem <: AnyRef](x: E): SimpleIdentitySet[Elem] =
      if (x `eq` x0) new Set1(x1)
      else if (x `eq` x1) new Set1(x0)
      else this
    def contains[E >: Elem <: AnyRef](x: E): Boolean = (x `eq` x0) || (x `eq` x1)
    def foreach(f: Elem => Unit): Unit = { f(x0.asInstanceOf[Elem]); f(x1.asInstanceOf[Elem]) }
    def exists[E >: Elem <: AnyRef](p: E => Boolean): Boolean =
      p(x0.asInstanceOf[E]) || p(x1.asInstanceOf[E])
    def /: [A, E >: Elem <: AnyRef](z: A)(f: (A, E) => A): A =
      f(f(z, x0.asInstanceOf[E]), x1.asInstanceOf[E])
    def toList = x0.asInstanceOf[Elem] :: x1.asInstanceOf[Elem] :: Nil
  }

  private class Set3[+Elem <: AnyRef](x0: AnyRef, x1: AnyRef, x2: AnyRef) extends SimpleIdentitySet[Elem] {
    def size = 3
    def + [E >: Elem <: AnyRef](x: E): SimpleIdentitySet[E] =
      if (contains(x)) this
      else {
        val xs = new Array[AnyRef](4)
        xs(0) = x0
        xs(1) = x1
        xs(2) = x2
        xs(3) = x
        new SetN[E](xs)
      }
    def - [E >: Elem <: AnyRef](x: E): SimpleIdentitySet[Elem] =
      if (x `eq` x0) new Set2(x1, x2)
      else if (x `eq` x1) new Set2(x0, x2)
      else if (x `eq` x2) new Set2(x0, x1)
      else this
    def contains[E >: Elem <: AnyRef](x: E): Boolean = (x `eq` x0) || (x `eq` x1) || (x `eq` x2)
    def foreach(f: Elem => Unit): Unit = {
      f(x0.asInstanceOf[Elem]); f(x1.asInstanceOf[Elem]); f(x2.asInstanceOf[Elem])
    }
    def exists[E >: Elem <: AnyRef](p: E => Boolean): Boolean =
      p(x0.asInstanceOf[E]) || p(x1.asInstanceOf[E]) || p(x2.asInstanceOf[E])
    def /: [A, E >: Elem <: AnyRef](z: A)(f: (A, E) => A): A =
      f(f(f(z, x0.asInstanceOf[E]), x1.asInstanceOf[E]), x2.asInstanceOf[E])
    def toList = x0.asInstanceOf[Elem] :: x1.asInstanceOf[Elem] :: x2.asInstanceOf[Elem] :: Nil
  }

  private class SetN[+Elem <: AnyRef](xs: Array[AnyRef]) extends SimpleIdentitySet[Elem] {
    def size = xs.length
    def + [E >: Elem <: AnyRef](x: E): SimpleIdentitySet[E] =
      if (contains(x)) this
      else {
        val xs1 = new Array[AnyRef](size + 1)
        System.arraycopy(xs, 0, xs1, 0, size)
        xs1(size) = x
        new SetN[E](xs1)
      }
    def - [E >: Elem <: AnyRef](x: E): SimpleIdentitySet[Elem] = {
      var i = 0
      while (i < size && (xs(i) `ne` x)) i += 1
      if (i == size) this
      else if (size == 4)
        if (i == 0) new Set3(xs(1), xs(2), xs(3))
        else if (i == 1) new Set3(xs(0), xs(2), xs(3))
        else if (i == 2) new Set3(xs(0), xs(1), xs(3))
        else new Set3(xs(0), xs(1), xs(2))
      else {
        val xs1 = new Array[AnyRef](size - 1)
        System.arraycopy(xs, 0, xs1, 0, i)
        System.arraycopy(xs, i + 1, xs1, i, size - (i + 1))
        new SetN(xs1)
      }
    }
    def contains[E >: Elem <: AnyRef](x: E): Boolean = {
      var i = 0
      while (i < size && (xs(i) `ne` x)) i += 1
      i < size
    }
    def foreach(f: Elem => Unit): Unit = {
      var i = 0
      while (i < size) { f(xs(i).asInstanceOf[Elem]); i += 1 }
    }
    def exists[E >: Elem <: AnyRef](p: E => Boolean): Boolean =
      xs.asInstanceOf[Array[E]].exists(p)
    def /: [A, E >: Elem <: AnyRef](z: A)(f: (A, E) => A): A =
      (z /: xs.asInstanceOf[Array[E]])(f)
    def toList: List[Elem] = {
      val buf = new ListBuffer[Elem]
      foreach(buf += _)
      buf.toList
    }
  }
}
