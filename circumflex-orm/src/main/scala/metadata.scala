package ru.circumflex.orm

import ru.circumflex.core._
import collection.mutable.{ListBuffer, HashMap}

/*!# Database Metadata

In order to be able to create database objects and perform some operations
like cache invalidation, Circumflex ORM needs a little information about
the structure of records. This information is retrieved via introspection
the first time a record is accessed and stored as an instance of `MetaData`.
*/
object RecordMetadata extends HashMap[Class[Record[_]], RecordMetadata]
  with CacheMap[Class[Record[_]], RecordMetadata] {
  override def default(key: Class[Record[_]]): RecordMetadata =
    new RecordMetadata(key)
}

class RecordMetadata(val recordClass: Class[Record[_]]) {
  protected val _fields = new ListBuffer[Field[_]]
}