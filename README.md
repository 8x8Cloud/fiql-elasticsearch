JAX-RS Search FIQL Elasticsearch Visitors
===========

### What is this?
This repository provides a reusable, tested, and solid implementation of a CXF visitor that can take a pre-parsed FIQL AST and generate an Elasticsearch [QueryBuilder](https://static.javadoc.io/org.elasticsearch/elasticsearch/5.2.2/index.html?org/elasticsearch/index/query/QueryBuilder.html). Given an expression, you can output a [QueryBuilder](https://static.javadoc.io/org.elasticsearch/elasticsearch/5.2.2/index.html?org/elasticsearch/index/query/QueryBuilder.html) that can be used with the standard Elasticsearch Java [SearchRequestBuilder](https://static.javadoc.io/org.elasticsearch/elasticsearch/5.2.2/index.html?org/elasticsearch/action/search/SearchRequestBuilder.html).

### Licensing
The work in this repository is licensed under the [Apache 2.0 License](https://opensource.org/licenses/Apache-2.0). Please see the repository's `LICENSE` file for details.

### What is FIQL
[FIQL](https://tools.ietf.org/html/draft-nottingham-atompub-fiql-00) is a standard URI-friendly way of expressing a filter across a collection of items. You can consider this something like generating a SQL `where` clause. Apache CXF provides an implementation of FIQL that it calls [JAX-RS Search](http://cxf.apache.org/docs/jax-rs-search.html).

The default implementation of JAX-RS Search includes a few visitors:

 * HBase
 * JPA
 * LDAP
 * Lucene
 * OData `$filter` clauses
 * SQL

Note that most of the above are not fully-featured search visitors, but instead their output approximates something like a `where` clause or filter. In the case of OData, this is explicitly obvious as JAX-RS implements only `$filter`.

Unlike most of these implementations (excepting Lucene, which is what ES is built upon), Elasticsearch is a fully-featured search product, and includes other functionality such as aggregation and various scripts. Please note that this library makes no attempt to handle aggregation, faceting, percolation etc. This only provides you with `query` params.

### How do I use it?

##### Maven Dependency
This artifact is currently unavailable in the Maven Central repository, or other open source repositories. You will want to cache this in a local Nexus/Artifactory and add it to your `pom.xml`:

```xml
<dependency>
    <groupId>com._8x8.cloud.platform</groupId>
    <artifactId>fiql-elasticsearch</artifactId>
    <version>1.0.16-a1e014d6</version>
</dependency>
```

##### Building it
```
mvn clean install
```

##### Using multiple JAX-RS implementations
If you are using another JAX-RS implementation, such as [Jersey](https://jersey.java.net/) or [RESTEasy](http://resteasy.jboss.org/) you may run into issues depending on how lucky you are with your classloader order. If you'd like to ensure that you keep using your JAX-RS provider rather than CXF, you'll want to add a `META-INF/services/javax.ws.rs.ext.RuntimeDelegate` file to denote your [RuntimeDelegate](http://docs.oracle.com/javaee/7/api/javax/ws/rs/ext/RuntimeDelegate.html). An example for Jersey 2.x would be:

```properties
org.glassfish.jersey.server.internal.RuntimeDelegateImpl
```

This tells the JAX-RS libraries which implementation's factories to use to bootstrap your application.

Want to know more?
 * Check out [JSR-339](https://jcp.org/en/jsr/detail?id=339), Chapter 11 for JAX-RS 2.0
 * Check out [JSR-311](https://jcp.org/en/jsr/detail?id=311), Chapter 7 for JAX-RS 1.0


#### Dependency Tree
We've tried to keep the dependency tree as simple as possible, leaking as few transitive dependencies as possible. It may be (will be) the case that we've messed this up. File a pull request, help us get it right!

### Examples
Check out the `ElasticsearchQueryBuilderVisitorIT` or the [JAX-RS Search docs](http://cxf.apache.org/docs/jax-rs-search.html).

Want a more streamlined experience? Check out the `ElasticsearchQueryBuilderIT`.

### Special Cases and Notes
If you read the [JAX-RS Search](http://cxf.apache.org/docs/jax-rs-search.html) page carefully you'll note that the functionality it support highly depends on what implementation you're using. Here are some relevant notes.

#### Thread Safety
This visitor is not threadsafe by design. This may seem somewhat stupid, but if you take a look at what a visitor does it doesn't really make sense to re-use them. All the state they contain is specific to the parsed expression being evaluated. Rather than attempting to use the somewhat scary thread safety primitives in JAX-RS Search, use this visitor like a prototype, and not a singleton.

#### Support for Collections
Unlike the [SQLPrinterVisitor](https://cxf.apache.org/javadoc/latest/index.html?org/apache/cxf/jaxrs/ext/search/sql/SQLPrinterVisitor.html) the `ElasticsearchQueryBuilderVisitor` supports collections.

If you have something that looks like this:
```java
import java.util.ArrayList;
import java.util.List;

public class TagCollections {
    private List<String> tags = new ArrayList<>();

    public List<String> getTags() {
        return tags;
    }

    public void setTags(final List<String> tags) {
        this.tags = tags;
    }
}
```

You can look for an item with a tag of "foo" via the following FIQL:

```fiql
tags==foo
```

Please note that `count` queries are not supported, and will throw an `IllegalArgumentException`. These continue to be supported solely by JPA.

#### Case Sensitivity
Case sensitivity in Elasticsearch is a bit more complicated than SQL. While the FIQL specification states that all searches will be case-insensitive, this depends entirely on your Elasticsearch mappings. If you need case-insensitive search, you'll need to use the case-insensitive analyzer.

#### Wildcard Support
Wildcards of `*` are supported for both the `==` and `!=` operators as a standard Elasticsearch wildcard query.

#### Null and Truthiness
This visitor does not handle the special case of null (or not-null) checks, nor for the detection of truthiness. Some of the other custom visitors may do so, and we can add it here if required.

#### Field Alias Mapping
Unlike some of the other visitors, this one does not support field alias mapping. We can add this feature if folks need it, but it's hard to understand how it would work with Elasticsearch without an example.

#### Date Formats
By default, the FIQL parsers support a date format of `yyyy-MM-DD`, without time. If you try and pass epoch/epoch+millis format, it will break. It is recommended that you use numeric mappings for these fields.

Conversely, output to the Elasticsearch query will also be in the same date format.
