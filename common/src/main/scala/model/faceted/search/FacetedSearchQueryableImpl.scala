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

// scalastyle:off
import model.EntityType
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse}
import org.elasticsearch.index.query.QueryStringQueryBuilder._
import org.elasticsearch.index.query._
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.histogram.{DateHistogramInterval, Histogram}
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.aggregations.metrics.cardinality.Cardinality
import org.elasticsearch.search.aggregations.metrics.max.Max
import org.elasticsearch.search.aggregations.metrics.min.Min
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
// import utils.Timing

import utils.RichString.richString

import scala.collection.JavaConversions._

object FacetedSearch {

  private val client = new ESTransportClient
  def fromIndexName(name: String): FacetedSearchQueryable = new FacetedSearchQueryableImpl(client, name)
}

class FacetedSearchQueryableImpl(clientService: SearchClientService, index: String) extends FacetedSearchQueryable {

  private val docDateField = "Created"
  private val docXDateField = "SimpleTimeExpresion"
  private val docContentField = "Content"

  private val histogramAggName = "histogram"

  // These two fields differ from the generic metadata
  private val keywordsField = "Keywords" -> "Keywords.Keyword.raw"
  private val entityIdsField = "Entities" -> "Entities.EntId"

  // TODO: in course of making other entity types available, we need to adapt these hardcoded labels
  private val entityTypeToField = Map(
    EntityType.Person -> "Entitiesper.EntId",
    EntityType.Organization -> "Entitiesorg.EntId",
    EntityType.Location -> "Entitiesloc.EntId",
    EntityType.Misc -> "Entitiesmisc.EntId"
  )

  private val yearMonthDayPattern = "yyyy-MM-dd"
  private val yearMonthPattern = "yyyy-MM"
  private val yearPattern = "yyyy"
  private val yearMonthDayFormat = DateTimeFormat.forPattern(yearMonthDayPattern)
  private val yearMonthFormat = DateTimeFormat.forPattern(yearMonthPattern)
  private val yearFormat = DateTimeFormat.forPattern(yearPattern)

  // TODO: Move to companion
  // Aggregation fields for each ES index
  private lazy val aggregationToField: Map[String, String] = {
    aggregationFields(index).map(k => k -> s"$k.raw").toMap ++ Map(keywordsField, entityIdsField)
  }

  // Always remove these fields from the aggregation.
  private val defaultExcludedAggregations = List(docContentField)
  private val defaultAggregationSize = 15

  // ----------------------------------------------------------------------------------
  // Document filtering
  //
  // ----------------------------------------------------------------------------------

  private def createQuery(facets: Facets): QueryBuilder = {
    if (facets.isEmpty) {
      QueryBuilders.matchAllQuery()
    } else {
      val request = QueryBuilders.boolQuery()

      addFulltextQuery(facets).map(request.must)
      addGenericFilter(facets).map(request.must)
      addEntitiesFilter(facets).map(request.must)
      addDateFilter(facets).map(request.must)
      addDateXFilter(facets).map(request.must)

      request
    }
  }

  private def addFulltextQuery(facets: Facets): Option[QueryStringQueryBuilder] = {
    if (facets.fullTextSearch.nonEmpty) {
      // Add trailing quote if number of quotes is uneven e.g "Angela
      // ES cannot parse query otherwise.
      val terms = facets.fullTextSearch.map {
        case term if term.count(_ == '"') % 2 != 0 => term + "\""
        case term => term
      }
      val query = QueryBuilders
        .queryStringQuery(terms.mkString(" "))
        .field(docContentField)
        .defaultOperator(Operator.AND)
      Some(query)
    } else {
      None
    }
  }

  private def addGenericFilter(facets: Facets): List[BoolQueryBuilder] = {
    facets.generic.flatMap {
      case (k, v) =>
        val filter = QueryBuilders.boolQuery()
        // Query for raw field
        v.map(meta => filter.should(QueryBuilders.termQuery(s"$k.raw", meta)))
    }.toList
  }

  private def addEntitiesFilter(facets: Facets): List[TermQueryBuilder] = {
    facets.entities.map {
      QueryBuilders.termQuery(entityIdsField._2, _)
    }
  }

  private def addDateFilter(facets: Facets): Option[BoolQueryBuilder] = {
    addGenericDateFilter(docDateField, facets.fromDate, facets.toDate, yearMonthDayPattern)
  }

