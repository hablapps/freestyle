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

// The purpose of this test is to see if freestyle still compiles classes in _root_ package. 

import freestyle._

@free trait Logitech[F[_]] {
  def eyes(s: String): FreeS.Par[F, Boolean]
  def skills(s: String): FreeS.Par[F, Boolean]
}

@free trait Asia[F[_]] {
  def recycled(msg: String): FreeS[F, Unit]
  def rhino(prompt: String): FreeS[F, String]
}

@module trait Age[F[_]] {
  val logitech: Logitech[F]
  val asia: Asia[F]
}