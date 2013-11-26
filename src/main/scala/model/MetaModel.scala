package domain.model

import java.util.Date;

class Identity(id: Int)

class Entity(val id: Int) extends Identity(id)

class DomainEvent(val eventVersion: Int, val occurredOn: Date)

trait EventSourcedRoot {
  def accept(event: DomainEvent)
}

trait ValueObject extends AnyRef