  private def addDateXFilter(facets: Facets): Option[BoolQueryBuilder] = {
    addGenericDateFilter(docXDateField, facets.fromXDate, facets.toXDate, s"$yearMonthDayPattern || $yearMonthPattern || $yearPattern")
  }

  private def addGenericDateFilter(field: String, from: Option[LocalDateTime], to: Option[LocalDateTime], dateFormat: String): Option[BoolQueryBuilder] = {
    if (from.isDefined || to.isDefined) {
      val query = QueryBuilders.boolQuery()
      val dateFilter = QueryBuilders
        .rangeQuery(field)
        .format(dateFormat)

      val gteFilter = from.map(d => dateFilter.gte(d.toString(yearMonthDayFormat))).getOrElse(dateFilter)
      val lteFilter = to.map(d => dateFilter.lte(d.toString(yearMonthDayFormat))).getOrElse(gteFilter)

      Some(query.must(lteFilter))
    } else {
      None
    }
  }

  // ----------------------------------------------------------------------------------
  // Timeline
  //
  // ----------------------------------------------------------------------------------

  override def histogram(facets: Facets, levelOfDetail: LoD.Value): Aggregation = {
    val lodToFormat = Map(
      LoD.overview -> yearPattern,
      LoD.decade -> yearPattern,
      LoD.year -> yearMonthPattern,
      LoD.month -> yearMonthDayPattern
    )
    val parser = (r: SearchResponse) => parseHistogram(r, histogramAggName)

    createDateHistogram(facets, docDateField, facets.fromDate, facets.toDate, levelOfDetail, parser, lodToFormat)
  }

  override def timeXHistogram(facets: Facets, levelOfDetail: LoD.Value): Aggregation = {
    // The first element in the format is used as bucket key format
    val lodToFormat = Map(
      LoD.overview -> s"$yearPattern || $yearMonthPattern || $yearMonthDayPattern",
      LoD.decade -> s"$yearPattern || $yearMonthPattern || $yearMonthDayPattern",
      LoD.year -> s"$yearMonthPattern || $yearPattern || $yearMonthDayPattern",
      LoD.month -> s"$yearMonthDayPattern || $yearMonthPattern || $yearPattern"
    )
    val parser = (r: SearchResponse) => parseXHistogram(r, histogramAggName, levelOfDetail, facets.fromXDate, facets.toXDate)

    createDateHistogram(facets, docXDateField, facets.fromXDate, facets.toXDate, levelOfDetail, parser, lodToFormat)
  }

  private def createDateHistogram(facets: Facets, dateField: String, fromDate: Option[LocalDateTime], toDate: Option[LocalDateTime], levelOfDetail: LoD.Value, parser: SearchResponse => Aggregation, lodToFormat: Map[LoD.Value, String]): Aggregation = {
    var requestBuilder = createSearchRequest(facets)
    val (format, level, minBound, maxBound) = getParameter(fromDate, toDate, levelOfDetail, lodToFormat)

    val histogramAgg = AggregationBuilders
      .dateHistogram(histogramAggName)
      .field(dateField)
      .interval(level)
      .format(format)
      .minDocCount(0)

    val boundedAgg = if (minBound.isDefined || maxBound.isDefined) histogramAgg.extendedBounds(minBound.get, maxBound.get) else histogramAgg
    requestBuilder = requestBuilder.addAggregation(boundedAgg)

    val response = executeRequest(requestBuilder, cache = false)
    val result = parser(response)

    levelOfDetail match {
      // Post process result if the overview is requested
      case LoD.overview =>
        val collectionFirstDate = minDate(dateField)
        val collectionLastDate = maxDate(dateField)

        groupToOverview(result.buckets, collectionFirstDate, collectionLastDate)
      case _ => result
    }
  }

