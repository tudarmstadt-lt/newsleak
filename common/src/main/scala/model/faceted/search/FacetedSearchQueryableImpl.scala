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
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.QueryStringQueryBuilder._
import org.elasticsearch.index.query.{QueryStringQueryBuilder, BoolQueryBuilder, QueryBuilder, QueryBuilders}
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.histogram.{DateHistogramInterval, Histogram}
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat

// scalastyle:off
import scala.collection.JavaConversions._

import utils.RichString.richString

object FacetedSearch extends FacetedSearchQueryableImpl

class FacetedSearchQueryableImpl extends FacetedSearchQueryable {

  private val clientService = new ESTransportClient
  private val elasticSearchIndex = "cable"
  // These two fields differ from the generic metadata
  private val keywordsField = "Keywords" -> "Keywords.Keyword.raw"
  private val entityIdsField = "Entities" -> "Entities.EntId"

  private val yearMonthDayPattern = "yyyy-MM-dd"
  private val yearMonthPattern = "yyyy-MM"
  private val yearPattern = "yyyy"
  private val yearMonthDayFormat = DateTimeFormat.forPattern(yearMonthDayPattern)
  private val yearMonthFormat = DateTimeFormat.forPattern(yearMonthPattern)
  private val yearFormat = DateTimeFormat.forPattern(yearPattern)

  private lazy val aggregationToField =
    aggregationFields().map(k => k -> s"$k.raw").toMap ++ Map(keywordsField, entityIdsField)

  // Always remove these fields from the aggregation.
  private val defaultExcludedAggregations = List("Content")
  private val defaultAggregationSize = 15

  private def aggregationFields(): List[String] = {
    val res = clientService.client.admin().indices().getMappings(new GetMappingsRequest().indices(elasticSearchIndex)).get()
    val mapping = res.mappings().get(elasticSearchIndex)
    val terms = mapping.flatMap { m =>
      val source = m.value.sourceAsMap()
      val properties = source.get("properties").asInstanceOf[java.util.LinkedHashMap[String, java.util.LinkedHashMap[String, String]]]
      properties.keySet()
    }
    terms.toList
  }

  private def createQuery(facets: Facets): QueryBuilder = {
    if (facets.isEmpty) {
      QueryBuilders.matchAllQuery()
    } else {
      val request = QueryBuilders.boolQuery()

      request
        .must(addFulltextQuery(facets).getOrElse(QueryBuilders.boolQuery()))
        .must(addGenericFilter(facets))
        .must(addEntitiesFilter(facets))
        .must(addDateFilter(facets))

      request
    }
  }

  private def addFulltextQuery(facets: Facets): Option[QueryStringQueryBuilder] = {
    if (facets.fullTextSearch.nonEmpty) {
      val luceneQuery = facets.fullTextSearch.mkString(" ")
      val query = QueryBuilders
        .queryStringQuery(luceneQuery)
        .field("Content")
        .defaultOperator(Operator.AND)
      Some(query)
    } else {
      None
    }
  }

  private def addGenericFilter(facets: Facets): BoolQueryBuilder = {
    // Search for generic facets
    val query = QueryBuilders.boolQuery()
    facets.generic.foreach {
      case (k, v) =>
        val filter = QueryBuilders.boolQuery()
        // Query for raw field
        v.map(meta => filter.should(QueryBuilders.termQuery(s"$k.raw", meta)))
        query.must(filter)
    }
    query
  }

  private def addEntitiesFilter(facets: Facets): BoolQueryBuilder = {
    val entitiesFilter = QueryBuilders.boolQuery()
    facets.entities.map { e =>
      entitiesFilter.must(QueryBuilders.termQuery(entityIdsField._2, e))
    }
    entitiesFilter
  }

  private def addDateFilter(facets: Facets): BoolQueryBuilder = {
    val query = QueryBuilders.boolQuery()
    if (facets.fromDate.isDefined || facets.toDate.isDefined) {
      val dateFilter = QueryBuilders
        .rangeQuery("Created")
        .format(yearMonthDayPattern)

      val gteFilter = facets.fromDate.map(d => dateFilter.gte(d.toString(yearMonthDayFormat))).getOrElse(dateFilter)
      val lteFilter = facets.toDate.map(d => dateFilter.lte(d.toString(yearMonthDayFormat))).getOrElse(gteFilter)

      query.must(lteFilter)
    } else {
      query
    }
  }

