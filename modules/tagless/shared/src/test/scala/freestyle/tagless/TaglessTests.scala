/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 package freestyle.tagless

import org.scalatest.{Matchers, WordSpec}

import freestyle.{free, module, tagless}

import cats.{~>, Monad}
import cats.arrow.FunctionK
import cats.free.Free
import cats.instances.either._
import cats.instances.option._
import cats.syntax.flatMap._
import cats.syntax.functor._

import algebras._
import handlers._
import modules._

class TaglessTests extends WordSpec with Matchers {

  "Tagless final algebras" should {

    "Allow a trait with an F[_] bound type param" in {
      "@tagless trait X[F[_]] { def bar(x:Int): FS[Int] }" should compile
    }

    "combine with other tagless algebras" in {

      def program[F[_] : Monad : TG1 : TG2: TG3] = {
        val a1 = TG1[F]
        val a2 = TG2[F]
        val a3 = TG3[F]
        for {
          a <- a1.x(1)
          b <- a1.y(1)
          c <- a2.x2(1)
          d <- a2.y2(1)
          e <- a3.y3(1)
        } yield a + b + c + d + e
      }
      program[Option] shouldBe Option(5)
    }

    "combine with FreeS monadic comprehensions" in {
      import freestyle._
      import freestyle.implicits._

      def program[F[_] : F1: TG1.StackSafe : TG2.StackSafe]: FreeS[F, Int] = {
        val tg1 = TG1.StackSafe[F]
        val tg2 = TG2.StackSafe[F]
        val fs = F1[F]
        val x : FreeS[F, Int] = for {
          a <- fs.a(1)
          tg2a <- tg2.x2(1)
          b <- fs.b(1)
          x <- tg1.x(1)
          tg2b <- tg2.y2(1)
          y <- tg1.y(1)
        } yield a + b + x + y + tg2a + tg2b
        x
      }
      import TG1._
      program[App.Op].interpret[Option] shouldBe Option(6)
    }

    "work with derived handlers" in {

      def program[F[_]: TG1: Monad] =
        for {
          x <- TG1[F].x(1)
          y <- TG1[F].y(2)
        } yield x + y

      type ErrorOr[A] = Either[String, A]

      implicit val fk: Option ~> ErrorOr =
        λ[Option ~> ErrorOr](_.toRight("error"))

      program[ErrorOr] shouldBe Right(3)
    }

  }

}

object algebras {

  @tagless
  trait TG1 {
    def x(a: Int): FS[Int]

    def y(a: Int): FS[Int]
  }

  @tagless
  trait TG2 {
    def x2(a: Int): FS[Int]

    def y2(a: Int): FS[Int]
  }

  @tagless
  trait TG3[F[_]] {
    def x3(a: Int): FS[Int]

    def y3(a: Int): FS[Int]
  }

  @free
  trait F1 {
    def a(a: Int): FS[Int]

    def b(a: Int): FS[Int]
  }

}

object handlers  {

  import algebras._

  implicit val optionHandler1: TG1.Handler[Option] = new TG1.Handler[Option] {
    def x(a: Int): Option[Int] = Some(a)

    def y(a: Int): Option[Int] = Some(a)
  }

  implicit val optionHandler2: TG2.Handler[Option] = new TG2.Handler[Option] {
    def x2(a: Int): Option[Int] = Some(a)

    def y2(a: Int): Option[Int] = Some(a)
  }

  implicit val optionHandler3: TG3.Handler[Option] = new TG3.Handler[Option] {
    def x3(a: Int): Option[Int] = Some(a)

    def y3(a: Int): Option[Int] = Some(a)
  }

  implicit val f1OptionHandler1: F1.Handler[Option] = new F1.Handler[Option] {
    def a(a: Int): Option[Int] = Some(a)

    def b(a: Int): Option[Int] = Some(a)
  }

  implicit def mIdentity[F[_]]: F ~> F = FunctionK.id[F]

}

object modules {

  import algebras._

  @module trait App {
    val f1: F1
    val tg1: TG1.StackSafe
    val tg2: TG2.StackSafe
  }

}

object utils {

  import algebras._

  val iterations = 50000

  def SOProgram[F[_] : Monad : TG1](i: Int): F[Int] = for {
    j <- TG1[F].x(i + 1)
    z <- if (j < iterations) SOProgram[F](j) else Monad[F].pure(j)
  } yield z

}