  private def getParameter(fromDate: Option[LocalDateTime], toDate: Option[LocalDateTime], levelOfDetail: _root_.model.faceted.search.LoD.Value, lodToFormat: Map[_root_.model.faceted.search.LoD.Value, String]): (String, DateHistogramInterval, Option[String], Option[String]) = {
    levelOfDetail match {
      case LoD.overview =>
        assert(fromDate.isEmpty)
        assert(toDate.isEmpty)
        (lodToFormat(levelOfDetail), DateHistogramInterval.YEAR, None, None)
      case LoD.decade =>
        val from = fromDate.map(_.toString(yearFormat))
        val to = toDate.map(_.toString(yearFormat))
        (lodToFormat(levelOfDetail), DateHistogramInterval.YEAR, from, to)
      case LoD.year =>
        val from = fromDate.map(_.toString(yearMonthFormat))
        val to = toDate.map(_.toString(yearMonthFormat))
        (lodToFormat(levelOfDetail), DateHistogramInterval.MONTH, from, to)
      case LoD.month =>
        val from = fromDate.map(_.toString(yearMonthDayFormat))
        val to = toDate.map(_.toString(yearMonthDayFormat))
        (lodToFormat(levelOfDetail), DateHistogramInterval.DAY, from, to)
      case _ => throw new IllegalArgumentException("Unknown level of detail.")
    }
  }

  private def groupToOverview(originalBuckets: List[Bucket], minDate: LocalDateTime, maxDate: LocalDateTime): Aggregation = {
    def getDecade(date: LocalDateTime) = date.getYear - (date.getYear % 10)
    // Starting decade
    val firstDecade = getDecade(minDate)
    // Number of decades
    val numDecades = (getDecade(maxDate) - firstDecade) / 10

    // Create map from decade start to buckets
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

    Aggregation(histogramAggName, buckets)
  }

  // ----------------------------------------------------------------------------------
  // Dynamic network generation
  //
  // ----------------------------------------------------------------------------------

  override def induceSubgraph(facets: Facets, size: Int): (List[Bucket], List[(Long, Long, Long)]) = {
    val buckets = aggregateEntities(facets, size, thresholdDocCount = 1).buckets
    val rels = induceRelationships(facets, buckets.collect { case NodeBucket(id, _) => id })
    (buckets, rels)
  }

  override def induceSubgraph(facets: Facets, nodeFraction: Map[EntityType.Value, Int], exclude: List[Long] = Nil): (List[Bucket], List[(Long, Long, Long)]) = {
    val buckets = nodeFraction.flatMap {
      case (t, size) =>
        aggregateEntitiesByType(facets, t, size, exclude = exclude).buckets
    }.toList

    val rels = induceRelationships(facets, buckets.collect { case NodeBucket(id, _) => id })
    (buckets, rels)
  }

  override def addNodes(facets: Facets, currentNetwork: List[Long], nodes: List[Long]): (List[NodeBucket], List[(Long, Long, Long)]) = {
    val buckets = aggregateEntities(facets, 1, nodes, thresholdDocCount = 1).buckets.collect { case a @ NodeBucket(_, _) => a }
    // Fetch relationships between new nodes
    val inBetweenRels = induceRelationships(facets, nodes)
    // Fetch relationships between new nodes and current network
    val connectingRels = nodes.flatMap { source =>
      currentNetwork.flatMap { dest => getRelationship(facets, source, dest) }
    }
    (buckets, inBetweenRels ++ connectingRels)
  }

  private def induceRelationships(facets: Facets, nodes: List[Long]): List[(Long, Long, Long)] = {
    val visitedList = scala.collection.mutable.ListBuffer[Long]()
    val rels = nodes.flatMap { source =>
      visitedList.add(source)
      val rest = nodes.filter(!visitedList.contains(_))
      rest.flatMap { dest => getRelationship(facets, source, dest) }
    }
    rels
  }

  private def getRelationship(facets: Facets, source: Long, dest: Long): Option[(Long, Long, Long)] = {
    val t = List(source, dest)
    val agg = aggregateEntities(facets.withEntities(t), 2, t, thresholdDocCount = 1)
    agg match {
      // No edge between both since their frequency is zero
      case Aggregation(_, NodeBucket(nodeA, 0) :: NodeBucket(nodeB, 0) :: Nil) =>
        None
      case Aggregation(_, NodeBucket(nodeA, freqA) :: NodeBucket(nodeB, freqB) :: Nil) =>
        // freqA and freqB are the same since we query for docs containing both
        Some((nodeA, nodeB, freqA))
      case _ => throw new RuntimeException("Wrong bucket type!")
    }
  }

  override def searchDocuments(facets: Facets, pageSize: Int): (Long, Iterator[Long]) = {
    val requestBuilder = createSearchRequest(facets, pageSize)
    val it = new SearchHitIterator(requestBuilder)
    // TODO: We have to figure out, why this returns "4.4.0" with source name kibana as id when we use a matchAllQuery
    (it.hits, it.flatMap(_.id().toLongOpt()))
  }

