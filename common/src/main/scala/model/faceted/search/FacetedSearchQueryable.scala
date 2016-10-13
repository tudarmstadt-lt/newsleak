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

package model.faceted.search

import model.EntityType

trait FacetedSearchQueryable {

  def histogram(facets: Facets, levelOfDetail: LoD.Value): Aggregation

  /**
   *
   * @param facets
   * @return tuple (entities, relationships)
   */
  def induceSubgraph(facets: Facets, size: Int): (List[Bucket], List[(Long, Long, Long)])

  def induceSubgraph(facets: Facets, nodeFraction: Map[EntityType.Value, Int], exclude: List[Long]): (List[Bucket], List[(Long, Long, Long)])

  /**
   * Returns an iterator that contains ids of matched documents.
   *
   * Example:
   *
   * val hitIterator = FacetedSearch.searchDocuments(None, facets, 10)
   * hitIterator.map(id => [...])
   *
   * @param facets
   * @param pageSize controls the number of elements per page. If you take more than
   *                 pageSize elements from the iterator it will reload the next pageSize
   *                 elements from the data store. Recommended value 10.
   * @return tuple (num, it), where num represents the number of matched documents and it
   *         is a iterator consisting of document ids.
   */
  def searchDocuments(facets: Facets, pageSize: Int): (Long, Iterator[Long])

  def getNeighborCounts(facets: Facets, entityId: Long): Aggregation

  /**
   * Example:
   * aggregate(Some("Clinton"), facets, "SignedBy", 4)
   *
   * @param facets
   * @param aggregationKey
   * @param size
   * @param filter the response will only contain the instances given in this list.
   * @return
   */
  def aggregate(facets: Facets, aggregationKey: String, size: Int, include: List[String], exclude: List[String], thresholdDocCount: Int = 0): Aggregation

  def aggregateKeywords(facets: Facets, size: Int, include: List[String]): Aggregation

  def aggregateEntities(facets: Facets, size: Int, include: List[Long], exclude: List[Long], thresholdDocCount: Int = 0): Aggregation

  def aggregateEntitiesByType(facets: Facets, etype: EntityType.Value, size: Int, include: List[Long], exclude: List[Long]): Aggregation

  /**
   * Applies the given facets and full-text search to the underling document collection and returns aggregations
   * for all available metadata. The response also contains an aggregation for prominent
   * entities.
   *
   * @param fullTextSearch match documents that contain the given term. None, if no full-text
   *                        search should be used.
   *
   * @param facets maps from metadata term keys to a list of possible instances for the given
   *               term key. Multiple instances as values and facets will be joined via 'and'.
   *
   *               The following example will query for documents that are 'CONFIDENTIAL' and
   *               tagged with 'ASEC' as well as 'PREL'.
   *
   *               val facets = Map(
   *                 "Classification" -> List("CONFIDENTIAL"),
   *                  "Tags" -> List("ASEC", "PREL")
   *               )
   *               aggregateAll(Some("Clinton"), facets, List("Header"))
   *
   * @param excludedAggregations given metadata keys will be excluded from the result. In the default
   *                            case, aggregations for all available metadata will be executed. Use
   *                            Exclude irrelevant fields to speed up the execution.
   *
   * @return Result contains aggregation for all available metadata and a subset of nodes that are
   *         prominent for the retrieved subset of documents.
   */
  def aggregateAll(facets: Facets, size: Int, excludedAggregations: List[String] = List()): List[Aggregation]
}
