package com.pjanof

import scala.language.higherKinds

object FreeExample {

  trait Monad[M[_]] {
    def pure[A](a: A): M[A] // return
    def flatMap[A,B](ma: M[A])(f: A => M[B]): M[B] // bind
  }

  object Monad {
    def apply[F[_]:Monad]: Monad[F] = implicitly[Monad[F]]
  }

  sealed trait ~>[F[_],G[_]] { self =>
    def apply[A](f: F[A]): G[A]

    def or[H[_]](f: H ~> G): ({ type f[x] = Coproduct[F, H, x]})#f ~> G =
      new (({type f[x] = Coproduct[F,H,x]})#f ~> G) {
        def apply[A](c: Coproduct[F,H,A]): G[A] = c.run match {
          case Left(fa) => self(fa)
          case Right(ha) => f(ha)
        }
      }
  }

  sealed trait Free[F[_],A] {
    def flatMap[B](f: A => Free[F,B]): Free[F,B] =
      this match {
        case Return(a) => f(a)
        case Bind(fx, g) =>
          Bind(fx, g andThen (_ flatMap f))
      }

    def map[B](f: A => B): Free[F,B] =
      flatMap(a => Return(f(a)))

    def foldMap[G[_]:Monad](f: F ~> G): G[A] =
      this match {
        case Return(a) => Monad[G].pure(a)
        case Bind(fx, g) =>
          Monad[G].flatMap(f(fx)) { a =>
            g(a).foldMap(f)
          }
      }
  }

  case class Return[F[_],A](a: A) extends Free[F,A]

  case class Bind[F[_],I,A](
    a: F[I],
    f: I => Free[F,A]) extends Free[F,A]

  type Id[A] = A

  implicit val identityMonad: Monad[Id] = new Monad[Id] {
    def pure[A](a: A) = a
    def flatMap[A,B](a: A)(f: A => B) = f(a)
  }

  case class Coproduct[F[_],G[_],A](run: Either[F[A],G[A]])
}