  override def getNeighborCounts(facets: Facets, entityId: Long): Aggregation = {
    // Add entity id as entities filter in order to receive documents where both co-occur
    val neighborFacets = facets.withEntities(List(entityId))
    cardinalityAggregate(neighborFacets)
  }

  private def cardinalityAggregate(facets: Facets): Aggregation = {
    val requestBuilder = createSearchRequest(facets)
    // Add neighbor aggregation for each NE type
    entityTypeToField.foreach {
      case (eType, f) =>
        val aggregation = AggregationBuilders
          .cardinality(eType.toString)
          .field(f)

        requestBuilder.addAggregation(aggregation)
    }
    val response = executeRequest(requestBuilder)
    // Parse result
    val buckets = entityTypeToField.map {
      case (eType, _) =>
        val agg: Cardinality = response.getAggregations.get(eType.toString)
        MetaDataBucket(eType.toString, agg.getValue)
    }.toList

    Aggregation("neighbors", buckets)
  }

  /* override def getDocument(docId: Long, fields: List[String]): Map[String, String] = {
    val response = clientService.client.prepareGet(currentIndexName, null, docId.toString).setFields(fields:_*).execute().actionGet()
    val result = response.getSource.mapValues(_.toString)
  } */

  // ----------------------------------------------------------------------------------
  // Term Aggregations
  //
  // ----------------------------------------------------------------------------------

  override def aggregateAll(
    facets: Facets,
    size: Int = defaultAggregationSize,
    excludedAggregations: List[String] = List()
  ): List[Aggregation] = {
    val excluded = defaultExcludedAggregations ++ excludedAggregations
    val validAggregations = aggregationToField.filterKeys(!excluded.contains(_))

    termAggregate(facets, validAggregations.map { case (k, v) => (k, (v, size)) })
  }

  override def aggregate(facets: Facets, aggregationKey: String, size: Int, include: List[String] = Nil, exclude: List[String] = Nil, thresholdDocCount: Int = 0): Aggregation = {
    val field = aggregationToField(aggregationKey)
    termAggregate(facets, Map(aggregationKey -> (field, size)), include, exclude, thresholdDocCount).head
  }

  override def aggregateKeywords(facets: Facets, size: Int, include: List[String] = Nil): Aggregation = {
    aggregate(facets, keywordsField._1, size, include, Nil, 1)
  }

  override def aggregateEntities(facets: Facets, size: Int, include: List[Long] = Nil, exclude: List[Long] = Nil, thresholdDocCount: Int = 0): Aggregation = {
    aggregate(facets, entityIdsField._1, size, include.map(_.toString), exclude.map(_.toString), thresholdDocCount)
  }

  override def aggregateEntitiesByType(facets: Facets, etype: EntityType.Value, size: Int, include: List[Long] = Nil, exclude: List[Long] = Nil): Aggregation = {
    val agg = Map(entityIdsField._1 -> (entityTypeToField(etype), size))
    termAggregate(facets, agg, include.map(_.toString), exclude.map(_.toString), 1).head
  }

  private def termAggregate(facets: Facets, aggs: Map[String, (String, Int)], include: List[String] = Nil, exclude: List[String] = Nil, thresholdDocCount: Int = 0): List[Aggregation] = {
    var requestBuilder = createSearchRequest(facets)

    val nonEmptyAggs = aggs.collect {
      // Ignore aggregations with zero size since ES returns all indexed types in this case.
      // We do not want this behaviour and return Aggregations with empty buckets instead.
      case (entry @ (k, (v, size))) if size != 0 =>
        // Default order is bucket size desc
        val agg = AggregationBuilders.terms(k)
          .field(v)
          .size(size)
          // Include empty buckets
          .minDocCount(thresholdDocCount)

        // Apply filter to the aggregation request
        val includeAggOpt = if (include.isEmpty) agg else agg.include(include.toArray)
        val excludeAggOpt = if (exclude.isEmpty) includeAggOpt else includeAggOpt.exclude(exclude.toArray)
        requestBuilder = requestBuilder.addAggregation(excludeAggOpt)
        entry
    }
    val response = executeRequest(requestBuilder)
    // There is no need to call shutdown, since this node is the only one in the cluster.
    parseResult(response, nonEmptyAggs, include) ++ aggs.collect { case ((k, (_, 0))) => Aggregation(k, List()) }
  }

