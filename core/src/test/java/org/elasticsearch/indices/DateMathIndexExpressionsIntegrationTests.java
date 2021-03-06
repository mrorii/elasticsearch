/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.indices;

import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.test.hamcrest.ElasticsearchAssertions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class DateMathIndexExpressionsIntegrationTests extends ElasticsearchIntegrationTest {

    public void testIndexNameDateMathExpressions() {
        DateTime now = new DateTime(DateTimeZone.UTC);
        String index1 = ".marvel-" + DateTimeFormat.forPattern("YYYY.MM.dd").print(now);
        String index2 = ".marvel-" + DateTimeFormat.forPattern("YYYY.MM.dd").print(now.minusDays(1));
        String index3 = ".marvel-" + DateTimeFormat.forPattern("YYYY.MM.dd").print(now.minusDays(2));
        createIndex(index1, index2, index3);

        String dateMathExp1 = "<.marvel-{now/d}>";
        String dateMathExp2 = "<.marvel-{now/d-1d}>";
        String dateMathExp3 = "<.marvel-{now/d-2d}>";
        client().prepareIndex(dateMathExp1, "type", "1").setSource("{}").get();
        client().prepareIndex(dateMathExp2, "type", "2").setSource("{}").get();
        client().prepareIndex(dateMathExp3, "type", "3").setSource("{}").get();
        refresh();

        SearchResponse searchResponse = client().prepareSearch(dateMathExp1, dateMathExp2, dateMathExp3).get();
        ElasticsearchAssertions.assertHitCount(searchResponse, 3);
        ElasticsearchAssertions.assertSearchHits(searchResponse, "1", "2", "3");

        GetResponse getResponse = client().prepareGet(dateMathExp1, "type", "1").get();
        assertThat(getResponse.isExists(), is(true));
        assertThat(getResponse.getId(), equalTo("1"));

        getResponse = client().prepareGet(dateMathExp2, "type", "2").get();
        assertThat(getResponse.isExists(), is(true));
        assertThat(getResponse.getId(), equalTo("2"));

        getResponse = client().prepareGet(dateMathExp3, "type", "3").get();
        assertThat(getResponse.isExists(), is(true));
        assertThat(getResponse.getId(), equalTo("3"));

        IndicesStatsResponse indicesStatsResponse = client().admin().indices().prepareStats(dateMathExp1, dateMathExp2, dateMathExp3).get();
        assertThat(indicesStatsResponse.getIndex(index1), notNullValue());
        assertThat(indicesStatsResponse.getIndex(index2), notNullValue());
        assertThat(indicesStatsResponse.getIndex(index3), notNullValue());

        DeleteResponse deleteResponse = client().prepareDelete(dateMathExp1, "type", "1").get();
        assertThat(deleteResponse.isFound(), equalTo(true));
        assertThat(deleteResponse.getId(), equalTo("1"));

        deleteResponse = client().prepareDelete(dateMathExp2, "type", "2").get();
        assertThat(deleteResponse.isFound(), equalTo(true));
        assertThat(deleteResponse.getId(), equalTo("2"));

        deleteResponse = client().prepareDelete(dateMathExp3, "type", "3").get();
        assertThat(deleteResponse.isFound(), equalTo(true));
        assertThat(deleteResponse.getId(), equalTo("3"));
    }

}
