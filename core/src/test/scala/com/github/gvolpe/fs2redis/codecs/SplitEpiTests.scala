/*
 * Copyright 2018-2019 Fs2 Redis
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

package com.github.gvolpe.fs2redis.codecs

import cats.Eq
import com.github.gvolpe.fs2redis.codecs.laws.SplitEpiLaws
import com.github.gvolpe.fs2redis.codecs.splits.SplitEpi
import org.scalacheck.Arbitrary
import org.scalacheck.Prop._
import org.typelevel.discipline.Laws
import cats.laws.discipline._

// Credits to Rob Norris (@tpolecat) -> https://skillsmatter.com/skillscasts/11626-keynote-pushing-types-and-gazing-at-the-stars
trait SplitEpiTests[A, B] extends Laws {
  def laws: SplitEpiLaws[A, B]

  def splitEpi(implicit a: Arbitrary[A], b: Arbitrary[B], eqA: Eq[A], eqB: Eq[B]): RuleSet =
    new DefaultRuleSet(
      name = "split epimorphism",
      parent = None,
      "identity" -> forAll(laws.identity _),
      "idempotence" -> forAll(laws.idempotence _)
    )
}

object SplitEpiTests {
  def apply[A, B](epi: SplitEpi[A, B]): SplitEpiTests[A, B] =
    new SplitEpiTests[A, B] {
      val laws = SplitEpiLaws[A, B](epi)
    }
}