  /**
   * Convert response to our internal model
   */
  // TODO: Refactor with creatorMethod that receives a method that creates certain Bucket instances ...
  private def parseResult(response: SearchResponse, aggregations: Map[String, (String, Int)], filters: List[String]): List[Aggregation] = {
    val res = aggregations.collect {
      // Create node bucket for entities
      case (k, (v, s)) if k == entityIdsField._1 =>
        val agg: Terms = response.getAggregations.get(k)
        val buckets = agg.getBuckets.collect {
          // If include filter is given don't add zero count entries (will be post processed)
          case (b) if filters.nonEmpty && filters.contains(b.getKeyAsString) => NodeBucket(b.getKeyAsNumber.longValue(), b.getDocCount)
          case (b) if filters.isEmpty => NodeBucket(b.getKeyAsNumber.longValue(), b.getDocCount)
        }.toList
        // We need to add missing zero buckets for entities filters manually,
        // because aggregation is not able to process long ids with zero buckets
        val addedBuckets = buckets.map(_.id)
        val zeroEntities = filters.filterNot(s => addedBuckets.contains(s.toInt))

        Aggregation(k, buckets ::: zeroEntities.map(s => NodeBucket(s.toInt, 0)))
      case (k, (v, s)) =>
        val agg: Terms = response.getAggregations.get(k)
        val buckets = agg.getBuckets.map(b => MetaDataBucket(b.getKeyAsString, b.getDocCount)).toList
        Aggregation(k, buckets)
    }
    res.toList
  }

  // TODO: was not able to incorporate this into the parseResult method. Cannot cast at runtime :(
  private def parseHistogram(response: SearchResponse, key: String): Aggregation = {
    val agg = response.getAggregations.get(key).asInstanceOf[Histogram]
    val buckets = agg.getBuckets.map(b => MetaDataBucket(b.getKeyAsString, b.getDocCount)).toList
    Aggregation(key, buckets)
  }

  private def groupToOverview(originalBuckets: List[Bucket]): Aggregation = {
    def getDecade(date: LocalDateTime) = date.getYear - (date.getYear % 10)
    // Starting decade
    val collectionFirstYear = LocalDateTime.parse(originalBuckets.head.asInstanceOf[MetaDataBucket].key, yearFormat)
    val firstDecade = getDecade(collectionFirstYear)
    // Number of decades
    val collectionLastYear = LocalDateTime.parse(originalBuckets.last.asInstanceOf[MetaDataBucket].key, yearFormat)
    val numDecades = (getDecade(collectionLastYear) - firstDecade) / 10

    //Create map from decade start to buckets
    val decadeToCount = originalBuckets.collect {
      case (b: MetaDataBucket) =>
        val decade = getDecade(LocalDateTime.parse(b.key, yearFormat))
        decade -> b.docCount
    }.groupBy(_._1).mapValues(_.map(_._2))

    val buckets = (0 to numDecades).map { decade =>
      val startDecade = firstDecade + 10 * decade
      val endDecade = firstDecade + 9 + 10 * decade
      val key = s"$startDecade-$endDecade"
      MetaDataBucket(key, decadeToCount.getOrElse(startDecade, Nil).sum)
    }.toList

    Aggregation("histogram", buckets)
  }

  override def histogram(facets: Facets, levelOfDetail: LoD.Value): Aggregation = {
    var requestBuilder = clientService.client.prepareSearch(elasticSearchIndex)
      .setQuery(createQuery(facets))
      .setSize(0)

    val (format, level, minBound, maxBound) = levelOfDetail match {
      case LoD.overview =>
        assert(facets.fromDate.isEmpty)
        assert(facets.toDate.isEmpty)
        (yearPattern, DateHistogramInterval.YEAR, None, None)
      case LoD.decade =>
        val from = facets.fromDate.map(_.toString(yearFormat))
        val to = facets.toDate.map(_.toString(yearFormat))
        (yearPattern, DateHistogramInterval.YEAR, from, to)
      case LoD.year =>
        val from = facets.fromDate.map(_.toString(yearMonthFormat))
        val to = facets.toDate.map(_.toString(yearMonthFormat))
        (yearMonthPattern, DateHistogramInterval.MONTH, from, to)
      case LoD.month =>
        val from = facets.fromDate.map(_.toString(yearMonthDayFormat))
        val to = facets.toDate.map(_.toString(yearMonthDayFormat))
        (yearMonthDayPattern.toString, DateHistogramInterval.DAY, from, to)
      case _ => throw new IllegalArgumentException("Unknown level of detail.")
    }

    val histogramAgg = AggregationBuilders
      .dateHistogram("histogram")
      .field("Created")
      .interval(level)
      .format(format)
      .minDocCount(0)

    val boundedAgg = if (minBound.isDefined || maxBound.isDefined) histogramAgg.extendedBounds(minBound.get, maxBound.get) else histogramAgg
    requestBuilder = requestBuilder.addAggregation(boundedAgg)

    val response = requestBuilder.execute().actionGet()
    val result = parseHistogram(response, "histogram")
    levelOfDetail match {
      // Post process result if the overview is requested
      case LoD.overview =>
        groupToOverview(result.buckets)
      case _ => result
    }
  }

