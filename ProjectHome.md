A collection of Java projects and utilities written by Andrew Phillips, amongst others:

  * <a href='http://blog.xebia.com/2009/06/23/composite-macro-annotations-for-java/'>@Composite</a>
  * <a href='http://blog.xebia.com/2009/04/24/declarative-equality-for-your-business-domain-objects/'>Business Objects</a>
  * <a href='http://blog.xebia.com/2009/04/02/dynamic-enums-in-java'>Dynamic Enum</a>
  * Graph Traverser
  * Hibernate Cloner

Add the following repository to your POM to reference these projects as Maven dependencies.

```
<repository>
  <id>qrmedia-releases</id>
  <url>http://aphillips.googlecode.com/svn/maven-repository/releases</url>
  <snapshots>
    <enabled>false</enabled>
  </snapshots>
</repository>
<repository>
  <id>qrmedia-snapshots</id>
  <url>http://aphillips.googlecode.com/svn/maven-repository/snapshots</url>
</repository>
```