  // ----------------------------------------------------------------------------------
  // Aggregation result parsing
  //
  // ----------------------------------------------------------------------------------

  /**
   * Convert response to our internal model
   */
  // TODO: Refactor with creatorMethod that receives a method that creates certain Bucket instances, because both
  // directions are almost the same ...
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

        val resBuckets = if (response.getHits.getTotalHits == 0) List() else buckets
        Aggregation(k, resBuckets ::: zeroEntities.map(s => NodeBucket(s.toInt, 0)))
      case (k, (v, s)) =>
        val agg: Terms = response.getAggregations.get(k)
        val buckets = agg.getBuckets.map(b => MetaDataBucket(b.getKeyAsString, b.getDocCount)).toList

        val resBuckets = if (response.getHits.getTotalHits == 0) buckets.filter(b => filters.contains(b.key)) else buckets
        Aggregation(k, resBuckets)
    }
    res.toList
  }

  // TODO: was not able to incorporate this into the parseResult method. Cannot cast at runtime :(
  private def parseHistogram(response: SearchResponse, key: String): Aggregation = {
    val agg = response.getAggregations.get(key).asInstanceOf[Histogram]
    val buckets = agg.getBuckets.map(b => MetaDataBucket(b.getKeyAsString, b.getDocCount)).toList
    Aggregation(key, buckets)
  }

  private def parseXHistogram(response: SearchResponse, key: String, lod: LoD.Value, from: Option[LocalDateTime], to: Option[LocalDateTime]): Aggregation = {
    val agg = response.getAggregations.get(key).asInstanceOf[Histogram]
    val buckets = agg.getBuckets.collect {
      // Filter date buckets, which are out of the given range.
      case b if lod != LoD.overview && isBetweenInclusive(from.get, to.get, b.getKeyAsString) =>
        MetaDataBucket(b.getKeyAsString, b.getDocCount)
      // Take everything from ES for the overview
      case b if lod == LoD.overview =>
        MetaDataBucket(b.getKeyAsString, b.getDocCount)
    }.toList
    Aggregation(key, buckets)
  }

  // ----------------------------------------------------------------------------------
  // Connection utils
  //
  // ----------------------------------------------------------------------------------

  private def createSearchRequest(facets: Facets, documentSize: Int = 0): SearchRequestBuilder = {
    val requestBuilder = clientService.client.prepareSearch(index)
      .setQuery(createQuery(facets))
      .setSize(documentSize)
      // We are only interested in the document id
      .addFields("id")

    requestBuilder
  }

  private def executeRequest(request: SearchRequestBuilder, cache: Boolean = true): SearchResponse = request.setRequestCache(cache).execute().actionGet()

  // ----------------------------------------------------------------------------------
  // Misc utils
  //
  // ----------------------------------------------------------------------------------

  private def aggregationFields(index: String): List[String] = {
    val res = clientService.client.admin().indices().getMappings(new GetMappingsRequest().indices(index)).get()
    val mapping = res.mappings().get(index)
    val terms = mapping.flatMap { m =>
      val source = m.value.sourceAsMap()
      val properties = source.get("properties").asInstanceOf[java.util.LinkedHashMap[String, java.util.LinkedHashMap[String, String]]]
      properties.keySet()
    }
    terms.toList
  }

  private def isBetweenInclusive(from: LocalDateTime, to: LocalDateTime, target: String): Boolean = {
    val targetDate = LocalDateTime.parse(target)
    !targetDate.isBefore(from) && !targetDate.isAfter(to)
  }

  private def minDate(field: String): LocalDateTime = {
    val aggName = "min_aggregation"
    val requestBuilder = createSearchRequest(Facets.empty)
    val agg = AggregationBuilders.min(aggName).field(field)
    requestBuilder.addAggregation(agg)

    val response = executeRequest(requestBuilder)
    val res: Min = response.getAggregations.get(aggName)

    LocalDateTime.parse(res.getValueAsString, yearMonthDayFormat)
  }

  private def maxDate(field: String): LocalDateTime = {
    val aggName = "max_aggregation"
    val requestBuilder = createSearchRequest(Facets.empty)
    val agg = AggregationBuilders.max(aggName).field(field)
    requestBuilder.addAggregation(agg)

    val response = executeRequest(requestBuilder)
    val res: Max = response.getAggregations.get(aggName)

    LocalDateTime.parse(res.getValueAsString, yearMonthDayFormat)
  }
}

