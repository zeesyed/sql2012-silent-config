/*
* Copyright 2015 herd contributors
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
package org.finra.herd.dao.impl;

import static org.elasticsearch.index.query.MultiMatchQueryBuilder.Type.BEST_FIELDS;
import static org.elasticsearch.index.query.MultiMatchQueryBuilder.Type.PHRASE_PREFIX;
import static org.elasticsearch.index.query.QueryBuilders.disMaxQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import org.finra.herd.core.HerdStringUtils;
import org.finra.herd.core.helper.ConfigurationHelper;
import org.finra.herd.dao.IndexSearchDao;
import org.finra.herd.dao.TransportClientFactory;
import org.finra.herd.dao.helper.ElasticsearchHelper;
import org.finra.herd.dao.helper.JsonHelper;
import org.finra.herd.model.api.xml.BusinessObjectDefinitionKey;
import org.finra.herd.model.api.xml.Facet;
import org.finra.herd.model.api.xml.Field;
import org.finra.herd.model.api.xml.Highlight;
import org.finra.herd.model.api.xml.IndexSearchRequest;
import org.finra.herd.model.api.xml.IndexSearchResponse;
import org.finra.herd.model.api.xml.IndexSearchResult;
import org.finra.herd.model.api.xml.IndexSearchResultKey;
import org.finra.herd.model.api.xml.TagKey;
import org.finra.herd.model.dto.ConfigurationValue;
import org.finra.herd.model.dto.ElasticsearchResponseDto;
import org.finra.herd.model.dto.IndexSearchHighlightFields;

/**
 * IndexSearchDaoImpl
 */
