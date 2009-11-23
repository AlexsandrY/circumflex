package ru.circumflex.orm

/**
 * Wraps relational nodes (tables, views, virtual tables, subqueries and other stuff)
 * with an alias so that they may appear within SQL FROM clause.
 */
abstract class RelationNode(val relation: Relation)
    extends Relation with Configurable {

  def recordClass = relation.recordClass

  /**
   * Delegates to relation's configuration.
   */
  override def configuration = relation.configuration

  /**
   * An alias to refer the node from projections and criteria.
   */
  def alias: String

  /**
   * SQL representation of this node for use in FROM clause.
   */
  def toSql: String

  /**
   * Just proxies relation's primary key.
   */
  def primaryKey = relation.primaryKey

  /**
   * One or more projections that correspond to this node.
   */
  def projections: Seq[Projection[_]]

  /**
   * Returns columns of underlying relation.
   */
  def columns = relation.columns

  /**
   * Retrieves an association path by delegating calls to underlying relations.
   */
  def getParentAssociation(parent: Relation): Option[Association] =
    parent match {
      case parentNode: RelationNode => getParentAssociation(parentNode.relation)
      case _ => relation match {
        case childNode: RelationNode => childNode.relation.getParentAssociation(parent)
        case _ => relation.getParentAssociation(parent)
      }
    }

  /**
   * Proxies relation's name.
   */
  def relationName = relation.relationName

  /**
   * Proxies relation's qualified name.
   */
  def qualifiedName = relation.qualifiedName

  /**
   * Creates a join with specified node.
   */
  def join(node: RelationNode): JoinNode =
    new JoinNode(this, node)

  /**
   * Returns associations defined on underlying relation.
   */
  def associations = relation.associations

  /**
   * Returns constraints defined on underlying relation.
   */
  def constraints = relation.constraints

  override def toString = toSql

}

class TableNode(val table: Table,
                var alias: String)
    extends RelationNode(table) {

  /**
   * Dialect should return qualified name with alias (e.g. "myschema.mytable as myalias")
   */
  def toSql = configuration.dialect.tableAlias(table, alias)

  /**
   * Creates a record projection.
   */
  def projections = List(record)

  /**
   * Creates a record projection.
   */
  def record = new RecordProjection(this)

  /**
   * Creates a field projection with specified alias.
   */
  def field[T](col: Column[T], alias: String): FieldProjection[T] =
    new FieldProjection(alias, this, col)

  /**
   * Creates a field projection with default alias.
   */
  def field[T](col: Column[T]): FieldProjection[T] =
    new FieldProjection(this, col)

}

/**
 * Represents a join node between parent and child relation.
 */
class JoinNode(val leftNode: RelationNode,
               val rightNode: RelationNode)
    extends RelationNode(leftNode) {

  private var inverse: Boolean = false;

  /**
   * Evaluates an association between parent and child; throws an exception if
   * failed.
   */
  val association: Association = leftNode.getParentAssociation(rightNode) match {
    case Some(a) => {
      this.inverse = true
      a
    } case None => leftNode.getChildAssociation(rightNode) match {
      case Some(a) => {
        this.inverse = false
        a
      } case None => throw new ORMException("Failed to join " + leftNode +
          " with " + rightNode + ": no associations found.")
    }
  }

  /**
   * Determines whether this join is "inverse", that is the child is joined against parent.
   * If parent is joined against child then this should yield <code>false</code>.
   */
  def isInverse: Boolean = inverse

  /**
   * Returns an alias of parent relation for this join.
   */
  def alias = leftNode.alias

  /**
   * Override join type if necessary.
   */
  def sqlJoinType: String = configuration.dialect.leftJoin

  /**
   * Dialect should return properly joined parent and child nodes.
   */
  def toSql = configuration.dialect.join(this)

  /**
   * Join nodes return parent node's projections joined with child node's ones.
   */
  def projections = leftNode.projections ++ rightNode.projections

}