object HistogramTestable extends App {
  val decadeFrom = LocalDateTime.parse("2000-01-01", DateTimeFormat.forPattern("yyyy-MM-dd"))
  val decadeTo = LocalDateTime.parse("2009-12-31", DateTimeFormat.forPattern("yyyy-MM-dd"))

  val yearFrom = LocalDateTime.parse("2000-01-01", DateTimeFormat.forPattern("yyyy-MM-dd"))
  val yearTo = LocalDateTime.parse("2009-12-31", DateTimeFormat.forPattern("yyyy-MM-dd"))

  val monthFrom = LocalDateTime.parse("2000-12-01", DateTimeFormat.forPattern("yyyy-MM-dd"))
  val monthTo = LocalDateTime.parse("2000-12-31", DateTimeFormat.forPattern("yyyy-MM-dd"))

  val filter = Facets(List(), Map(), List(), None, None, Some(monthFrom), Some(monthTo))
  val overview = Facets(List(), Map(), List(), None, None, None, None)

  var res = FacetedSearch.fromIndexName("enron").timeXHistogram(filter, LoD.month)
  //var res = FacetedSearch.fromIndexName("enron").timeXHistogram(overview, LoD.overview)
  println(res)
}

/* object Testable extends App {

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
  val entityFacets = Facets(List(), genericSimple, List(), None, None)
  val complexFacets = Facets(List("\"Bill Clinton\" Merkel", "\"Frank White\""), genericComplex, List(), None, None)
  val zeroFacets = Facets(List("\"wdawdad\""), genericComplex, List(), None, None)


  //FacetedSearch.aggregateEntities(emptyFacets.withEntities(List(902475, 1352530)), 2, List(902475, 1352530))

  /*val ns = Timing.time {
   // (1 to 4).foreach { d =>
      println(FacetedSearch.induceSubgraph(emptyFacets, 5))
   // }
  }
  System.err.println()
  System.err.println("Completed in " + Timing.Seconds.format(ns) + " seconds")*/

  // println(FacetedSearch.aggregateAll(dateRangeFacets, 10, List("Header")))
  // println(FacetedSearch.aggregateEntities(complexFacets, 4, List(653341)))
  // println(FacetedSearch.aggregateEntities(entityFacets, 4, List()))
  // println(FacetedSearch.aggregateEntities(emptyFacets, 4, List()))

  // TODO write unit tests since there are so many expectations. However, this should work for now.
  def asAggregation(key: String, buckets: (String, Int)*) = {
    Aggregation(key, buckets.map { case (a, b) => MetaDataBucket(a, b) }.toList)
  }
  assert(FacetedSearch.aggregate(entityFacets, "Tags", 4, List("PREL")) == asAggregation("Tags", ("PREL", 70681)))
  assert(FacetedSearch.aggregate(entityFacets, "Tags", 2, List()) == asAggregation("Tags", ("PREL", 70681), ("PGOV", 62763)))
  assert(FacetedSearch.aggregate(zeroFacets, "Tags", 4, List()) == asAggregation("Tags"))
  assert(FacetedSearch.aggregate(zeroFacets, "Tags", 4, List("PREL")) == asAggregation("Tags", ("PREL", 0)))

  // println(FacetedSearch.aggregateEntities(entityFacets, 4, List(9)))
  // println(FacetedSearch.aggregateEntities(complexFacets, 4, List(653341, 3)))
  // println(FacetedSearch.aggregateEntities(complexFacets, 10, List()))
  //println(FacetedSearch.aggregateEntitiesByType(entityFacets, EntityType.Misc, 10, List()))
  println(FacetedSearch.aggregateEntities(Facets(List(), Map(), List(268826), None, None), 100, List(), 1))
  // println(FacetedSearch.aggregate(complexFacets, "Tags", 4, List("ASEC", "SAMA")))
  // println(FacetedSearch.aggregate(emptyFacets, "Tags", 4))
  // println(FacetedSearch.aggregateKeywords(f, 4))
  // val hitIterator = FacetedSearch.searchDocuments(emptyFacets, 21)

  // val (numDocs, hitIterator) = FacetedSearch.searchDocuments(complexFacets, 21)
  // println(hitIterator.count(_ => true))
  // println(numDocs)
} */ 