@Repository
public class IndexSearchDaoImpl implements IndexSearchDao
{
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexSearchDaoImpl.class);

    /**
     * Best fields query boost
     */
    private static final float BEST_FIELDS_QUERY_BOOST = 1f;

    /**
     * The business object definition index
     */
    private static final String BUSINESS_OBJECT_DEFINITION_INDEX = "bdef";

    /**
     * The boost amount for the business object definition index
     */
    private static final float BUSINESS_OBJECT_DEFINITION_INDEX_BOOST = 1f;

    /**
     * String to select the tag type code and namespace code
     */
    private static final String CODE = "code";

    /**
     * Source string for the description
     */
    private static final String DESCRIPTION_SOURCE = "description";

    /**
     * Constant to hold the display name option for the business object definition search
     */
    private static final String DISPLAY_NAME_FIELD = "displayname";

    /**
     * Source string for the display name
     */
    private static final String DISPLAY_NAME_SOURCE = "displayName";

    /**
     * String to select the namespace
     */
    private static final String NAMESPACE = "namespace";

    /**
     * Source string for the namespace code
     */
    private static final String NAMESPACE_CODE_SOURCE = "namespace.code";

    /**
     * Source string for the name
     */
    private static final String NAME_SOURCE = "name";

    /**
     * N-Grams field type
     */
    private static final String FIELD_TYPE_NGRAMS = "ngrams";

    /**
     * Phrase prefix query boost
     */
    private static final float PHRASE_PREFIX_QUERY_BOOST = 10f;

    /**
     * The number of the indexSearch results to return
     */
    private static final int SEARCH_RESULT_SIZE = 200;

    /**
     * Constant to hold the short description option for the business object definition search
     */
    private static final String SHORT_DESCRIPTION_FIELD = "shortdescription";

    /**
     * Stemmed field type
     */
    private static final String FIELD_TYPE_STEMMED = "stemmed";

    /**
     * Source string for the tagCode
     */
    private static final String TAG_CODE_SOURCE = "tagCode";

    /**
     * The tag index
     */
    private static final String TAG_INDEX = "tag";

    /**
     * The boost amount for the tag index
     */
    private static final float TAG_INDEX_BOOST = 1000f;

    /**
     * String to select the tag type
     */
    private static final String TAG_TYPE = "tagType";

    /**
     * Source string for the tagType.code
     */
    private static final String TAG_TYPE_CODE_SOURCE = "tagType.code";

    /**
     * The configuration helper used to retrieve configuration values
     */
    @Autowired
    private ConfigurationHelper configurationHelper;

    /**
     * Helper to deserialize JSON values
     */
    @Autowired
    private JsonHelper jsonHelper;

    /**
     * The transport client factory will create a transport client which is a connection to the elasticsearch index
     */
    @Autowired
    private TransportClientFactory transportClientFactory;

    @Autowired
    private ElasticsearchHelper elasticsearchHelper;

    @Override
    public IndexSearchResponse indexSearch(final IndexSearchRequest request, final Set<String> fields)
    {
        final Integer tagShortDescMaxLength = configurationHelper.getProperty(ConfigurationValue.TAG_SHORT_DESCRIPTION_LENGTH, Integer.class);
        final Integer businessObjectDefinitionShortDescMaxLength =
            configurationHelper.getProperty(ConfigurationValue.BUSINESS_OBJECT_DEFINITION_SHORT_DESCRIPTION_LENGTH, Integer.class);

        // Build two multi match queries, one with phrase prefix, and one with best fields, but boost the phrase prefix
        final MultiMatchQueryBuilder phrasePrefixMultiMatchQueryBuilder =
            buildMultiMatchQuery(request.getSearchTerm(), PHRASE_PREFIX, PHRASE_PREFIX_QUERY_BOOST, FIELD_TYPE_STEMMED);
        final MultiMatchQueryBuilder bestFieldsMultiMatchQueryBuilder =
            buildMultiMatchQuery(request.getSearchTerm(), BEST_FIELDS, BEST_FIELDS_QUERY_BOOST, FIELD_TYPE_NGRAMS);

        QueryBuilder queryBuilder;

        // Add filter clauses if index search filters are specified in the request
        if (CollectionUtils.isNotEmpty(request.getIndexSearchFilters()))
        {
            BoolQueryBuilder indexSearchQueryBuilder = elasticsearchHelper.addIndexSearchFilterBooleanClause(request.getIndexSearchFilters());

            // Add the multi match queries to a dis max query and wrap within a bool query, then apply filters to it
            queryBuilder = QueryBuilders.boolQuery().must(disMaxQuery().add(phrasePrefixMultiMatchQueryBuilder).add(bestFieldsMultiMatchQueryBuilder))
                .filter(indexSearchQueryBuilder);
        }
        else
        {
            // Add only the multi match queries to a dis max query if no filters are specified
            queryBuilder = disMaxQuery().add(phrasePrefixMultiMatchQueryBuilder).add(bestFieldsMultiMatchQueryBuilder);
        }

        // The fields in the search indexes to return
        final String[] searchSources = {NAME_SOURCE, NAMESPACE_CODE_SOURCE, TAG_CODE_SOURCE, TAG_TYPE_CODE_SOURCE, DISPLAY_NAME_SOURCE, DESCRIPTION_SOURCE};

        // Create a new indexSearch source builder
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // Fetch only the required fields
        searchSourceBuilder.fetchSource(searchSources, null);
        searchSourceBuilder.query(queryBuilder);

        // Create a indexSearch request builder
        final TransportClient transportClient = transportClientFactory.getTransportClient();
        SearchRequestBuilder searchRequestBuilder = transportClient.prepareSearch(BUSINESS_OBJECT_DEFINITION_INDEX, TAG_INDEX);
        searchRequestBuilder.setSource(searchSourceBuilder).setSize(SEARCH_RESULT_SIZE)
            .addIndexBoost(BUSINESS_OBJECT_DEFINITION_INDEX, BUSINESS_OBJECT_DEFINITION_INDEX_BOOST).addIndexBoost(TAG_INDEX, TAG_INDEX_BOOST)
            .addSort(SortBuilders.scoreSort());

        String preTag = null;
        String postTag = null;

        // Add highlighting if specified in the request
        if (BooleanUtils.isTrue(request.isEnableHitHighlighting()))
        {
            // Fetch configured 'tag' values for highlighting
            preTag = configurationHelper.getProperty(ConfigurationValue.ELASTICSEARCH_HIGHLIGHT_PRETAGS);
            postTag = configurationHelper.getProperty(ConfigurationValue.ELASTICSEARCH_HIGHLIGHT_POSTTAGS);

            searchRequestBuilder.highlighter(buildHighlightQuery(preTag, postTag));
        }

        // Add facet aggregations if specified in the request
        if (CollectionUtils.isNotEmpty(request.getFacetFields()))
        {
            searchRequestBuilder = elasticsearchHelper.addFacetFieldAggregations(new HashSet<>(request.getFacetFields()), searchRequestBuilder);
        }

        // Log the actual elasticsearch query when debug is enabled
        LOGGER.debug("indexSearchRequest={}", searchRequestBuilder.toString());

        // Retrieve the indexSearch response
        final SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();
        final SearchHits searchHits = searchResponse.getHits();
        final SearchHit[] searchHitArray = searchHits.hits();

        final List<IndexSearchResult> indexSearchResults = new ArrayList<>();

        // For each indexSearch hit
        for (final SearchHit searchHit : searchHitArray)
        {
            // Get the source map from the indexSearch hit
            final Map<String, Object> sourceMap = searchHit.sourceAsMap();

            // Get the index from which this result is from
            final String index = searchHit.getShard().getIndex();

            // Create a new document to populate with the indexSearch results
            final IndexSearchResult indexSearchResult = new IndexSearchResult();

            // Populate the results
            indexSearchResult.setIndexSearchResultType(index);
            if (fields.contains(DISPLAY_NAME_FIELD))
            {
                indexSearchResult.setDisplayName((String) sourceMap.get(DISPLAY_NAME_SOURCE));
            }

            // Populate tag index specific key
            if (index.equals(TAG_INDEX))
            {
                if (fields.contains(SHORT_DESCRIPTION_FIELD))
                {
                    indexSearchResult
                        .setShortDescription(HerdStringUtils.getShortDescription((String) sourceMap.get(DESCRIPTION_SOURCE), tagShortDescMaxLength));
                }

                final TagKey tagKey = new TagKey();
                tagKey.setTagCode((String) sourceMap.get(TAG_CODE_SOURCE));
                tagKey.setTagTypeCode((String) ((Map) sourceMap.get(TAG_TYPE)).get(CODE));
                indexSearchResult.setIndexSearchResultKey(new IndexSearchResultKey(tagKey, null));
            }
            // Populate business object definition key
            else if (index.equals(BUSINESS_OBJECT_DEFINITION_INDEX))
            {
                if (fields.contains(SHORT_DESCRIPTION_FIELD))
                {
                    indexSearchResult.setShortDescription(
                        HerdStringUtils.getShortDescription((String) sourceMap.get(DESCRIPTION_SOURCE), businessObjectDefinitionShortDescMaxLength));
                }

                final BusinessObjectDefinitionKey businessObjectDefinitionKey = new BusinessObjectDefinitionKey();
                businessObjectDefinitionKey.setNamespace((String) ((Map) sourceMap.get(NAMESPACE)).get(CODE));
                businessObjectDefinitionKey.setBusinessObjectDefinitionName((String) sourceMap.get(NAME_SOURCE));
                indexSearchResult.setIndexSearchResultKey(new IndexSearchResultKey(null, businessObjectDefinitionKey));
            }

            if (BooleanUtils.isTrue(request.isEnableHitHighlighting()))
            {
                // Extract highlighted content from the search hit and clean html tags except the pre/post-tags as configured
                Highlight highlightedContent = extractHighlightedContent(searchHit, preTag, postTag);

                // Set highlighted content in the response element
                indexSearchResult.setHighlight(highlightedContent);
            }

            indexSearchResults.add(indexSearchResult);
        }

        List<Facet> facets = null;
        if (CollectionUtils.isNotEmpty(request.getFacetFields()))
        {
            // Extract facets from the search response
            facets = new ArrayList<>(extractFacets(request, searchResponse));
        }

        return new IndexSearchResponse(searchHits.getTotalHits(), indexSearchResults, facets);
    }

    /**
     * Extracts highlighted content from a given {@link SearchHit}
     *
     * @param searchHit a given {@link SearchHit} from the elasticsearch results
     * @param preTag the specified pre-tag for highlighting
     * @param postTag the specified post-tag for highlighting
     *
     * @return {@link Highlight} a cleaned highlighted content
     */
    private Highlight extractHighlightedContent(SearchHit searchHit, String preTag, String postTag)
    {
        Highlight highlightedContent = new Highlight();

        List<Field> highlightFields = new ArrayList<>();

        // make sure there is highlighted content in the search hit
        if (searchHit.getHighlightFields() != null)
        {
            for (Map.Entry<String, HighlightField> entry : searchHit.getHighlightFields().entrySet())
            {
                Field field = new Field();

                // Extract the field-name
                field.setFieldName(entry.getKey());

                List<String> cleanFragments = new ArrayList<>();

                // Extract fragments which have the highlighted content
                Text[] fragments = entry.getValue().getFragments();

                for (Text fragment : fragments)
                {
                    cleanFragments.add(HerdStringUtils.stripHtml(fragment.toString(), preTag, postTag));
                }
                field.setFragments(cleanFragments);
                highlightFields.add(field);
            }
        }

        highlightedContent.setFields(highlightFields);

        return highlightedContent;
    }

    /**
     * Extracts facet information from a {@link SearchResponse} object
     *
     * @param request The specified {@link IndexSearchRequest}
     * @param searchResponse A given {@link SearchResponse} to extract the facet information from
     *
     * @return A list of {@link Facet} objects
     */
    private List<Facet> extractFacets(IndexSearchRequest request, SearchResponse searchResponse)
    {
        ElasticsearchResponseDto elasticsearchResponseDto = new ElasticsearchResponseDto();
        if (request.getFacetFields().contains(ElasticsearchHelper.TAG_FACET))
        {
            elasticsearchResponseDto.setNestTagTypeIndexSearchResponseDtos(elasticsearchHelper.getNestedTagTagIndexSearchResponseDto(searchResponse));
            elasticsearchResponseDto.setTagTypeIndexSearchResponseDtos(elasticsearchHelper.getTagTagIndexSearchResponseDto(searchResponse));
        }
        if (request.getFacetFields().contains(ElasticsearchHelper.RESULT_TYPE_FACET))
        {
            elasticsearchResponseDto.setResultTypeIndexSearchResponseDtos(elasticsearchHelper.getResultTypeIndexSearchResponseDto(searchResponse));
        }

        return elasticsearchHelper.getFacetsResponse(elasticsearchResponseDto, true);
    }

    /**
     * Private method to build a multi match query.
     *
     * @param searchTerm the term on which to search
     *
     * @return the multi match query
     */
    private MultiMatchQueryBuilder buildMultiMatchQuery(final String searchTerm, final MultiMatchQueryBuilder.Type queryType, final float queryBoost,
        final String fieldType)
    {
        final MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.multiMatchQuery(searchTerm).type(queryType);
        multiMatchQueryBuilder.boost(queryBoost);

        if (fieldType.equals(FIELD_TYPE_STEMMED))
        {
            // Get the configured value for 'stemmed' fields and their respective boosts if any
            String stemmedFieldsValue = configurationHelper.getProperty(ConfigurationValue.ELASTICSEARCH_SEARCHABLE_FIELDS_STEMMED);

            try
            {
                @SuppressWarnings("unchecked")
                final Map<String, String> stemmedFieldsWithBoost = jsonHelper.unmarshallJsonToObject(Map.class, stemmedFieldsValue);
                final Map<String, Float> fieldsBoosts = new HashMap<>();

                // This additional step is needed because trying to cast an unmarshalled json to a Map of anything other than String key-value pairs won't work
                stemmedFieldsWithBoost.entrySet().forEach(entry -> fieldsBoosts.put(entry.getKey(), Float.parseFloat(entry.getValue())));

                // Set the fields and their respective boosts to the multi-match query
                multiMatchQueryBuilder.fields(fieldsBoosts);
            }
            catch (IOException e)
            {
                LOGGER.warn("Could not parse the configured JSON value for stemmed fields: {}", ConfigurationValue.ELASTICSEARCH_SEARCHABLE_FIELDS_STEMMED, e);
            }
        }

        if (fieldType.equals(FIELD_TYPE_NGRAMS))
        {
            // Get the configured value for 'ngrams' fields and their respective boosts if any
            String ngramsFieldsValue = configurationHelper.getProperty(ConfigurationValue.ELASTICSEARCH_SEARCHABLE_FIELDS_NGRAMS);

            try
            {
                @SuppressWarnings("unchecked")
                final Map<String, String> ngramsFieldsWithBoost = jsonHelper.unmarshallJsonToObject(Map.class, ngramsFieldsValue);

                final Map<String, Float> fieldsBoosts = new HashMap<>();

                // This additional step is needed because trying to cast an unmarshalled json to a Map of anything other than String key-value pairs won't work
                ngramsFieldsWithBoost.entrySet().forEach(entry -> fieldsBoosts.put(entry.getKey(), Float.parseFloat(entry.getValue())));

                // Set the fields and their respective boosts to the multi-match query
                multiMatchQueryBuilder.fields(fieldsBoosts);
            }
            catch (IOException e)
            {
                LOGGER.warn("Could not parse the configured JSON value for ngrams fields: {}", ConfigurationValue.ELASTICSEARCH_SEARCHABLE_FIELDS_NGRAMS, e);
            }
        }

        return multiMatchQueryBuilder;
    }

    /**
     * Builds a {@link HighlightBuilder} based on (pre/post)tags and fields fetched from the DB config which is added to the main {@link SearchRequestBuilder}
     *
     * @param preTag The specified pre-tag to be used for highlighting
     * @param postTag The specified post-tag to be used for highlighting
     *
     * @return A configured {@link HighlightBuilder} object
     */
    private HighlightBuilder buildHighlightQuery(String preTag, String postTag)
    {
        HighlightBuilder highlightBuilder = new HighlightBuilder();

        // Field matching is not needed since we are matching on multiple 'type' fields like stemmed and ngrams and enabling highlighting on all those fields
        // will yield duplicates
        highlightBuilder.requireFieldMatch(false);

        // Set the configured value for pre-tags for highlighting
        highlightBuilder.preTags(preTag);

        // Set the configured value for post-tags for highlighting
        highlightBuilder.postTags(postTag);

        // Get highlight fields value from configuration
        String highlightFieldsValue = configurationHelper.getProperty(ConfigurationValue.ELASTICSEARCH_HIGHLIGHT_FIELDS);

        try
        {
            @SuppressWarnings("unchecked")
            IndexSearchHighlightFields highlightFieldsConfig = jsonHelper.unmarshallJsonToObject(IndexSearchHighlightFields.class, highlightFieldsValue);

            highlightFieldsConfig.getHighlightFields().forEach(
                highlightFieldConfig -> {

                    // set the field name to the configured value
                    HighlightBuilder.Field highlightField = new HighlightBuilder.Field(highlightFieldConfig.getFieldName());

                    // set matched_fields to the configured list of fields, this accounts for 'multifields' that analyze the same string in different ways
                    if (CollectionUtils.isNotEmpty(highlightFieldConfig.getMatchedFields()))
                    {
                        highlightField.matchedFields(highlightFieldConfig.getMatchedFields().toArray(new String[0]));
                    }

                    // set fragment size to the configured value
                    if (highlightFieldConfig.getFragmentSize() != null)
                    {
                        highlightField.fragmentSize(highlightFieldConfig.getFragmentSize());
                    }

                    // set the number of desired fragments to the configured value
                    if (highlightFieldConfig.getNumOfFragments() != null)
                    {
                        highlightField.numOfFragments(highlightFieldConfig.getNumOfFragments());
                    }

                    highlightBuilder.field(highlightField);
                }
            );

        }
        catch (IOException e)
        {
            LOGGER.warn("Could not parse the configured value for highlight fields: {}", ConfigurationValue.ELASTICSEARCH_HIGHLIGHT_FIELDS, e);
        }

        return highlightBuilder;
    }
}