  override def induceSubgraph(facets: Facets, size: Int): (List[Bucket], List[(Long, Long, Long)]) = {

    val nodeBuckets = aggregateEntities(facets, size, Nil).buckets
    val nodes = nodeBuckets.collect { case NodeBucket(id, _) => id }

    val visitedList = scala.collection.mutable.ListBuffer[Long]()
    val rels = nodes.flatMap { source =>
      visitedList.add(source)
      val rest = nodes.filter(!visitedList.contains(_))
      rest.flatMap { dest =>
        val t = List(source, dest)
        val agg = FacetedSearch.aggregateEntities(facets.withEntities(t), 2, t)
        println(agg)
        agg.buckets
          .collect { case NodeBucket(id, freq) if freq != 0 => (id, freq) }
          .sliding(2).map {
            case List((nodeA, freqA), (nodeB, freqB)) if nodeA != nodeB =>
              assert(nodeA == source || nodeA == dest)
              assert(nodeB == source || nodeB == dest)
              assert(freqA == freqB)
              println((source, dest, freqA))
              (source, dest, freqA)
          }.toList
      }
    }
    (nodeBuckets, rels)
  }

  override def searchDocuments(facets: Facets, pageSize: Int): (Long, Iterator[Long]) = {
    val requestBuilder = clientService.client.prepareSearch(elasticSearchIndex)
      .setQuery(createQuery(facets))
      .setSize(pageSize)

    val it = new SearchHitIterator(requestBuilder)
    // TODO: We have to figure out, why this returns "4.4.0" with source name kibana as id when we use a matchAllQuery
    (it.hits, it.flatMap(_.id().toLongOpt()))
  }

  override def aggregateAll(
    facets: Facets,
    size: Int = defaultAggregationSize,
    excludedAggregations: List[String] = List()
  ): List[Aggregation] = {
    val excluded = defaultExcludedAggregations ++ excludedAggregations
    val validAggregations = aggregationToField.filterKeys(!excluded.contains(_))

    aggregate(facets, validAggregations.map { case (k, v) => (k, (v, size)) }, Nil)
  }

  override def aggregate(facets: Facets, aggregationKey: String, size: Int, filter: List[String]): Aggregation = {
    val field = aggregationToField(aggregationKey)
    aggregate(facets, Map(aggregationKey -> (field, size)), filter).head
  }

  override def aggregateKeywords(facets: Facets, size: Int, filter: List[String]): Aggregation = {
    aggregate(facets, keywordsField._1, size, filter)
  }

  override def aggregateEntitiesByType(facets: Facets, etype: EntityType.Value, size: Int, filter: List[Long]): Aggregation = {
    val agg = aggregate(facets, entityIdsField._1, size * 7, filter.map(_.toString))

    def isType(id: Long, t: EntityType.Value) = {
      val entity = model.Entity.getById(id)
      entity.exists(_.entityType == t)
    }
    val buckets = agg.buckets.collect { case b @ NodeBucket(k, _) if isType(k, etype) => b }.take(size)
    Aggregation(agg.key, buckets)
  }

  override def aggregateEntities(facets: Facets, size: Int, filter: List[Long]): Aggregation = {
    aggregate(facets, entityIdsField._1, size, filter.map(_.toString))
  }

