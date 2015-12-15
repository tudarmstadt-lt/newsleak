/*
 * Copyright (C) 2015  Language Technology Group
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils

import testFactories.FlatSpecWithCommonTraits

class SequentialNumbererTest extends FlatSpecWithCommonTraits {

  val uut = new SequentialNumberer[String]

  it should "convert externalId to same internalId" in {
    val expected = "Angela"
    val internalId = uut.externalToInternal(expected)
    val actual = uut.internalToExternal(internalId)

    assert(expected === actual)
  }

  it should "not convert different externalId's to the same indernalId" in {

    val internalId1 = uut.externalToInternal("Angela")
    val internalId2 = uut.externalToInternal("Peter")

    val externalId1 = uut.internalToExternal(internalId1)
    val externalId2 = uut.internalToExternal(internalId2)

    assert(!(externalId1 === externalId2))
  }
}
