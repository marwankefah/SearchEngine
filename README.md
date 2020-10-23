# SearchEngine

# Table of Contents

Objectives

Search Engine Modules

Web Crawler

Indexer

Query ProcessorRanker

Web Interface Additional Features

# Objectives

The aim of this project is to develop a simple Crawler- based search engine that demonstrates the main features of a search engine (web crawling, indexing and ranking) and the interaction between them.

# Search Engine Modules

## Web Crawler

The web crawler is a software agent that collects documents from the web. The crawler starts with a list of URL addresses (seed set). It downloads the documents identified by these URLs and extracts hyper-links from them. The extracted URLs are added to the list of URLs to be downloaded. Thus, web crawling is a recursive process.

## Indexer

The output of web crawling process is a set of downloaded HTML documents. To respond to user queries fast enough, the contents of these documents have to be indexed in a data structure that stores the words contained in each document and their importance .

## Query Processor

This module receives search queries, performs necessary preprocessing and searches the index for relevant documents. Retrieve documents containing words that share the same stem with those in the search query. For example, the search query &quot;travel&quot; should match (with lower degree) the words &quot;traveler&quot;, &quot;traveling&quot; … etc.

## Ranker

The ranker module sorts documents based on their popularity and relevance to the search query.

#### Relevance:

Relevance is a relation between the query words and the result page and could be calculated in several

ways such as tf-idf of the query word in the result page or simply whether the query word appeared in the title, heading, or body. And then you aggregate the scores from all query words to produce the final page relevancescore.


#### Popularity:

Popularity is a measure for the importance of any web page regardless the requested query

## Web Interface

- This interface receives user queries and displays the resulting pages returned by theengine
- The result appears with **snippets** of the text containing queries words. The output should looklike google/bing&#39;s resultspage

- Pagination of results (i.e. if you got 200 results, they should appear on 20 pages, each page with10

results)

- suggestion mechanism that stores queries submitted by all users. As the user types a newquery, your web application should suggest popular completions to that query using some interactive mechanism such asAJAX.
- Thewebinterfaceshoulddisplaysuggestionswhiletheuseristypingtheirsearchquery.Forexample, if the user typed &#39;World&#39;, then a list of suggestions should be displayed **&#39;World Cup&#39;** , **&#39;World Health Organization&#39;** , **&#39;World War&#39;** , **&#39;World Meter&#39;** , ..etc.

# Additional Features

### ImageSearch:

The user can search the web for **images** on a given search query. For example, if the user used this feature and searched for &quot;World Cup&quot;, then the ranker should return the most relevant images to this query.


### RelevanceScore:

Your relevance score has to include the following aside from **word similarity** :

- **Geographic location of the user:** increasing the score of web pages related to the user&#39;s location. A web page(s) can be related to certain location(s) in many ways (server location, company&#39;s location, visitors&#39; location, URL extension, etc). It will be sufficient to consider one of these ways to score the geographic relevance of web page. For example, a web page having the .uk extension is more relevant to users in UK, a web page having the .cn extension is more relevant to users in China, and soon.

- **How recent is the web page?** A web page&#39;s score increases because it was published recently. Itshouldbenotedthatsomewebsitesdonotmentionthewebpage&#39;screationdateintheHTML.

.

### Trends:

Your query processor should keep track of search trends. We need to view the trends about the **most**** searched persons**in each country.

### Voice RecognitionSearch:

The user can use a voice query instead of a typed query. NLP Libraries and APIs (such as the Stanford CoreNLPlibrary) to recognize and understand a voice query, transform it into textual query and perform the search accordingly were used.