  private def aggregate(facets: Facets, aggs: Map[String, (String, Int)], filter: List[String]): List[Aggregation] = {
    var requestBuilder = clientService.client.prepareSearch(elasticSearchIndex)
      .setQuery(createQuery(facets))
      .setSize(0)
      // We are only interested in the document id
      .addFields("id")

    for ((k, (v, size)) <- aggs) {
      // Default order is bucket size desc
      val agg = AggregationBuilders.terms(k)
        .field(v)
        .size(size)
        // Include empty buckets
        .minDocCount(0)

      // Apply filter to the aggregation request
      val filteredAgg = if (filter.isEmpty) agg else agg.include(filter.toArray)
      requestBuilder = requestBuilder.addAggregation(filteredAgg)
    }

    println(requestBuilder)

    val response = requestBuilder.execute().actionGet()
    // There is no need to call shutdown, since this node is the only
    // one in the cluster.
    parseResult(response, aggs, filter)
  }
}

/* object HistogramTestable extends App {
  // Format should be yyyy-MM-dd


  val decadeFrom = LocalDateTime.parse("1990-01-01", DateTimeFormat.forPattern("yyyy-MM-dd"))
  val decadeTo = LocalDateTime.parse("1999-12-31", DateTimeFormat.forPattern("yyyy-MM-dd"))

  val yearFrom = LocalDateTime.parse("1985-01-01", DateTimeFormat.forPattern("yyyy-MM-dd"))
  val yearTo = LocalDateTime.parse("1985-12-31", DateTimeFormat.forPattern("yyyy-MM-dd"))

  val overviewFacet = Facets(List(), Map(), List(), None, None)
  val decadeFacet = Facets(List(), Map(), List(), Some(decadeFrom), Some(decadeTo))
  val yearFacet = Facets(List(), Map(), List(), Some(yearFrom), Some(yearTo))

  val monthFrom = LocalDateTime.parse("1985-12-01", DateTimeFormat.forPattern("yyyy-MM-dd"))
  val monthTo = LocalDateTime.parse("1985-12-31", DateTimeFormat.forPattern("yyyy-MM-dd"))
  // Fulltext filter, metadata filter, entities filter, from and to range filter
  val monthFacet = Facets(List(), Map(), List(), Some(monthFrom), Some(monthTo))
  println(FacetedSearch.histogram(monthFacet, LoD.month))
  //println(FacetedSearch.histogram(yearFacet, LoD.year))
  //println(FacetedSearch.histogram(decadeFacet, LoD.decade))
  //println(FacetedSearch.histogram(overviewFacet, LoD.overview))
} */

object Testable extends App {

  val genericSimple = Map(
    "Classification" -> List("CONFIDENTIAL")
  )

  val genericComplex = Map(
    "Classification" -> List("CONFIDENTIAL", "UNCLASSIFIED"),
    "Tags" -> List("ASEC", "PREL")
  )

  // Format should be yyyy-MM-dd
  val from = LocalDateTime.parse("1985-01-01", DateTimeFormat.forPattern("yyyy-MM-dd"))
  val to = LocalDateTime.parse("1985-12-31", DateTimeFormat.forPattern("yyyy-MM-dd"))

  val emptyFacets = Facets(List(), Map(), List(), None, None)
  val dateRangeFacets = Facets(List(), Map(), List(), Some(from), Some(to))
  val entityFacets = Facets(List(), genericSimple, List(999999), None, None)
  val complexFacets = Facets(List("\"Bill Clinton\" Merkel", "\"Frank White\""), genericComplex, List(), None, None)

  println(FacetedSearch.induceSubgraph(emptyFacets, 5))

  // println(FacetedSearch.aggregateAll(dateRangeFacets, 10, List("Header")))
  // println(FacetedSearch.aggregateEntities(complexFacets, 4, List(653341)))
  // println(FacetedSearch.aggregateEntities(complexFacets, 4, List(653341, 3)))
  // println(FacetedSearch.aggregateEntities(complexFacets, 10, List()))
  // println(FacetedSearch.aggregateEntitiesByType(complexFacets, EntityType.Person, 10, List()))
  // println(FacetedSearch.aggregate(complexFacets, "Tags", 4, List("ASEC", "SAMA")))
  // println(FacetedSearch.aggregate(emptyFacets, "Tags", 4))
  // println(FacetedSearch.aggregateKeywords(f, 4))
  // val hitIterator = FacetedSearch.searchDocuments(emptyFacets, 21)

  // val (numDocs, hitIterator) = FacetedSearch.searchDocuments(complexFacets, 21)
  // println(hitIterator.count(_ => true))
  // println(numDocs)
}